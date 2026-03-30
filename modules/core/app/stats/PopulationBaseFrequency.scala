package stats

import play.api.libs.json.*
import play.api.libs.json.Json

// ---------------------------------------------------------------------------
// Enums (Scala 3) — reemplazan los Enumeration de Scala 2 del legacy
// ---------------------------------------------------------------------------

enum ProbabilityModel:
  case HardyWeinberg, NrcIIMixture, NrcIIMatch

object ProbabilityModel:
  given Format[ProbabilityModel] = Format(
    Reads { json =>
      json.validate[String].flatMap {
        case "HardyWeinberg" => JsSuccess(HardyWeinberg)
        case "NrcIIMixture"  => JsSuccess(NrcIIMixture)
        case "NrcIIMatch"    => JsSuccess(NrcIIMatch)
        case other           => JsError(s"Unknown ProbabilityModel: $other")
      }
    },
    Writes(m => JsString(m.toString))
  )

  def withName(s: String): ProbabilityModel = s match
    case "HardyWeinberg" => HardyWeinberg
    case "NrcIIMixture"  => NrcIIMixture
    case "NrcIIMatch"    => NrcIIMatch
    case other           => throw new NoSuchElementException(s"Unknown ProbabilityModel: $other")

enum MinimunFrequencyCalc:
  case BudowleMonsonChakraborty, NRCII, Weir, FminValue

object MinimunFrequencyCalc:
  given Format[MinimunFrequencyCalc] = Format(
    Reads { json =>
      json.validate[String].flatMap {
        case "BudowleMonsonChakraborty" => JsSuccess(BudowleMonsonChakraborty)
        case "NRCII"                    => JsSuccess(NRCII)
        case "Weir"                     => JsSuccess(Weir)
        case "FminValue"                => JsSuccess(FminValue)
        case other                      => JsError(s"Unknown MinimunFrequencyCalc: $other")
      }
    },
    Writes(m => JsString(m.toString))
  )

// ---------------------------------------------------------------------------
// Domain models (idénticos al legacy)
// ---------------------------------------------------------------------------

case class PopulationBaseFrequency(
  name: String,
  theta: Double,
  model: ProbabilityModel,
  base: Seq[PopulationSampleFrequency]
)

case class PopulationSampleFrequency(
  marker: String,
  allele: Double,
  frequency: BigDecimal
)

case class PopulationBaseFrequencyGrouppedByLocus(base: Map[String, List[Double]])

case class PopulationBaseFrequencyNameView(
  name: String,
  theta: Double,
  model: String,
  state: Boolean,
  default: Boolean
)

object PopulationBaseFrequencyNameView:
  given Format[PopulationBaseFrequencyNameView] = Json.format[PopulationBaseFrequencyNameView]

case class PopulationBaseFrequencyView(
  alleles: List[Double],
  markers: List[String],
  frequencys: List[List[Option[BigDecimal]]]
)

object PopulationBaseFrequencyView:
  // El macro Json.format de Scala 3 no resuelve la cadena List[List[Option[BigDecimal]]];
  // se construyen explícitamente para evitar que traversableReads trate Option como colección.
  private val readsOptBD: Reads[Option[BigDecimal]] = Reads {
    case JsNull      => JsSuccess(None)
    case JsNumber(n) => JsSuccess(Some(n))
    case other       => JsError(s"Expected null or number, got: $other")
  }
  private given readsListListOptBD: Reads[List[List[Option[BigDecimal]]]] =
    Reads.list(Reads.list(readsOptBD))
  private given writesListListOptBD: Writes[List[List[Option[BigDecimal]]]] =
    Writes { outer =>
      JsArray(outer.map(inner => JsArray(inner.map {
        case None    => JsNull
        case Some(x) => JsNumber(x)
      })))
    }
  given Format[PopulationBaseFrequencyView] = Json.format[PopulationBaseFrequencyView]

// Contenedor para los parámetros de cálculo de frecuencia mínima
case class Fmins(
  calcOption: MinimunFrequencyCalc,
  config: Map[String, Seq[Double]]  // Profile.Marker es alias de String en el legacy
)

object Fmins:
  given Format[Fmins] = Json.format[Fmins]

// Resultado de operaciones de importación / inserción
case class PopBaseFreqResult(
  status: String,
  key: Option[String],
  loci: Option[Seq[String]],
  errors: Seq[String],
  inserts: Option[Int]
)

object PopBaseFreqResult:
  given Format[PopBaseFreqResult] = Json.format[PopBaseFreqResult]

// Estructura interna usada durante la validación del CSV
private[stats] case class NormalizedPbF(
  markers: List[String],
  alleles: List[Double],
  freqs: List[List[Option[BigDecimal]]],
  fmins: List[BigDecimal]
)
