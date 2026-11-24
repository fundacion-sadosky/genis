package validation

// Informe de contraste contra LR_referencia, el valor de referencia que
// viaja en casos.csv.
//
// El arnes no USA LR_referencia para calcular -- eso convertiria la validacion
// en una tautologia -- pero de eso no se sigue que no deba INFORMAR la
// discrepancia. Sin este informe, para saber si los numeros entregados estan
// bien habria que depender por entero del clasificador del modulo de referencia.
// Este contraste y el veredicto del informe de la referencia deben coincidir
// caso por caso: es una verificacion cruzada independiente, y una diferencia
// entre los dos hay que reportarla.
//
// Este modulo no participa del calculo (Corredor/MapeoGenis no lo importan):
// solo lo usa Runner, despues de calcular, para escribir
// validation/out/contraste.txt.
object Contraste {

  sealed trait Veredicto
  case object Pasa extends Veredicto
  case object Revisar extends Veredicto
  case object Falla extends Veredicto
  // El LR no es el de referencia pero si, exactamente, el de la otra
  // convencion documentada del caso (LR_otra_convencion).
  case object Convencion extends Veredicto
  case object SinReferencia extends Veredicto

  case class Fila(
    caso: String,
    lrObtenido: Double,
    lrReferencia: Option[Double],
    discrepanciaRelativa: Option[Double],
    veredicto: Veredicto
  )

  private val UMBRAL_PASA = 1e-9
  private val UMBRAL_REVISAR = 1e-6

  // (disc_rel, veredicto) de un LR contra un valor: disc_rel =
  // |LR_obs - LR_ref| / max(|LR_ref|, 1e-300). Los dos ceros dan PASA
  // (exclusion coincidente); que uno solo sea cero da FALLA (no hay disc_rel
  // bien definida ahi, y no es un empate).
  private def contra(lr: Double, ref: Double): (Option[Double], Veredicto) =
    if (ref == 0.0 && lr == 0.0) (Some(0.0), Pasa)
    else if (ref == 0.0 || lr == 0.0) (None, Falla)
    else {
      val discRel = math.abs(lr - ref) / math.max(math.abs(ref), 1e-300)
      val v = if (discRel <= UMBRAL_PASA) Pasa else if (discRel <= UMBRAL_REVISAR) Revisar else Falla
      (Some(discRel), v)
    }

  // Sin referencia, SIN_REFERENCIA. Un LR que no pasa contra la referencia
  // pero coincide con LR_otra_convencion (misma tolerancia que PASA) es
  // CONVENCION; la disc_rel que se informa es siempre la medida contra
  // LR_referencia.
  def evaluar(caso: Caso, lrObtenido: Double): Fila = caso.lrReferencia match {
    case None =>
      Fila(caso.id, lrObtenido, None, None, SinReferencia)

    case Some(ref) =>
      val (discRel, v) = contra(lrObtenido, ref)
      val otra = caso.lrOtraConvencion.exists(alt => contra(lrObtenido, alt)._2 == Pasa)
      Fila(caso.id, lrObtenido, Some(ref), discRel, if (v != Pasa && otra) Convencion else v)
  }

  // Publico: el mismo texto ("PASA", "FALLA", ...) que contraste.txt
  // informa, para que cualquier otro consumidor coincida palabra por palabra.
  def textoDe(v: Veredicto): String = v match {
    case Pasa          => "PASA"
    case Revisar       => "REVISAR"
    case Falla         => "FALLA"
    case Convencion    => "CONVENCION"
    case SinReferencia => "SIN_REFERENCIA"
  }

  private def col(s: String, ancho: Int): String = s.padTo(ancho, ' ')

  // Separador minimo entre columnas. El ancho de cada columna no viene de un
  // numero fijo que un valor largo pueda desbordar: sale del contenido real
  // (el maximo entre el encabezado y todas las filas) mas este separador, asi
  // que siempre queda al menos este espacio entre una columna y la que sigue,
  // sea cual sea el largo del valor.
  private val SEPARADOR = 2

  // Texto plano, una linea por caso mas un resumen al pie con los totales
  // por veredicto. No es CSV: es un informe de lectura humana, aparte del
  // CSV de entrega (no lo contamina).
  def informe(filas: List[Fila]): String = {
    val encabezadoCeldas = List("caso", "LR_obtenido", "LR_referencia", "disc_rel", "veredicto")

    val cuerpoCeldas = filas.map { f =>
      List(
        f.caso,
        f.lrObtenido.toString,
        f.lrReferencia.map(_.toString).getOrElse("-"),
        f.discrepanciaRelativa.map(_.toString).getOrElse("-"),
        textoDe(f.veredicto))
    }

    val todasLasFilas = encabezadoCeldas :: cuerpoCeldas

    // Un ancho por columna, salvo la ultima (veredicto), que no necesita
    // relleno porque no hay ninguna columna despues que se le pueda pegar.
    val anchos = (0 until encabezadoCeldas.size - 1).map { i =>
      todasLasFilas.map(fila => fila(i).length).max + SEPARADOR
    }

    def formatearFila(celdas: List[String]): String =
      celdas.init.zip(anchos).map { case (c, ancho) => col(c, ancho) }.mkString + celdas.last

    val encabezado = formatearFila(encabezadoCeldas)
    val cuerpo = cuerpoCeldas.map(formatearFila)

    val porVeredicto = filas.groupBy(f => textoDe(f.veredicto)).map { case (v, fs) => v -> fs.size }
    val totales = List("PASA", "CONVENCION", "REVISAR", "FALLA", "SIN_REFERENCIA")
      .map(v => s"$v=${porVeredicto.getOrElse(v, 0)}")
      .mkString(", ")

    (encabezado :: cuerpo).mkString("\n") +
      s"\n\nTotales: ${filas.size} casos calculados. $totales\n"
  }
}
