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
paquetes original (`app/probability/…`, `app/pedigree/…`). El código de cada archivo de `app/`
es el del archivo homónimo de GENis 5.1.15 (ver [Versión](#versión)).

**El motor compila y se ejecuta solo**, sin el resto del sistema: no hacen falta bases de datos,
servidor web ni autenticación. Eso permite tres usos:

1. **Leer**: seguir el cálculo desde los puntos de entrada documentados abajo.
2. **Ejecutar**: correr los casos incluidos y ver el LR que produce el motor
   ([Compilar y ejecutar](#compilar-y-ejecutar)).
3. **Probar casos propios**: escribir un caso nuevo contra el motor y contrastarlo con una
   herramienta independiente ([Familias](https://familias.name/),
   [forrel](https://github.com/magnusdv/forrel), LRmix Studio) o con una cuenta a mano.

La parte que se compila no incluye capa web, persistencia, autenticación, inyección de
dependencias ni configuración de despliegue; los servicios de sólo lectura sí dependen de ellas.

### Versión

El código de `app/` corresponde a **GENis 5.1.15**. Es el mismo código, sin cambios; sólo se
editaron comentarios, para quitar referencias a documentos y registros internos del proyecto que
no forman parte de este repositorio. Cada publicación de este repositorio lleva la etiqueta de
la versión de GENis de la que sale (`git tag`). Un laboratorio puede tener instalada otra
versión: para saber qué código calculó un LR determinado hay que consultar la versión instalada
allí.

### Qué se compila y qué es sólo lectura

Los archivos de `app/` son de dos clases:

- **El motor** (se compila y se ejecuta): el cálculo y los tipos de datos que usan el cálculo y
  los servicios que lo invocan.
- **La orquestación** (sólo lectura): los servicios que, dentro de GENis, leen los datos y llaman
  al motor. Dependen de partes del sistema que este repositorio no incluye, así que se publican
  para mostrar cómo se invoca el cálculo, pero el build no los compila.

Los archivos de sólo lectura son estos diez:

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

Un editor puede marcar errores al abrir esos archivos; es lo esperado.

**Un archivo se compila desde un extracto.** `app/pedigree/MutationService.scala` contiene a la
vez un servicio (sólo lectura) y el `object MutationService`, con funciones puras que el motor de
pedigrí necesita. Ese `object` se compila desde `standalone/pedigree/MutationServiceObject.scala`,
que es un extracto sin modificaciones del archivo original; un test verifica que el extracto
coincide línea por línea con el original y que el archivo no contiene nada más.

---

## Compilar y ejecutar

Requisitos: **Java 8** y [sbt](https://www.scala-sbt.org/) (el proyecto fija sbt 0.13.15 y
Scala 2.11.11, las versiones de GENis; sbt las descarga solo).

```bash
sbt compile   # compila el motor
sbt test      # corre los casos incluidos
```

La primera vez sbt descarga unos 250 MB de dependencias y puede tardar más de diez minutos; las
corridas siguientes llevan segundos.

Conviene usar sbt desde la terminal. Un IDE que compile por su cuenta (sin delegar en sbt) va a
intentar compilar también los archivos de sólo lectura y a marcar errores en ellos.

### Casos de referencia

`sbt test` ejecuta tres suites de `test/`:

| Suite | Qué muestra |
|---|---|
| `forensic.ForensicReferenceCasesTest` | Casos de referencia del motor forense: cada caso declara su LR esperado como una fórmula cerrada (fuente única, θ, mezclas de dos personas, drop-out, drop-in) y lo compara con el del motor. Al final imprime una tabla `case / expected / GENis / rel. diff / formula` |
| `pedigree.PedigreeReferenceCasesTest` | Casos de referencia del motor de pedigrí, cada uno con su cuenta a mano en el comentario: un dúo de paternidad; un trío con una inconsistencia paterna, sin modelo de mutación y con el modelo Equal; un nieto con los dos abuelos paternos tipificados; un cuarto hermano con tres hermanos tipificados; y un hijo con el padre y un hermano tipificados |
| `standalone.MutationServiceExtractTest` | Que el extracto de `MutationService` coincide con el original |

**Qué muestran.** Los casos comprueban que el motor reproduce, en situaciones chicas y con una
tabla de frecuencias inventada, resultados que se pueden calcular a mano con las fórmulas de la
literatura, y sirven de modelo para armar casos propios. No son una validación de GENis: la
validación del motor de pedigrí está en `validation/` (ver [Validación](#validación)), y para
contrastar un caso real hay que reproducir sus entradas (tabla poblacional, θ, loci considerados,
parámetros de mutación) y compararlo con una herramienta independiente (ver
[Reproducir un cálculo](#reproducir-un-cálculo)).

### Probar un caso propio

- **Forense**: copiar un bloque `check(...)` de `test/forensic/ForensicReferenceCasesTest.scala`
  y cambiar la muestra, las hipótesis, los parámetros y la fórmula esperada.
- **Pedigrí**: copiar un caso de `test/pedigree/PedigreeReferenceCasesTest.scala` y cambiar el
  genograma, los perfiles y la tabla de frecuencias. El comentario inicial de ese archivo explica
  cómo se arma un caso: el genograma, el individuo `unknown`, los perfiles, el formato de los
  códigos de muestra y la tabla de frecuencias. El caso del trío muestra cómo se declara un
  modelo de mutación.

Si se copia un archivo entero para hacer una suite nueva, hay que cambiarle también el nombre de
la clase. `sbt "testOnly forensic.ForensicReferenceCasesTest"` corre una sola suite.

El motor forense (`LRMixCalculator`) toma su pool de hilos de una aplicación Play en ejecución.
`test/standalone/EngineApp.scala` arranca una mínima, sin bases ni servicios, con la
configuración de `conf/application.conf`; cualquier suite propia que use el motor forense debe
hacer lo mismo que `ForensicReferenceCasesTest` (mezclar `OneAppPerSuite` y definir `app` con
`EngineApp()`). Si una suite falla con `There is no started application` o con
`Could not initialize class probability.LRMixCalculator$`, es que le falta eso. El motor de
pedigrí no lo necesita.

---

## Puntos de entrada

El cálculo tiene dos puntos de entrada principales, uno por motor; el matching masivo de MPI usa
un tercero, `BayesianNetwork.getLR` (ver abajo). Para entender o reproducir el LR, conviene
empezar por acá.

### Forense: `LRMixCalculator.calculateLRMix`

`app/probability/LRMixCalculator.scala`

```scala
def calculateLRMix(
  scenario: FullCalculationScenario,
  frequencyTable: FrequencyTable,
  allelesRanges: Option[NewMatchingResult.AlleleMatchRange] = None
): Future[LRResult]
```

Implementa el modelo **semicontinuo LRmix** (Haned y Gill; modelo de Curran et al. 2005 y Gill
et al. 2007) con drop-in, drop-out y corrección por subestructura poblacional θ.

**Entradas**

| Parámetro | Definición |
|---|---|
| `scenario.sample` | `Genotypification` = `Map[Marker, Seq[AlleleValue]]`. El perfil de la evidencia |
| `scenario.prosecutor` | Hipótesis H1: `FullHypothesis(selected, unselected, unknowns, dropOut)` |
| `scenario.defense` | Hipótesis H2, misma estructura |
| `scenario.stats` | `StatOption(frequencyTable, probabilityModel, theta, dropIn, dropOut)` |
| `frequencyTable` | `Map[(Marker, BigDecimal), Double]`. Frecuencia por alelo. La clave `(marker, -1)` es **fmin** |

En cada `FullHypothesis`: `selected` son los contribuyentes conocidos que la hipótesis afirma
presentes, `unselected` los conocidos que afirma ausentes, `unknowns` la cantidad de
contribuyentes desconocidos, y `dropOut` la probabilidad de drop-out para esa hipótesis.

De `scenario.stats` el motor lee `theta` y `dropIn`. Los nombres de tabla y de modelo y el
`dropOut` de `StatOption` no intervienen en esta función: la tabla llega en `frequencyTable` y el
drop-out se toma de cada hipótesis.

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
  locusRangeMap: NewMatchingResult.AlleleMatchRange = Map.empty
): (Double, Map[String, MarkerLRDetail])
```

Construye la red bayesiana del pedigrí, la resuelve por **eliminación de variables** y devuelve
el LR junto con el detalle por marcador.

Notar que acá `FrequencyTable` tiene otro tipo que en el módulo forense:
`Map[String, Map[Double, Double]]` (marcador → alelo → frecuencia).

**Entradas**

| Parámetro | Definición |
|---|---|
| `genogram` | `Array[Individual(alias, idFather, idMother, sex, globalCode, unknown, isReference)]`. La topología del pedigrí. `unknown = true` marca al individuo cuya identidad se pone a prueba; tiene que haber uno |
| `profiles` | Los perfiles genotipificados, vinculados al genograma por `globalCode` |
| `linkage` | `Map[Marker, (Marker, Double)]`. Pares de marcadores ligados y su fracción de recombinación |
| `mutationModelType` | `1` = Equal, `2` = Stepwise, `3` = Stepwise extended, `None` = sin modelo de mutación |
| `mutationModelData` | Una terna `(MutationModelParameter, List[MutationModelKi], MutationModel)` por locus y sexo: tasa, rango y tasa de microvariante, los `k_i` por alelo y el modelo. El sexo es `"F"`, `"M"` o, si el modelo ignora el sexo (`ignoreSex = true`), `"I"` |
| `seenAlleles` | Universo de alelos posibles por marcador. Si se omite, sale de la tabla de frecuencias |

**Salida**: el LR global y `Map[Marker, MarkerLRDetail(lr, classification, singleBandIndividuals)]`,
donde `classification` ∈ `{"normal", "mutation", "singleBand"}`: compatible sin mutación;
incompatible sin mutación (con modelo de mutación el LR sale de un salto mutacional, sin modelo
es 0); o marcador en el que el perfil consultado tiene un solo alelo (queda fuera del cálculo,
con LR 1). `singleBandIndividuals` lista los familiares con un solo
alelo en ese marcador, que no aportan datos ahí.

Antes de calcular, los perfiles se normalizan (`normalizarParaParentesco`): un marcador con algún
alelo `0` (sin lectura) se descarta entero para ese perfil.

Para el LR de un perfil individual contra un pedigrí ya genotipificado (el camino que usa el
matching masivo de MPI) el punto de entrada es `BayesianNetwork.getLR`.

### Qué modela cada motor

Los dos motores no modelan lo mismo. Conviene tenerlo presente antes de comparar contra otra
herramienta:

| | Forense (`LRMixCalculator`) | MPI/DVI (`BayesianNetwork`) |
|---|---|---|
| Subestructura poblacional (θ) | Sí | No: el cálculo equivale a θ = 0 |
| Drop-in y drop-out | Sí | No. Un marcador en el que el perfil consultado tiene un solo alelo queda fuera del cálculo (LR 1); un genotipo escrito con dos alelos iguales se toma como homocigoto |
| Mutación | No aplica | Tres modelos: Equal, Stepwise y Stepwise extended. No hay otros |

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
frecuencia mínima; todos comparten esa clave, así que el motor los trata como un único alelo
genérico. `fmin` se calcula por NRC II, Weir, Budowle-Monson-Chakraborty o valor fijo
(`insertFmin` en `app/stats/PopulationBaseFrequencyService.scala`).

El motor forense usa la tabla de frecuencias tal como la recibe, sin renormalizarla: un alelo
que no está en la tabla vale fmin, y a los alelos de la tabla que no aparecen en la muestra se
les asigna, en conjunto, la suma de sus frecuencias.

### Normalización de la tabla de frecuencias (motor de pedigrí)

`BayesianNetwork.getNormalizedFrequencyTable` es un punto crítico para reproducir resultados del
motor de pedigrí, y una fuente frecuente de discrepancias contra otras herramientas. Por
marcador hace, en orden:

1. **Arma el universo de alelos `n`**: los de la tabla de frecuencias (o `seenAlleles`, si se
   pasa), más todo alelo observado en los perfiles del pedigrí que la tabla no traiga
   (`extendNWithObservedAlleles`).
2. **Agrega a la tabla los alelos de `n` que no estaban**, con frecuencia **fmin** (la clave
   `-1.0`).
3. **Sube a fmin** toda frecuencia que esté por debajo.
4. **Renormaliza**: suma las frecuencias sin contar la clave `-1.0` y, si la suma no es 1,
   divide todas por esa suma.

```scala
val sum = flooredAlleles.filterKeys(_ != -1.0).values.sum
if (sum != 1) { /* dividir todas las frecuencias por sum */ }
```

Decisiones que hay que replicar para obtener los mismos números:

- **fmin queda fuera de la suma.** La clave `-1.0` es un piso para alelos ausentes de la tabla,
  no masa de probabilidad.
- **Se normaliza en ambas direcciones**, tanto si la tabla suma más de 1 como menos.
- **Un alelo observado que no está en la tabla entra con fmin y participa de la
  renormalización**, igual que los demás.

### Modelos de mutación

`getProbabityOfDiagonal` / `getProbabityOfNonDiagonal` en `app/pedigree/BayesianNetwork.scala`:

| Tipo | Diagonal (sin mutación) | Fuera de la diagonal |
|---|---|---|
| **1: Equal** | `1 − tasa` | La tasa repartida uniformemente entre los alelos restantes |
| **2: Stepwise** | `1 −` (suma de la fila fuera de la diagonal) | `k_i · rango^pasos` para alelos a distancia entera, hasta el tope de pasos del modelo; `0` para el resto |
| **3: Stepwise extended** | `1 −` (suma de la fila fuera de la diagonal) | Como Stepwise entre alelos de la misma clase de microvariante; entre clases distintas (por ejemplo de `31.2` a `30`), la tasa de microvariante repartida por igual entre los alelos de otra clase |

En los tipos 2 y 3 la diagonal se deriva de la fila, no se fija de antemano: se calcula la suma
de todas las transiciones fuera de la diagonal sobre el dominio del marcador y se resta de 1. El
dominio es `n`, el universo de alelos del marcador descripto en la sección anterior.

Los `k_i` normalizan cada fila para que la tasa de mutación del alelo hacia alelos a distancia
entera sea la declarada: `MutationService.recalibrateKis` los calcula sobre el mismo universo
`n`. En el tipo 3 a esa tasa se le suma la de microvariante.

La clave `-1.0` (el piso, y el valor al que se llevan los alelos fuera de escalera) no es un
alelo: no recibe ni transmite masa de mutación con ningún modelo.

### Resolución de la red bayesiana

`calculateLR` arma un CPT (`app/pedigree/PlainCPT.scala`) por individuo y marcador, con una
variable por alelo materno y paterno (`app/pedigree/Variable.scala`), y resuelve por eliminación
de variables (`variableElimination`, `sumFactor`, `prodFactor`). El orden de eliminación sale de
un grafo de interacción (`makeInteractionGraph`, `getOrdering`).

Las probabilidades de cada marcador se calculan en **log-probabilidad**
(`getQueryProbabilityLog`) y se convierten al LR del marcador. Sin ligamiento, el LR global es el
producto de los LR por marcador; con ligamiento se calcula en log sobre la red completa
(`getEvidenceProbabilityLog`, `getGenotypeProbabilityLog`).

---

## Recorrido de archivos

### Módulo forense

| Archivo | Rol |
|---|---|
| `app/probability/LRMixCalculator.scala` | El motor: `pCond`, `pRep`, `calculateLRMix` |
| `app/probability/FullCalculationScenario.scala` | El escenario: muestra, hipótesis y parámetros |
| `app/probability/PValueCalculator.scala` | Armado de la tabla de frecuencias que consume el motor |
| `app/probability/ProbabilityModel.scala` | Modelos Hardy-Weinberg, NRC II 4.1 y 4.10 |
| `app/matching/MatchingAlgorithm.scala` | Comparación de genotipos alelo a alelo |
| `app/probability/ProbabilityService.scala` *(sólo lectura)* | Estimación de cantidad de contribuyentes y parámetros estadísticos por defecto |
| `app/matching/MatchingCalculatorService.scala` *(sólo lectura)* | Orquestación del LR sobre resultados de matching |

### Módulo MPI/DVI

| Archivo | Rol |
|---|---|
| `app/pedigree/BayesianNetwork.scala` | Red bayesiana, LR, mutación, normalización de frecuencias |
| `app/pedigree/PlainCPT.scala`, `CPT.scala` | Tablas de probabilidad condicional |
| `app/pedigree/Variable.scala` | Variables de la red (alelo materno / paterno por marcador) |
| `app/pedigree/Mutation*.scala` | Modelos de mutación y sus parámetros |
| `app/pedigree/SingleBandExclusions.scala`, `app/profile/SingleBand.scala` | Marcadores con un solo alelo: se excluyen del cálculo y se informan |
| `app/pedigree/PedigreeMatchingAlgorithm.scala` | Matching de pedigrí contra el banco de perfiles |
| `app/pedigree/CandidateUniverse.scala` | Universo de alelos del cotejo: qué marcadores hay que rehacer cuando el candidato trae alelos que no estaban en el universo de la familia |
| `app/pedigree/Pedigree.scala` | `Individual`, `PedigreeGenogram`. La topología |
| `app/pedigree/BayesianNetworkService.scala`, `PedigreeGenotypificationService.scala`, `PedigreeSparkMatcher.scala`, `MutationService.scala` *(sólo lectura)* | Cómo GENis lee los datos e invoca al motor: escenarios, búsqueda masiva, parámetros de mutación |

### Frecuencias y modelo genético

| Archivo | Rol |
|---|---|
| `app/stats/PopulationBaseFrequency.scala` | Tabla poblacional: frecuencias, θ, modelo |
| `app/stats/PopulationBaseFrequencyService.scala` *(sólo lectura)* | Carga y validación de tablas poblacionales; cálculo de fmin (`insertFmin`) |
| `app/types/MinimunFrequencyCalc.scala` | Enumeración de los métodos de cálculo de fmin |
| `app/profile/Profile.scala`, `AlleleValue.scala` | Perfiles, alelos, microvariantes, fuera de escalera |
| `app/kits/Locus.scala`, `StrKit.scala` | Loci, rangos alélicos y kits STR |

El resto de los archivos de `app/` son, en su mayoría, tipos de datos que los anteriores usan
(estados, resultados de coincidencia, identificadores).

---

## Reproducir un cálculo

Un procedimiento para contrastar un caso contra otra herramienta:

1. **Fijá la tabla de frecuencias.** Es la fuente de discrepancia más común. Anotá si suma
   exactamente 1 y qué valor de fmin se usó. Para el motor de pedigrí, aplicá la
   [normalización](#normalización-de-la-tabla-de-frecuencias-motor-de-pedigrí) antes de comparar
   contra otra herramienta; el motor forense usa la tabla sin renormalizar.
2. **Fijá θ.** El motor forense lo recibe en `scenario.stats.theta`; GENis lo completa por
   defecto con el θ de la tabla poblacional (`PopulationBaseFrequency.theta`). El motor de
   pedigrí no aplica θ, lo que equivale a θ = 0.
3. **Declará el modelo de mutación** explícitamente, incluyendo tasa por locus y sexo. Sin
   modelo (`mutationModelType = None`) toda exclusión mendeliana lleva el LR del marcador a cero.
   Con modelo, tené en cuenta que las herramientas difieren en cómo aplican la mutación a las
   transmisiones de un progenitor no tipificado.
4. **Compará marcador por marcador, no solo el total.** Tanto `LRResult.detailed` como
   `MarkerLRDetail` exponen el LR individual, que es donde se localiza cualquier divergencia.
5. **Compará los totales en escala logarítmica.** El LR global de un pedigrí con 20 o más
   marcadores abarca muchos órdenes de magnitud.

---

## Validación

`validation/` contiene la validación del cálculo de LR del **motor de pedigrí**, con su
resultado. El motor forense no forma parte de ella. Tiene dos bloques:

- **Catálogo**: 51 casos chicos (parentescos, mutación, alelo silente, frecuencias, invariantes)
  cuyo LR se conoce en forma cerrada por la literatura (Evett y Weir 1998; Slooten 2011). El
  LR del motor se compara con ese valor con una tolerancia relativa de 1e-9.
- **Búsqueda masiva**: familias simuladas contra una nómina de personas de interés, celda por
  celda, contra dos programas independientes ([Familias](https://familias.name/) y la suite
  `pedtools`/`forrel`).

El resultado de la última corrida publicada está en
[`validation/referencia/out/VEREDICTO.txt`](validation/referencia/out/VEREDICTO.txt) (resumen)
y en [`validation/referencia/out/INFORME.md`](validation/referencia/out/INFORME.md) (detalle
por caso). Cómo volver a correrla, qué necesita y cuánto tarda cada bloque:
[`validation/COMO-CORRER.md`](validation/COMO-CORRER.md).

---

## Reportar una discrepancia

Si un cálculo de este repositorio no coincide con una cuenta a mano o con otra herramienta, se
puede abrir un issue con los datos del caso (tabla de frecuencias, perfiles, hipótesis y
parámetros) y la versión usada. No incluyas datos de casos reales.

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
