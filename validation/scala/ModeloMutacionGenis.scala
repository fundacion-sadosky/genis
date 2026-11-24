package validation

import pedigree.{BayesianNetwork, MutationModel, MutationModelKi, MutationModelParameter,
  MutationService}

import scala.math.BigDecimal.RoundingMode

// Traduce una parametrizacion a los argumentos de modelo de mutacion que
// consume BayesianNetwork.calculateProbability.
//
// El ki NO se calcula aca: la suma de normalizacion la calcula la funcion de
// GENis (MutationService.mutationNormalizationSum). Si el arnes la
// reimplementara, estaria validando la formula de GENis contra una copia suya.
//
// La lista de ki que arma `datos` es ademas PROVISORIA: el motor la reemplaza
// antes de armar ninguna tabla de probabilidad
// (MutationService.recalibrateKis, llamada desde
// BayesianNetwork.getGenotypification y getGenotypificationWithMarkers), y
// VolcadoMatrices.volcarConModelo espeja esa recalibracion. Cumple el mismo
// papel que la lista que el servicio de GENis persiste: se lee y se descarta.

case class ParametrosMutacion(
  locus: String,
  sexo: String,                 // "M" | "F" | "I"
  modelo: String,               // "equal" | "stepwise" | "stepwise_extendido"
  tasa: Double,
  tasa2: Double,
  rango: Option[Double],
  pasos: Int,
  kiRedondeado: Boolean
)

object ModeloMutacionGenis {

  // Identificadores de tipo de modelo de mutacion de GENis. En el 3
  // ("Stepwise extended"; BayesianNetwork.getProbabityOfNonDiagonal, caso
  // (3, ...)) los vecinos de la misma clase decimal usan la formula y la
  // k_i del tipo 2, y mutationRateMicrovariant se reparte por igual entre los
  // alelos de otra clase, como pedmut::mutationMatrix("stepwise", rate2) y el
  // "Extended stepwise" de Familias.
  val TIPO: Map[String, Long] = Map(
    "equal" -> 1L,
    "stepwise" -> 2L,
    "stepwise_extendido" -> 3L)

  // La suma de normalizacion la calcula GENis. Lo unico que hace el arnes es
  // la division y, si se lo piden, el redondeo a 8 decimales que
  // MutationService.getMutationModelKit aplica antes de persistir.
  //
  // Por que `redondear` es un parametro: grilla.csv declara ese eje en la
  // columna `ki_redondeado`. El motor no lee la constante persistida: la
  // recalibra en memoria sobre el universo vigente, sin redondeo, y el
  // volcado espeja esa recalibracion. Por eso las dos variantes de la grilla
  // producen la MISMA matriz del lado de GENis. El parametro se conserva
  // porque grilla.csv declara ese eje y porque getMutationModelKit redondea
  // lo que persiste, pero no cambia ninguna celda; lo que el arnes entrega en
  // `ki` es la constante que se persiste, no la que el motor usa.
  def kiDe(
    alelo: Double,
    calibracion: List[Double],
    tasa: Double,
    rango: Double,
    pasos: Int,
    redondear: Boolean
  ): BigDecimal = {
    val suma = MutationService.mutationNormalizationSum(alelo, calibracion, rango, pasos.toLong)
    if (suma == 0.0) BigDecimal(0)
    else {
      val ki = BigDecimal(tasa) / BigDecimal(suma)
      if (redondear) ki.setScale(8, RoundingMode.HALF_EVEN) else ki
    }
  }

  def datos(
    p: ParametrosMutacion,
    calibracion: List[Double]
  ): BayesianNetwork.MutationModelData = {
    val cabecera = MutationModel(
      id = 1L,
      name = s"validacion-${p.modelo}",
      mutationType = TIPO.getOrElse(p.modelo,
        throw new IllegalArgumentException(s"modelo desconocido: ${p.modelo}")),
      active = true,
      // El motor decide CON QUE SEXO buscar el parametro en getSex
      // (BayesianNetwork.scala): si algun modelo de la lista trae
      // ignoreSex = false, usa el sexo que deduce del sufijo del nombre de la
      // variable (_p -> M, _m -> F); solo si todos lo traen en true busca con
      // "I". Como los nombres de variable de un CPT real SIEMPRE llevan
      // sufijo, un parametro declarado con sexo "I" y la marca en false no se
      // encuentra (findMutationParam no matchea) y el caso no se calcularia
      // con el modelo declarado. Por eso la marca se deriva del sexo
      // declarado en vez de fijarse.
      ignoreSex = p.sexo == "I",
      cantSaltos = p.pasos.toLong)

    val parametro = MutationModelParameter(
      id = 1L,
      idMutationModel = 1L,
      locus = p.locus,
      sex = p.sexo,
      mutationRate = Some(BigDecimal(p.tasa)),
      mutationRange = p.rango.map(BigDecimal(_)),
      mutationRateMicrovariant = Some(BigDecimal(p.tasa2)))

    // Equal no usa ki: su celda vale tasa/(n-1) y no pasa por la constante.
    // El tipo 3 usa la MISMA k_i que el 2 para sus vecinos de la misma clase
    // decimal (MutationService.recalibrateKis la recalibra para los dos), asi
    // que la lista provisoria y las guardas son las mismas.
    val kis =
      if (cabecera.mutationType == 2L || cabecera.mutationType == 3L) {
        // p.rango.getOrElse(0.0) degradaria esto en silencio: un stepwise sin
        // rango declarado daria tasa efectiva 0 en kiDe (Math.pow(0.0, ...) es
        // 0 para toda distancia > 0) y la matriz resultante seria la
        // identidad, sin ningun error que lo delate. Mejor reventar aca, con
        // el id de la fila a la vista de quien llama.
        require(p.rango.isDefined,
          s"modelo ${p.modelo} (locus=${p.locus}, sexo=${p.sexo}) sin rango definido")
        // Defensa en profundidad: con `universo.getOrElse(m.locus, Nil)`
        // una calibracion vacia produce un modelo ESTRUCTURALMENTE VALIDO con
        // todos los ki en 0 -- revierte cada LR al mendeliano estricto sin
        // ningun error. La invariante de
        // PaqueteMasiva.leer (panel x {M,F} completo) ya deberia impedir que
        // esto ocurra desde el paquete; esta guarda cubre a quien llame
        // `datos` directo, sin pasar por esa lectura.
        require(calibracion.nonEmpty,
          s"modelo ${p.modelo} (locus=${p.locus}, sexo=${p.sexo}) sin escalera de " +
            "calibracion (el locus no esta en el universo/panel)")
        calibracion.map(a => MutationModelKi(
          id = 0L,
          idMutationModelParameter = 1L,
          allele = a,
          ki = kiDe(a, calibracion, p.tasa, p.rango.get, p.pasos, p.kiRedondeado)))
      } else Nil

    (parametro, kis, cabecera)
  }

