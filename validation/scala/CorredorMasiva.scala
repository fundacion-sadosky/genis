package validation

import kits.AnalysisType
import pedigree.{BayesianNetwork, CandidateUniverse, Individual, MarkerPlainCPT2,
  PedigreeCompatibilityMatch, PedigreeGenotypification, PedigreeMatchingAlgorithm, PlainCPT2,
  SingleBandExclusions}

import scala.util.control.NonFatal

// La genotipificacion de UNA familia: la distribucion condicional del genotipo
// del desaparecido dados los parientes tipificados, por locus. Se calcula una
// sola vez por familia y responde a cualquier POI.
//
// Todo se guarda como PlainCPT2 / MarkerPlainCPT2, que llevan
// Array[Array[Double]], igual que el documento que el servicio de GENis
// persiste. NO como PlainCPT, cuya matriz es un Iterator (var matrix,
// PlainCPT.scala:13-16) y se consume en el primer uso; los PlainCPT frescos
// por comparacion los materializa findCompatibilityMatches.
//
// `universoGeno` es el universo con que se construyeron estos CPT: el panel
// mas los alelos observados en la familia. Va sellado en `documento`, que es
// lo que el servicio de GENis persiste y lo que el cotejo usa como universo.
case class GenotipificacionFamilia(
  familia: String,
  tipo: String,
  bajoPrueba: String,
  principal: Array[PlainCPT2],
  estricta: Array[PlainCPT2],
  h2: Option[Array[MarkerPlainCPT2]],
  conMarcador: Array[MarkerPlainCPT2],
  universoGeno: Map[String, List[Double]],
  perfiles: Array[profile.Profile],
  genograma: Array[Individual],
  bandaUnica: Map[String, List[String]],
  // Memo de marcadores rehechos, uno por familia: la clave del servicio de
  // GENis lleva idPedigree, que aca es fijo, y con una cache por familia
  // no hay colision posible.
  cache: PedigreeMatchingAlgorithm.RebuildCache = PedigreeMatchingAlgorithm.newRebuildCache()
) {
  // El documento que el servicio de GENis guarda por pedigri
  // (PedigreeGenotypificationService.saveGenotypification) y que
  // findCompatibilityMatches recibe en cada comparacion. Umbral 0, para
  // obtener la distribucion completa; el universo va SELLADO (el cotejo usa
  // `universe` si esta, y si no lo recomputa desde getNFromTable, que trae
  // el -1.0 del piso adentro). _id, tabla y rango no se usan para nada aca.
  lazy val documento: PedigreeGenotypification = PedigreeGenotypification(
    _id = 0L,
    genotypification = principal,
    boundary = 0.0,
    frequencyTable = "",
    unknowns = Array(bajoPrueba),
    strictGenotypification = estricta,
    genotypificationH2 = h2,
    genotypificationWithMarkers = conMarcador,
    familySingleBand = bandaUnica,
    universe = universoGeno,
    familyFingerprint = Some(CandidateUniverse.familyFingerprint(perfiles, genograma)))
}

sealed trait Celda
// log10LR puede ser Double.NegativeInfinity: es la exclusion.
case class CeldaOk(log10LR: Double, observaciones: String) extends Celda
case class CeldaError(motivo: String) extends Celda

object CorredorMasiva {

  private val TIPO_ANALISIS: AnalysisType = MapeoGenis.TIPO_DE_ANALISIS
  // Identidad del pedigri y del caso que findCompatibilityMatches copia en la
  // coincidencia; no entran en ningun calculo.
  private val ID_PEDIGRI = 0L
  private val ID_CASO = 0L
  private val RESPONSABLE = "arnes"
  private val TIPO_DE_CASO = "MPI"

