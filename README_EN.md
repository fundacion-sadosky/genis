# GENis: Likelihood Ratio calculation engine

*[Versión en español](README.md)*

Likelihood Ratio (LR) calculation code from [GENis](https://www.fundacionsadosky.org.ar), the
forensic tool developed by Fundación Dr. Manuel Sadosky to compare genetic profiles through STR
analysis. It covers both of the system's engines:

- **Forensic**: profile-to-profile comparison, including mixtures (`app/probability/`)
- **MPI/DVI**: pedigree-based identification through Bayesian networks (`app/pedigree/`)

It is published to allow independent reading, review and reproduction of the calculations.

## What this repository is and is not

This repository contains **the files where the LR math lives**, with the original package
structure (`app/probability/…`, `app/pedigree/…`). The code of every file under `app/` is that
of the file with the same name in GENis 5.1.15 (see [Version](#version)).

**The engine compiles and runs on its own**, without the rest of the system: no databases, web
server or authentication are needed. That allows three uses:

1. **Read**: follow the calculation from the entry points documented below.
2. **Run**: execute the included cases and see the LR the engine produces
   ([Building and running](#building-and-running)).
3. **Try your own cases**: write a new case against the engine and compare it with an
   independent tool ([Familias](https://familias.name/),
   [forrel](https://github.com/magnusdv/forrel), LRmix Studio) or with a hand calculation.

The compiled part does not include the web layer, persistence, authentication, dependency
injection or deployment configuration; the read-only services do depend on them.

### Version

The code under `app/` corresponds to **GENis 5.1.15**. It is the same code, unchanged; only
comments were edited, to remove references to internal project documents and records that are
not part of this repository. Each release of this repository is tagged with the GENis version
it comes from (`git tag`). A laboratory may have a different version installed: to know which
code produced a given LR, check the version installed there.

### What is compiled and what is read-only

The files under `app/` are of two kinds:

- **The engine** (compiled and runnable): the calculation and the data types used by the
  calculation and by the services that invoke it.
- **The orchestration** (read-only): the services that, inside GENis, read the data and call the
  engine. They depend on parts of the system this repository does not include, so they are
  published to show how the calculation is invoked, but the build does not compile them.

The read-only files are these ten:

- `app/configdata/QualityParamsProvider.scala`
- `app/matching/MatchingCalculatorService.scala`
- `app/pedigree/BayesianNetworkService.scala`
- `app/pedigree/MutationService.scala`
- `app/pedigree/PedigreeGenotypificationService.scala`
- `app/pedigree/PedigreeSparkMatcher.scala`
- `app/probability/CalculationTypeService.scala`
- `app/probability/ProbabilityModule.scala`
- `app/probability/ProbabilityService.scala`
- `app/stats/PopulationBaseFrequencyService.scala`

An editor may flag errors when opening those files; that is expected.

**One file is compiled from an extract.** `app/pedigree/MutationService.scala` holds both a
service (read-only) and `object MutationService`, pure functions the pedigree engine needs. That
object is compiled from `standalone/pedigree/MutationServiceObject.scala`, an unmodified extract
of the original file; a test checks that the extract matches the original line by line and that
the file contains nothing else.

---

## Building and running

Requirements: **Java 8** and [sbt](https://www.scala-sbt.org/) (the project pins sbt 0.13.15 and
Scala 2.11.11, the versions GENis uses; sbt downloads them by itself).

```bash
sbt compile   # compiles the engine
sbt test      # runs the included cases
```

The first time, sbt downloads about 250 MB of dependencies and may take more than ten minutes;
later runs take seconds.

Using sbt from the terminal is recommended. An IDE that compiles on its own (without delegating
to sbt) will also try to compile the read-only files and flag errors in them.

### Reference cases

`sbt test` runs three suites under `test/`:

| Suite | What it shows |
|---|---|
| `forensic.ForensicReferenceCasesTest` | Reference cases for the forensic engine: each case states its expected LR as a closed formula (single source, θ, two-person mixtures, drop-out, drop-in) and compares it with the engine's. It prints a `case / expected / GENis / rel. diff / formula` table at the end |
| `pedigree.PedigreeReferenceCasesTest` | Reference cases for the pedigree engine: a paternity duo whose paternity index can be worked out by hand, and a trio with a paternal inconsistency, without a mutation model and with the Equal model |
| `standalone.MutationServiceExtractTest` | That the `MutationService` extract matches the original |

**What they show.** The cases check that the engine reproduces, in small situations and with a
made-up frequency table, results that can be worked out by hand from the formulas in the
literature, and they serve as templates for writing your own cases. They are not a validation of
GENis: to check a real case, reproduce its inputs (population table, θ, loci taken into account,
mutation parameters) and compare it against an independent tool (see
[Reproducing a calculation](#reproducing-a-calculation)).

### Trying your own case

- **Forensic**: copy a `check(...)` block in `test/forensic/ForensicReferenceCasesTest.scala`
  and change the sample, the hypotheses, the parameters and the expected formula.
- **Pedigree**: copy a case in `test/pedigree/PedigreeReferenceCasesTest.scala` and change the
  genogram, the profiles and the frequency table. The comment at the top of that file explains
  how a case is put together: the genogram, the `unknown` individual, the profiles, the format
  of sample codes and the frequency table. The trio case shows how a mutation model is declared.

If you copy a whole file to make a new suite, change the class name as well.
`sbt "testOnly forensic.ForensicReferenceCasesTest"` runs a single suite.

The forensic engine (`LRMixCalculator`) takes its thread pool from a running Play application.
`test/standalone/EngineApp.scala` starts a minimal one, with no databases or services, using the
configuration in `conf/application.conf`; any suite of your own that uses the forensic engine
must do what `ForensicReferenceCasesTest` does (mix in `OneAppPerSuite` and define `app` as
`EngineApp()`). If a suite fails with `There is no started application` or with
`Could not initialize class probability.LRMixCalculator$`, that is what it is missing. The
pedigree engine does not need it.

---

## Entry points

The calculation has two main entry points, one per engine; MPI's bulk matching uses a third one,
`BayesianNetwork.getLR` (see below). To understand or reproduce the LR, start here.

### Forensic: `LRMixCalculator.calculateLRMix`

`app/probability/LRMixCalculator.scala`

```scala
def calculateLRMix(
  scenario: FullCalculationScenario,
  frequencyTable: FrequencyTable,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
): Future[LRResult]
```

Implements the **semi-continuous LRmix model** (Haned and Gill; model of Curran et al. 2005 and
Gill et al. 2007) with drop-in, drop-out and a θ correction for population substructure.

**Inputs**

| Parameter | Definition |
|---|---|
| `scenario.sample` | `Genotypification` = `Map[Marker, Seq[AlleleValue]]`. The evidence profile |
| `scenario.prosecutor` | Hypothesis H1: `FullHypothesis(selected, unselected, unknowns, dropOut)` |
| `scenario.defense` | Hypothesis H2, same structure |
| `scenario.stats` | `StatOption(frequencyTable, probabilityModel, theta, dropIn, dropOut)` |
| `frequencyTable` | `Map[(Marker, BigDecimal), Double]`. Frequency per allele. Key `(marker, -1)` is **fmin** |

Within each `FullHypothesis`: `selected` are the known contributors the hypothesis asserts are
present, `unselected` the known ones it asserts are absent, `unknowns` the number of unknown
contributors, and `dropOut` the drop-out probability for that hypothesis.

From `scenario.stats` the engine reads `theta` and `dropIn`. The table and model names and the
`dropOut` of `StatOption` play no part in this function: the table arrives in `frequencyTable`
and drop-out is taken from each hypothesis.

**Output**: `LRResult(total: Double, detailed: Map[Marker, Option[Double]])`. The overall LR and
the per-marker LR.

### MPI/DVI: `BayesianNetwork.calculateProbability`

`app/pedigree/BayesianNetwork.scala`

```scala
def calculateProbability(
  profiles: Array[Profile],
  genogram: Array[Individual],
  frequencyTable: FrequencyTable,
  analysisType: AnalysisType,
  linkage: Linkage,
  verbose: Boolean = false,
  mutationModelType: Option[Long] = None,
  mutationModelData: Option[List[MutationModelData]] = None,
  seenAlleles: Map[String, List[Double]] = Map.empty,
  locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty
): (Double, Map[String, MarkerLRDetail])
```

Builds the pedigree's Bayesian network, solves it by **variable elimination** and returns the LR
together with the per-marker breakdown.

Note that `FrequencyTable` has a different type here than in the forensic module:
`Map[String, Map[Double, Double]]` (marker → allele → frequency).

**Inputs**

| Parameter | Definition |
|---|---|
| `genogram` | `Array[Individual(alias, idFather, idMother, sex, globalCode, unknown, isReference)]`. The pedigree topology. `unknown = true` marks the individual whose identity is in question; there must be one |
| `profiles` | The genotyped profiles, linked to the genogram through `globalCode` |
| `linkage` | `Map[Marker, (Marker, Double)]`. Linked marker pairs and their recombination fraction |
| `mutationModelType` | `1` = Equal, `2` = Stepwise, `3` = Stepwise extended, `None` = no mutation model |
| `mutationModelData` | One triple `(MutationModelParameter, List[MutationModelKi], MutationModel)` per locus and sex: rate, range and microvariant rate, the per-allele `k_i` and the model. Sex is `"F"`, `"M"` or, when the model ignores sex (`ignoreSex = true`), `"I"` |
| `seenAlleles` | Space of possible alleles per marker. When omitted it comes from the frequency table |

**Output**: the overall LR and `Map[Marker, MarkerLRDetail(lr, classification, singleBandIndividuals)]`,
where `classification` ∈ `{"normal", "mutation", "singleBand"}`: compatible without mutation;
incompatible without mutation (with a mutation model the LR comes from a mutational step,
without one it is 0); or a marker where the queried profile has a single allele (it is left out
of the calculation, with LR 1). `singleBandIndividuals` lists the relatives with a
single allele at that marker, who contribute no data there.

Before calculating, profiles are normalized (`normalizarParaParentesco`): a marker with any `0`
allele (no call) is dropped entirely for that profile.

For the LR of a single profile against an already genotyped pedigree (the path used by MPI's
bulk matching) the entry point is `BayesianNetwork.getLR`.

---

## The math

### Conditional genotype probability with θ

`LRMixCalculator.pCond` is the basis of the entire forensic module. For each unknown allele `u`
as it is added to the set of alleles already placed:

```
                 nᵢ · θ + (1 − θ) · pᵢ
    P(u | ...) = ──────────────────────
                   1 + (n − 1) · θ
```

where `n` is how many alleles have already been placed, `nᵢ` how many times allele `u` has
already appeared, and `pᵢ` its population frequency. This is the Balding-Nichols sampling
formula: with `θ = 0` it reduces to Hardy-Weinberg. The product over all alleles is multiplied by
the multinomial factor `k! / ∏ mⱼ!` counting the distinguishable orderings of the `k` unknowns.

**Worked example, verifiable by hand.** Marker with `p₁₂ = 0.2`, homozygous genotype `{12, 12}`,
no known contributors:

| θ | Step 1 (`n=0, nᵢ=0`) | Step 2 (`n=1, nᵢ=1`) | Factor | Result |
|---|---|---|---|---|
| `0.00` | `0.2 / 1` = 0.2 | `0.2 / 1` = 0.2 | `2!/2!` = 1 | **0.04** = p² |
| `0.01` | `0.198 / 0.99` = 0.2 | `0.208 / 1` = 0.208 | 1 | **0.0416** |

For the heterozygote `{12, 13}` with `p₁₃ = 0.3` and `θ = 0`: `0.2 × 0.3 × 2!/(1!·1!)` = **0.12** = 2pq.

### Drop-in, drop-out and the forensic LR

`pRep` combines the two typing-error mechanisms:

- **`pRepOut`**: for each contributor allele present in the sample, `1 − pOut^oᵢ`; for each
  absent allele, `pOut^oᵢ`, where `oᵢ` is how many times that allele appears among the
  contributors.
- **`pRepIn`**: if there are no extra alleles in the sample, `1 − pIn`; if there are,
  `pIn^d · ∏ pⱼ` over the `d` drop-in alleles.

The LR is the ratio of the probabilities of the evidence under each hypothesis, marker by marker:

```
    LR = P(E | H1) / P(E | H2)
```

Off-ladder alleles (`OutOfLadderAllele`) and microvariants (`MicroVariant`) are mapped to `-1.0`
in `transformAlleleValues`, which makes them resolve against **fmin**, the minimum frequency
floor; they all share that key, so the engine treats them as a single generic allele. `fmin` is
computed by NRC II, Weir, Budowle-Monson-Chakraborty or a fixed value (`insertFmin` in
`app/stats/PopulationBaseFrequencyService.scala`).

The forensic engine uses the frequency table as received, without renormalizing it: an allele
missing from the table is worth fmin, and the alleles of the table that do not appear in the
sample are given, together, the sum of their frequencies.

### Frequency table normalization (pedigree engine)

`BayesianNetwork.getNormalizedFrequencyTable` is a critical point for reproducing results of the
pedigree engine, and a frequent source of discrepancies against other tools. Per marker it does,
in order:

1. **Builds the allele space `n`**: the alleles of the frequency table (or `seenAlleles`, when
   given), plus every allele observed in the pedigree's profiles that the table lacks
   (`extendNWithObservedAlleles`).
2. **Adds to the table the alleles of `n` that were missing**, with frequency **fmin** (key
   `-1.0`).
3. **Raises to fmin** every frequency below it.
4. **Renormalizes**: sums the frequencies leaving out key `-1.0` and, if the sum is not 1,
   divides them all by that sum.

```scala
val sum = flooredAlleles.filterKeys(_ != -1.0).values.sum
if (sum != 1) { /* divide every frequency by sum */ }
```

Decisions that must be replicated to obtain the same numbers:

- **fmin is excluded from the sum.** Key `-1.0` is a floor for alleles missing from the table,
  not probability mass.
- **Normalization runs in both directions**, whether the table sums to more or less than 1.
- **An observed allele that is not in the table enters at fmin and takes part in the
  renormalization**, like the rest.

### Mutation models

`getProbabityOfDiagonal` / `getProbabityOfNonDiagonal` in `app/pedigree/BayesianNetwork.scala`:

| Type | Diagonal (no mutation) | Off-diagonal |
|---|---|---|
| **1: Equal** | `1 − rate` | The rate spread uniformly across the remaining alleles |
| **2: Stepwise** | `1 −` (sum of the off-diagonal row) | `k_i · range^steps` for alleles a whole number of repeats away, up to the model's step limit; `0` otherwise |
| **3: Stepwise extended** | `1 −` (sum of the off-diagonal row) | As Stepwise between alleles of the same microvariant class; across classes (for instance from `31.2` to `30`), the microvariant rate spread evenly over the alleles of other classes |

Under types 2 and 3 the diagonal is derived from the row rather than fixed beforehand: the sum
of all off-diagonal transitions over the marker's domain is computed and subtracted from 1. The
domain is `n`, the marker's allele space described in the previous section.

The `k_i` normalize each row so that the allele's mutation rate towards alleles a whole number
of repeats away equals the declared one: `MutationService.recalibrateKis` computes them over the
same space `n`. Under type 3 the microvariant rate is added to that rate.

Key `-1.0` (the floor, and the value off-ladder alleles are mapped to) is not an allele: it
neither receives nor transmits mutation mass under any model.

### Solving the Bayesian network

`calculateLR` builds one CPT (`app/pedigree/PlainCPT.scala`) per individual and marker, with one
variable per maternal and paternal allele (`app/pedigree/Variable.scala`), and solves it by
variable elimination (`variableElimination`, `sumFactor`, `prodFactor`). The elimination ordering
comes from an interaction graph (`makeInteractionGraph`, `getOrdering`).

The probabilities of each marker are computed in **log-probability**
(`getQueryProbabilityLog`) and converted to the marker's LR. Without linkage, the overall LR is
the product of the per-marker LRs; with linkage it is computed in log space over the whole
network (`getEvidenceProbabilityLog`, `getGenotypeProbabilityLog`).

---

## File guide

### Forensic module

| File | Role |
|---|---|
| `app/probability/LRMixCalculator.scala` | The engine: `pCond`, `pRep`, `calculateLRMix` |
| `app/probability/FullCalculationScenario.scala` | The scenario: sample, hypotheses and parameters |
| `app/probability/PValueCalculator.scala` | Builds the frequency table the engine consumes |
| `app/probability/ProbabilityModel.scala` | Hardy-Weinberg, NRC II 4.1 and 4.10 models |
| `app/matching/MatchingAlgorithm.scala` | Allele-by-allele genotype comparison |
| `app/probability/ProbabilityService.scala` *(read-only)* | Number-of-contributors estimation and default statistical parameters |
| `app/matching/MatchingCalculatorService.scala` *(read-only)* | LR orchestration over matching results |

### MPI/DVI module

| File | Role |
|---|---|
| `app/pedigree/BayesianNetwork.scala` | Bayesian network, LR, mutation, frequency normalization |
| `app/pedigree/PlainCPT.scala`, `CPT.scala` | Conditional probability tables |
| `app/pedigree/Variable.scala` | Network variables (maternal / paternal allele per marker) |
| `app/pedigree/Mutation*.scala` | Mutation models and their parameters |
| `app/pedigree/SingleBandExclusions.scala`, `app/profile/SingleBand.scala` | Single-allele markers: left out of the calculation and reported |
| `app/pedigree/PedigreeMatchingAlgorithm.scala` | Pedigree matching against the profile database |
| `app/pedigree/CandidateUniverse.scala` | Allele space of the comparison: which markers must be rebuilt when the candidate carries alleles that were not in the family's allele space |
| `app/pedigree/Pedigree.scala` | `Individual`, `PedigreeGenogram`. The topology |
| `app/pedigree/BayesianNetworkService.scala`, `PedigreeGenotypificationService.scala`, `PedigreeSparkMatcher.scala`, `MutationService.scala` *(read-only)* | How GENis reads the data and invokes the engine: scenarios, bulk search, mutation parameters |

### Frequencies and genetic model

| File | Role |
|---|---|
| `app/stats/PopulationBaseFrequency.scala` | Population table: frequencies, θ, model |
| `app/stats/PopulationBaseFrequencyService.scala` *(read-only)* | Population table loading and validation; fmin calculation (`insertFmin`) |
| `app/types/MinimunFrequencyCalc.scala` | Enumeration of the fmin calculation methods |
| `app/profile/Profile.scala`, `AlleleValue.scala` | Profiles, alleles, microvariants, off-ladder |
| `app/kits/Locus.scala`, `StrKit.scala` | Loci, allele ranges and STR kits |

The remaining files under `app/` are, for the most part, data types the ones above use
(statuses, match results, identifiers).

---

## Reproducing a calculation

A procedure to check a case against another tool:

1. **Pin down the frequency table.** It is the most common source of discrepancy. Record whether
   it sums to exactly 1 and which fmin value was used. For the pedigree engine, apply the
   [normalization](#frequency-table-normalization-pedigree-engine) before comparing against
   another tool; the forensic engine uses the table without renormalizing.
2. **Pin down θ.** The forensic engine receives it in `scenario.stats.theta`; GENis fills it in
   by default with the θ of the population table (`PopulationBaseFrequency.theta`). The pedigree
   engine does not apply θ, which is equivalent to θ = 0.
3. **Declare the mutation model** explicitly, including rate per locus and sex. With no model
   (`mutationModelType = None`) any Mendelian exclusion drives that marker's LR to zero. With a
   model, bear in mind that tools differ in how they apply mutation to the transmissions of an
   untyped parent.
4. **Compare marker by marker, not just the total.** Both `LRResult.detailed` and
   `MarkerLRDetail` expose the individual LR, which is where any divergence is localized.
5. **Compare totals on a logarithmic scale.** The overall LR of a pedigree with 20 or more
   markers spans many orders of magnitude.

---

## Reporting a discrepancy

If a calculation in this repository does not match a hand calculation or another tool, you can
open an issue with the data of the case (frequency table, profiles, hypotheses and parameters)
and the version used. Do not include data from real cases.

---

## License

GNU Affero General Public License v3.0. See [LICENSE](LICENSE).

## Project

| | English | Español |
|---|---|---|
| Project data and official site | [ABOUT_EN.md](ABOUT_EN.md) | [ABOUT.md](ABOUT.md) |
| Coordination and contributions | [AUTHORS_EN.md](AUTHORS_EN.md) | [AUTHORS.md](AUTHORS.md) |
| Code of conduct | [CODE_OF_CONDUCT_EN.md](CODE_OF_CONDUCT_EN.md) | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) |

## Contact

[Fundación Dr. Manuel Sadosky](https://www.fundacionsadosky.org.ar)