  // Traduce los modelos declarados a los argumentos de mutacion del motor.
  // Vive aca y no en Corredor porque los dos bloques la necesitan: el
  // catalogo (una parametrizacion por caso) y la busqueda masiva (una sola
  // para todo el escenario). Duplicar las cinco guardas garantizaria que en
  // algun momento diverjan.
  //
  // Las tres cosas que el paquete puede declarar y GENis no puede expresar se
  // INFORMAN, no se fuerzan: un numero obtenido forzando la entrada produce
  // una falla inatribuible.
  //
  // Un `modelos` vacio significa "sin modelo declarado": el caso corre con
  // None, es decir el LR mendeliano estricto, sin pasar por ninguna guarda.
  //
  // Los mensajes de las guardas son generales porque esta funcion es
  // compartida: no dicen "en un mismo caso" (vocabulario del bloque 1;
  // la busqueda masiva no tiene casos, tiene un unico modelo para todo el
  // escenario) ni nombran `modelos.csv`. Lo segundo no es porque la busqueda
  // masiva no lea ese archivo -- lo lee, PaqueteMasiva.leer lo carga cuando
  // existe -- sino porque esta funcion recibe una
  // `List[ModeloDeclarado]` ya parseada y no sabe de que archivo salio, ni
  // siquiera si salio de un archivo: nombrar uno concreto en el mensaje de
  // un traductor que no lee archivos es responsabilidad del lector, no de
  // quien traduce. El contexto de que se trata sigue presente: lo aporta
  // `etiqueta`, que en el bloque 1 es el id del caso.
  def desdeDeclarados(
    etiqueta: String,
    modelos: List[ModeloDeclarado],
    universo: Map[String, List[Double]]
  ): Either[String, (Option[Long], Option[List[BayesianNetwork.MutationModelData]])] = {
    if (modelos.isEmpty) Right((None, None))
    else {
      val desconocidos = modelos.map(_.modelo).distinct.filterNot(TIPO.contains)
      val sexos = modelos.map(_.sexoProgenitor).distinct
      val sinPasos =
        modelos.filter(m => m.modelo != "equal" && m.pasos.isEmpty).map(_.locus).distinct

      if (desconocidos.nonEmpty)
        Left(s"$etiqueta: GENis no implementa el modelo ${desconocidos.sorted.mkString(", ")}")
      else if (modelos.map(m => TIPO(m.modelo)).distinct.size > 1)
        Left(s"$etiqueta: declara modelos de tipo distinto; GENis tiene un solo tipo por " +
          "modelo de mutacion")
      else if (sexos.contains("I") && sexos.size > 1)
        Left(s"$etiqueta: mezcla el sexo indistinto con sexos concretos " +
          s"(${sexos.sorted.mkString(", ")}); GENis no puede expresar las dos cosas en un " +
          "mismo modelo")
      else if (sinPasos.nonEmpty)
        Left(s"$etiqueta: el modelo declarado trunca y no se declara 'pasos' en " +
          sinPasos.sorted.mkString(", "))
      else {
        val tipos = modelos.map(m => TIPO(m.modelo)).distinct
        // Nombrada `filas` y no `datos`: dentro de este objeto ese nombre
        // taparia a la funcion `datos` que se invoca aca abajo.
        val filas = modelos.map { m =>
          datos(ParametrosMutacion(
            locus = m.locus,
            sexo = m.sexoProgenitor,
            modelo = m.modelo,
            tasa = m.tasa,
            tasa2 = m.tasa2.getOrElse(0.0),
            rango = m.rango,
            pasos = m.pasos.getOrElse(0),
            kiRedondeado = false),
            universo.getOrElse(m.locus, Nil))
        }
        Right((Some(tipos.head), Some(filas)))
      }
    }
  }
}