  // La genotipificacion de la familia por el codigo real de GENis:
  // BayesianNetwork.buildGenotypification es la composicion que
  // PedigreeGenotypificationService.saveGenotypification persiste -- la
  // principal, la estricta (sin modelo mutacional, solo para contar
  // exclusiones mendelianas), el par H2 / con-marcador, y la tabla normalizada
  // SOBRE el universo -- y la misma que usa rebuildMarker en el cotejo.
  // Lo que el arnes arma es lo que en el servicio de GENis sale de la base:
  // los perfiles normalizados como en el servicio, el universo (panel mas
  // alelos observados en la familia, extendNWithObservedAlleles) y el modelo
  // de mutacion. Rangos de locus vacios, como el bloque 1 (el servicio los
  // toma de LocusService; el paquete no tiene alelos fuera de escalera).
  //
  // El universo queda guardado en `universoGeno` y sellado en el documento:
  // el cotejo usa ese mismo universo (ver `documento`), asi que las dos
  // mitades de la busqueda corren sobre el mismo universo.
  def genotipificar(
    e: EntradaFamilia,
    frecuencias: Map[String, Map[Double, Double]],
    universo: Map[String, List[Double]],
    modelo: (Option[Long], Option[List[BayesianNetwork.MutationModelData]])
  ): GenotipificacionFamilia = {
    val (tipoModelo, datosModelo) = modelo
    val perfiles = e.perfiles.map(BayesianNetwork.normalizarParaParentesco(_, frecuencias))
    val universoGeno = BayesianNetwork.extendNWithObservedAlleles(
      universo, perfiles, e.genograma, TIPO_ANALISIS)

    val g = BayesianNetwork.buildGenotypification(
      perfiles, e.genograma, frecuencias, TIPO_ANALISIS, Map.empty,
      None, false, Map.empty, tipoModelo, datosModelo, universoGeno)

    GenotipificacionFamilia(
      familia = e.familia,
      tipo = e.tipo,
      bajoPrueba = e.bajoPrueba,
      principal = g.genotypification,
      estricta = g.strictGenotypification,
      h2 = g.genotypificationH2,
      conMarcador = g.genotypificationWithMarkers,
      universoGeno = universoGeno,
      perfiles = perfiles,
      genograma = e.genograma,
      bandaUnica = SingleBandExclusions.byIndividual(perfiles, e.genograma, frecuencias))
  }

  // Un POI contra la genotipificacion de la familia, por el camino real de
  // GENis: PedigreeMatchingAlgorithm.findCompatibilityMatches, lo mismo que
  // PedigreeSparkMatcher llama por cada candidato. Umbral 0 (la distribucion
  // completa): con `boundary = 0.0` toda comparacion devuelve su
  // coincidencia, tambien con LR 0, y la poda por umbral
  // (`lrOthers * bound < boundary`) no puede disparar. La interseccion de
  // marcadores, los PlainCPT frescos por comparacion y el rehecho de
  // marcadores con alelos fuera del universo los hace el codigo de GENis;
  // el arnes no replica nada de eso.
  //
  // Lo unico que es del arnes es armar los argumentos que en el servicio de
  // GENis salen de la base: el documento de la familia
  // (`g.documento`, que trae el universo sellado), la tabla, el modelo de
  // mutacion y la cache de rehechos, una por familia.
  //
  // Ninguna coincidencia con umbral 0 tiene una sola causa: LR NaN
  // (`NaN >= 0.0` es falso). Se informa como error explicito, no como celda
  // ausente, para que la planilla no quede con un hueco silencioso.
  def cotejar(
    g: GenotipificacionFamilia,
    frecuencias: Map[String, Map[Double, Double]],
    poi: profile.Profile,
    modelo: (Option[Long], Option[List[BayesianNetwork.MutationModelData]])
  ): Celda =
    try {
      val (tipoModelo, datosModelo) = modelo
      val coincidencias = PedigreeMatchingAlgorithm.findCompatibilityMatches(
        frequencyTable = frecuencias,
        p = poi,
        pG = g.documento,
        idPedigree = ID_PEDIGRI,
        assignee = RESPONSABLE,
        analysisType = TIPO_ANALISIS,
        mutationModelType = tipoModelo,
        mutationModelData = datosModelo,
        caseType = TIPO_DE_CASO,
        idCourtCase = ID_CASO,
        familyProfilesCrudos = g.perfiles,
        genogram = g.genograma,
        locusRangeMap = Map.empty,
        rebuildCache = Some(g.cache))
        .collect { case c: PedigreeCompatibilityMatch => c }

      coincidencias match {
        case Seq(c) =>
          val observaciones = if (c.message != null && c.message.nonEmpty) c.message else ""
          // El cero exacto es la exclusion. log10(0) ya da -Infinity en la JVM,
          // pero se escribe explicito para que la intencion no dependa de eso.
          CeldaOk(if (c.compatibility == 0.0) Double.NegativeInfinity else math.log10(c.compatibility),
            observaciones)
        case Seq() =>
          CeldaError("LR NaN: findCompatibilityMatches no devolvio coincidencia con umbral 0")
        case varias =>
          CeldaError(s"${varias.size} coincidencias para un solo POI")
      }
    } catch {
      // NonFatal, no Throwable: con 200 familias corriendo horas, un
      // OutOfMemoryError no puede quedar disfrazado de una celda "error"
      // mas -- tiene que tumbar la corrida para que se note. NonFatal
      // excluye los Error de la JVM y los deja propagarse.
      case NonFatal(ex) =>
        CeldaError(s"${ex.getClass.getSimpleName}: " +
          Option(ex.getMessage).getOrElse("sin mensaje"))
    }
}
