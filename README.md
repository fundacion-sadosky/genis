# GENis: motor de cálculo de Likelihood Ratio

*[English version](README_EN.md)*

Código de cálculo de **Likelihood Ratio (LR)** de [GENis](https://www.fundacionsadosky.org.ar),
la herramienta forense desarrollada por la Fundación Dr. Manuel Sadosky para comparar perfiles
genéticos mediante análisis STR. Cubre los dos motores del sistema:

- **Forense**: comparación perfil a perfil, incluyendo mezclas (`app/probability/`)
- **MPI/DVI**: identificación por pedigrí mediante redes bayesianas (`app/pedigree/`)

Se publica para permitir la lectura, revisión y reproducción independiente de los cálculos.

## Qué es y qué no es este repositorio

Este repositorio contiene **los archivos donde vive la matemática del LR**, con la estructura de
paquetes original (`app/probability/…`, `app/pedigree/…`).

**No compila por sí solo.** No incluye capa web, persistencia, autenticación, inyección de
dependencias ni configuración de despliegue. Reproducir los cálculos no significa ejecutar este
repositorio: significa partir de las fórmulas y los puntos de entrada documentados abajo, que son
los mismos que corren en producción.

Hay dos caminos, y este README documenta los dos:

1. **Verificar**: recalcular un caso con una herramienta independiente
   ([Familias](https://familias.name/), [forrel](https://github.com/magnusdv/forrel), LRmix Studio)
   y contrastar contra el LR que reporta GENis.
2. **Reimplementar**: escribir el cálculo desde las fórmulas de la sección
   [La matemática](#la-matemática) y comparar contra este código, que es la referencia normativa.

---

## Puntos de entrada

Todo el cálculo entra por dos funciones. Si querés entender o reproducir el LR, empezá por acá.

### Forense: `LRMixCalculator.calculateLRMix`

`app/probability/LRMixCalculator.scala`

```scala
def calculateLRMix(
  scenario: FullCalculationScenario,
  frequencyTable: FrequencyTable,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
): Future[LRResult]
```

Implementa el modelo **semicontinuo LRmix** (Curran / Gill) con drop-in, drop-out y corrección
por subestructura poblacional θ.

**Entradas**

| Parámetro | Definición |
|---|---|
| `scenario.sample` | `Genotypification` = `Map[Marker, Seq[AlleleValue]]`. El perfil de la evidencia |
| `scenario.prosecutor` | Hipótesis H1: `FullHypothesis(selected, unselected, unknowns, dropOut)` |
| `scenario.defense` | Hipótesis H2, misma estructura |
| `scenario.stats` | `StatOption(frequencyTable, probabilityModel, theta, dropIn, dropOut)` |
| `frequencyTable` | `Map[(Marker, Double), Double]`. Frecuencia por alelo. La clave `(marker, -1)` es **fmin** |

En cada `FullHypothesis`: `selected` son los contribuyentes conocidos que la hipótesis afirma
presentes, `unselected` los conocidos que afirma ausentes, `unknowns` la cantidad de
contribuyentes desconocidos, y `dropOut` la probabilidad de drop-out para esa hipótesis.

**Salida**: `LRResult(total: Double, detailed: Map[Marker, Option[Double]])`. El LR global y el
LR por marcador.

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

Construye la red bayesiana del pedigrí, la resuelve por **eliminación de variables** y devuelve
el LR junto con el detalle por marcador.

Notar que acá `FrequencyTable` tiene otro tipo que en el módulo forense:
`Map[String, Map[Double, Double]]` (marcador → alelo → frecuencia).

**Entradas**

| Parámetro | Definición |
|---|---|
| `genogram` | `Array[Individual(alias, idFather, idMother, sex, globalCode, unknown, isReference)]`. La topología del pedigrí |
| `profiles` | Los perfiles genotipificados, vinculados al genograma por `globalCode` |
| `linkage` | `Map[Marker, (Marker, Double)]`. Pares de marcadores ligados y su fracción de recombinación |
| `mutationModelType` | `1` = Equal, `2` = Stepwise, `None` = sin modelo de mutación |
| `mutationModelData` | Por locus y sexo: tasa, rango y tasa de microvariante, más los `k_i` por alelo |
| `maxExclusionsAllowed` | Exclusiones mendelianas toleradas antes de descartar el escenario |

**Salida**: el LR global y `Map[Marker, MarkerLRDetail(lr, classification)]`, donde
`classification` ∈ `{"normal", "mutation", "excluded"}`: match directo, explicado por un salto
mutacional, o exclusión mendeliana tolerada.

Para el LR de un perfil individual contra un pedigrí ya genotipificado (el camino que usa el
matching masivo de MPI) el punto de entrada es `BayesianNetwork.getLR`.

---

## La matemática

### Probabilidad condicional de genotipo con θ

`LRMixCalculator.pCond` es la base de todo el módulo forense. Para cada alelo desconocido `u`
que se va agregando al conjunto de alelos ya vistos:

```
                 nᵢ · θ + (1 − θ) · pᵢ
    P(u | ...) = ──────────────────────
                   1 + (n − 1) · θ
```

donde `n` es la cantidad de alelos ya colocados, `nᵢ` cuántas veces ya apareció el alelo `u`, y
`pᵢ` su frecuencia poblacional. Es la fórmula de muestreo de Balding-Nichols: con `θ = 0` se
reduce a Hardy-Weinberg. El producto sobre todos los alelos se multiplica por el factor
multinomial `k! / ∏ mⱼ!` que cuenta los órdenes distinguibles de los `k` desconocidos.

**Ejemplo verificable a mano.** Marcador con `p₁₂ = 0.2`, genotipo homocigota `{12, 12}`,
sin conocidos:

| θ | Paso 1 (`n=0, nᵢ=0`) | Paso 2 (`n=1, nᵢ=1`) | Factor | Resultado |
|---|---|---|---|---|
| `0.00` | `0.2 / 1` = 0.2 | `0.2 / 1` = 0.2 | `2!/2!` = 1 | **0.04** = p² |
| `0.01` | `0.198 / 0.99` = 0.2 | `0.208 / 1` = 0.208 | 1 | **0.0416** |

Para el heterocigota `{12, 13}` con `p₁₃ = 0.3` y `θ = 0`: `0.2 × 0.3 × 2!/(1!·1!)` = **0.12** = 2pq.

### Drop-in, drop-out y el LR forense

`pRep` combina los dos mecanismos de error de tipificación:

- **`pRepOut`**: para cada alelo del contribuyente presente en la muestra, `1 − pOut^oᵢ`; para
  cada alelo ausente, `pOut^oᵢ`, con `oᵢ` la cantidad de veces que ese alelo aparece entre los
  contribuyentes.
- **`pRepIn`**: si no hay alelos extra en la muestra, `1 − pIn`; si los hay,
  `pIn^d · ∏ pⱼ` sobre los `d` alelos de drop-in.

El LR es el cociente de las probabilidades de la evidencia bajo cada hipótesis, marcador a marcador:

```
    LR = P(E | H1) / P(E | H2)
```

Los alelos fuera de escalera (`OutOfLadderAllele`) y las microvariantes (`MicroVariant`) se
mapean a `-1.0` en `transformAlleleValues`, lo que los hace resolver contra **fmin**, el piso de
frecuencia mínima. `fmin` se calcula por NRC II, Weir, Budowle-Monson-Chakraborty o valor fijo
(`app/types/MinimunFrequencyCalc.scala`).

### Normalización de la tabla de frecuencias

`BayesianNetwork.getNormalizedFrequencyTable` es un punto crítico para reproducir resultados,
y una fuente frecuente de discrepancias contra otras herramientas:

```scala
val sum = alleles.filterKeys(_ != -1.0).values.sum
if (sum != 1) { /* dividir todas las frecuencias por sum */ }
```

Dos decisiones que hay que replicar para obtener los mismos números:

1. **fmin queda fuera de la suma.** El alelo `-1.0` es un piso para alelos ausentes de la tabla,
   no masa de probabilidad real. Incluirlo infla el divisor y sesga el LR.
2. **Se normaliza en ambas direcciones**, tanto si la tabla suma más de 1 como menos. El universo
   de estados de un fundador no tipificado (`getNFromTable`) se arma **antes** de normalizar,
   sobre la tabla cruda, así que la masa faltante se reparte reescalando los alelos existentes en
   lugar de agregar un alelo "no visto" que ningún CPT enumeraría.

### Modelos de mutación

`getProbabityOfDiagonal` / `getProbabityOfNonDiagonal` en `app/pedigree/BayesianNetwork.scala`:

| Tipo | Diagonal (sin mutación) | Fuera de la diagonal |
|---|---|---|
| **1: Equal** | `1 − tasa` | La tasa repartida uniformemente entre los alelos restantes |
| **2: Stepwise** | `1 −` (suma de la fila fuera de la diagonal) | Decae con la distancia en repeticiones, ponderado por `k_i` |

En Stepwise la diagonal se deriva de la fila, no se fija de antemano: se calcula la suma de todas
las transiciones fuera de la diagonal sobre el dominio del marcador y se resta de 1. El dominio
sale de `n`, el universo de alelos posibles, que debe salir de la **misma** tabla de frecuencias
declarada en el escenario, no de la unión de todas las tablas cargadas en el sistema.

Los saltos son enteros: solo se modelan transiciones entre repeticiones completas.

### Resolución de la red bayesiana

`calculateLR` arma un CPT (`app/pedigree/PlainCPT.scala`) por individuo y marcador, con una
variable por alelo materno y paterno (`app/pedigree/Variable.scala`), y resuelve por eliminación
de variables (`variableElimination`, `sumFactor`, `prodFactor`). El orden de eliminación sale de
un grafo de interacción (`makeInteractionGraph`, `getOrdering`).

El cálculo se hace en **log-probabilidad** (`getQueryProbabilityLog`, `getEvidenceProbabilityLog`,
`getGenotypeProbabilityLog`) para evitar underflow: con muchos marcadores las probabilidades
conjuntas caen por debajo del mínimo representable en punto flotante.

---

## Recorrido de archivos

### Módulo forense

| Archivo | Rol |
|---|---|
| `app/probability/LRMixCalculator.scala` | El motor: `pCond`, `pRep`, `calculateLRMix` |
| `app/probability/MixtureLRCalculator.scala` | Armado del escenario de mezcla |
| `app/probability/PValueCalculator.scala` | p-value y parseo de la tabla de frecuencias |
| `app/probability/ProbabilityService.scala` | Estimación de cantidad de contribuyentes |
| `app/probability/MatchingProbabilityCalculationMode.scala` | Modelos Hardy-Weinberg, NRC II 4.1 y 4.10 |
| `app/matching/MatchingCalculatorService.scala` | Orquestación del LR sobre resultados de matching |

### Módulo MPI/DVI

| Archivo | Rol |
|---|---|
| `app/pedigree/BayesianNetwork.scala` | Red bayesiana, LR, mutación, normalización de frecuencias |
| `app/pedigree/PlainCPT.scala`, `CPT.scala` | Tablas de probabilidad condicional |
| `app/pedigree/Variable.scala` | Variables de la red (alelo materno / paterno por marcador) |
| `app/pedigree/Mutation*.scala` | Modelos de mutación y sus parámetros |
| `app/pedigree/PedigreeMatchingAlgorithm.scala` | Matching de pedigrí contra el banco de perfiles |
| `app/pedigree/Pedigree.scala` | `Individual`, `PedigreeGenogram`. La topología |

### Frecuencias y modelo genético

| Archivo | Rol |
|---|---|
| `app/stats/PopulationBaseFrequencyService.scala` | Carga y validación de tablas poblacionales |
| `app/types/MinimunFrequencyCalc.scala` | Métodos de cálculo de fmin |
| `app/profile/Profile.scala`, `AlleleValue.scala` | Perfiles, alelos, microvariantes, fuera de escalera |
| `app/kits/Locus.scala`, `StrKit.scala` | Loci, rangos alélicos y kits STR |

---

## Reproducir un cálculo

Un procedimiento que funciona sin ejecutar este código:

1. **Fijá la tabla de frecuencias.** Es la fuente de discrepancia más común. Anotá si suma
   exactamente 1 y qué valor de fmin se usó, y aplicá la normalización de la sección anterior
   antes de comparar contra cualquier otra herramienta.
2. **Fijá θ.** GENis lo toma de la tabla poblacional (`PopulationBaseFrequency.theta`), no del
   escenario. Muchas herramientas usan θ = 0 por defecto.
3. **Declará el modelo de mutación** explícitamente, incluyendo tasa por locus y sexo. Sin
   modelo (`mutationModelType = None`) toda exclusión mendeliana lleva el LR del marcador a cero.
4. **Compará marcador por marcador, no solo el total.** Tanto `LRResult.detailed` como
   `MarkerLRDetail` exponen el LR individual, que es donde se localiza cualquier divergencia.
5. **Trabajá en log.** Un LR global de un pedigrí con 20+ marcadores no es comparable en punto
   flotante directo.

---

## Licencia

GNU Affero General Public License v3.0. Ver [LICENSE](LICENSE).

## Proyecto

| | Español | English |
|---|---|---|
| Datos del proyecto y sitio oficial | [ABOUT.md](ABOUT.md) | [ABOUT_EN.md](ABOUT_EN.md) |
| Coordinación y colaboraciones | [AUTHORS.md](AUTHORS.md) | [AUTHORS_EN.md](AUTHORS_EN.md) |
| Código de conducta | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | [CODE_OF_CONDUCT_EN.md](CODE_OF_CONDUCT_EN.md) |

## Contacto

[Fundación Dr. Manuel Sadosky](https://www.fundacionsadosky.org.ar)
