# GENis: Likelihood Ratio calculation engine

*[Versión en español](README.md)*

Likelihood Ratio (LR) calculation code from [GENis](https://www.fundacionsadosky.org.ar), the
forensic tool developed by Fundación Dr. Manuel Sadosky to compare genetic profiles through STR
analysis. It covers both of the system's engines:

- **Forensic**: profile-to-profile comparison, including mixtures (`app/probability/`)
- **MPI/DVI**: pedigree-based identification through Bayesian networks (`app/pedigree/`)

It is published to allow independent reading, review and reproduction of the calculations.

## What this repository is and is not

This repository holds **the files where the LR math lives**, keeping the original package
structure (`app/probability/…`, `app/pedigree/…`).

**It does not compile on its own.** It contains no web layer, persistence, authentication,
dependency injection or deployment configuration. Reproducing the calculations does not mean
running this repository: it means starting from the formulas and entry points documented below,
which are the same ones running in production.

There are two paths, and this README documents both:

1. **Verify**: recompute a case with an independent tool
   ([Familias](https://familias.name/), [forrel](https://github.com/magnusdv/forrel), LRmix Studio)
   and contrast it against the LR that GENis reports.
2. **Reimplement**: write the calculation from the formulas in [The math](#the-math) and compare
   against this code, which is the normative reference.

---

## Entry points

The whole calculation goes through two functions. To understand or reproduce the LR, start here.

### Forensic: `LRMixCalculator.calculateLRMix`

`app/probability/LRMixCalculator.scala`

```scala
def calculateLRMix(
  scenario: FullCalculationScenario,
  frequencyTable: FrequencyTable,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
): Future[LRResult]
```

Implements the **semi-continuous LRmix model** (Curran / Gill) with drop-in, drop-out and a θ
correction for population substructure.

**Inputs**

| Parameter | Definition |
|---|---|
| `scenario.sample` | `Genotypification` = `Map[Marker, Seq[AlleleValue]]`. The evidence profile |
| `scenario.prosecutor` | Hypothesis H1: `FullHypothesis(selected, unselected, unknowns, dropOut)` |
| `scenario.defense` | Hypothesis H2, same structure |
| `scenario.stats` | `StatOption(frequencyTable, probabilityModel, theta, dropIn, dropOut)` |
| `frequencyTable` | `Map[(Marker, Double), Double]`. Frequency per allele. Key `(marker, -1)` is **fmin** |

Within each `FullHypothesis`: `selected` are the known contributors the hypothesis asserts are
present, `unselected` the known ones it asserts are absent, `unknowns` the number of unknown
contributors, and `dropOut` the drop-out probability for that hypothesis.

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
  locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty,
  maxExclusionsAllowed: Int = 0
): (Double, Map[String, MarkerLRDetail])
```

Builds the pedigree's Bayesian network, solves it by **variable elimination** and returns the LR
together with the per-marker breakdown.

Note that `FrequencyTable` has a different type here than in the forensic module:
`Map[String, Map[Double, Double]]` (marker → allele → frequency).

**Inputs**

| Parameter | Definition |
|---|---|
| `genogram` | `Array[Individual(alias, idFather, idMother, sex, globalCode, unknown, isReference)]`. The pedigree topology |
| `profiles` | The genotyped profiles, linked to the genogram through `globalCode` |
| `linkage` | `Map[Marker, (Marker, Double)]`. Linked marker pairs and their recombination fraction |
| `mutationModelType` | `1` = Equal, `2` = Stepwise, `None` = no mutation model |
| `mutationModelData` | Per locus and sex: rate, range and microvariant rate, plus the per-allele `k_i` |
| `maxExclusionsAllowed` | Mendelian exclusions tolerated before discarding the scenario |

**Output**: the overall LR and `Map[Marker, MarkerLRDetail(lr, classification)]`, where
`classification` ∈ `{"normal", "mutation", "excluded"}`: direct match, explained by a mutational
step, or tolerated Mendelian exclusion.

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
floor. `fmin` is computed by NRC II, Weir, Budowle-Monson-Chakraborty or a fixed value
(`app/types/MinimunFrequencyCalc.scala`).

### Frequency table normalization

`BayesianNetwork.getNormalizedFrequencyTable` is a critical point for reproducing results, and a
frequent source of discrepancies against other tools:

```scala
val sum = alleles.filterKeys(_ != -1.0).values.sum
if (sum != 1) { /* divide every frequency by sum */ }
```

Two decisions that must be replicated to obtain the same numbers:

1. **fmin is excluded from the sum.** Allele `-1.0` is a floor for alleles missing from the table,
   not real probability mass. Including it inflates the divisor and biases the LR.
2. **Normalization runs in both directions**, whether the table sums to more or less than 1. The
   state space of an untyped founder (`getNFromTable`) is built **before** normalizing, from the
   raw table, so the missing mass is redistributed by rescaling the existing alleles rather than
   by adding an "unseen" allele that no CPT would enumerate.

### Mutation models

`getProbabityOfDiagonal` / `getProbabityOfNonDiagonal` in `app/pedigree/BayesianNetwork.scala`:

| Type | Diagonal (no mutation) | Off-diagonal |
|---|---|---|
| **1: Equal** | `1 − rate` | The rate spread uniformly across the remaining alleles |
| **2: Stepwise** | `1 −` (sum of the off-diagonal row) | Decays with the distance in repeats, weighted by `k_i` |

Under Stepwise the diagonal is derived from the row rather than fixed beforehand: the sum of all
off-diagonal transitions over the marker's domain is computed and subtracted from 1. The domain
comes from `n`, the space of possible alleles, which must come from the **same** frequency table
declared in the scenario, not from the union of every table loaded in the system.

Steps are integral: only transitions between whole repeat units are modelled.

### Solving the Bayesian network

`calculateLR` builds one CPT (`app/pedigree/PlainCPT.scala`) per individual and marker, with one
variable per maternal and paternal allele (`app/pedigree/Variable.scala`), and solves it by
variable elimination (`variableElimination`, `sumFactor`, `prodFactor`). The elimination ordering
comes from an interaction graph (`makeInteractionGraph`, `getOrdering`).

The calculation is carried out in **log-probability** (`getQueryProbabilityLog`,
`getEvidenceProbabilityLog`, `getGenotypeProbabilityLog`) to avoid underflow: with many markers
the joint probabilities fall below the smallest representable floating-point value.

---

## File guide

### Forensic module

| File | Role |
|---|---|
| `app/probability/LRMixCalculator.scala` | The engine: `pCond`, `pRep`, `calculateLRMix` |
| `app/probability/MixtureLRCalculator.scala` | Mixture scenario assembly |
| `app/probability/PValueCalculator.scala` | p-value and frequency table parsing |
| `app/probability/ProbabilityService.scala` | Number-of-contributors estimation |
| `app/probability/MatchingProbabilityCalculationMode.scala` | Hardy-Weinberg, NRC II 4.1 and 4.10 models |
| `app/matching/MatchingCalculatorService.scala` | LR orchestration over matching results |

### MPI/DVI module

| File | Role |
|---|---|
| `app/pedigree/BayesianNetwork.scala` | Bayesian network, LR, mutation, frequency normalization |
| `app/pedigree/PlainCPT.scala`, `CPT.scala` | Conditional probability tables |
| `app/pedigree/Variable.scala` | Network variables (maternal / paternal allele per marker) |
| `app/pedigree/Mutation*.scala` | Mutation models and their parameters |
| `app/pedigree/PedigreeMatchingAlgorithm.scala` | Pedigree matching against the profile database |
| `app/pedigree/Pedigree.scala` | `Individual`, `PedigreeGenogram`. The topology |

### Frequencies and genetic model

| File | Role |
|---|---|
| `app/stats/PopulationBaseFrequencyService.scala` | Population table loading and validation |
| `app/types/MinimunFrequencyCalc.scala` | fmin calculation methods |
| `app/profile/Profile.scala`, `AlleleValue.scala` | Profiles, alleles, microvariants, off-ladder |
| `app/kits/Locus.scala`, `StrKit.scala` | Loci, allele ranges and STR kits |

---

## Reproducing a calculation

A procedure that works without executing this code:

1. **Pin down the frequency table.** It is the most common source of discrepancy. Record whether
   it sums to exactly 1 and which fmin value was used, and apply the normalization from the
   section above before comparing against any other tool.
2. **Pin down θ.** GENis takes it from the population table (`PopulationBaseFrequency.theta`), not
   from the scenario. Many tools default to θ = 0.
3. **Declare the mutation model** explicitly, including rate per locus and sex. With no model
   (`mutationModelType = None`) any Mendelian exclusion drives that marker's LR to zero.
4. **Compare marker by marker, not just the total.** Both `LRResult.detailed` and
   `MarkerLRDetail` expose the individual LR, which is where any divergence is localized.
5. **Work in log space.** An overall pedigree LR across 20+ markers is not comparable in direct
   floating point.

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
