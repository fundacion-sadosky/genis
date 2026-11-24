package validation

import java.io.File
import java.nio.file.{Files, StandardCopyOption}

import com.github.tototoshi.csv.{CSVReader, CSVWriter, DefaultCSVFormat}

case class FilaMasiva(
  familia: String,
  poi: String,
  log10LR: Option[Double], // None = no se calculo; ver `estado`
  estado: String,          // ok | no_soportado | no_representable | error
  observaciones: String
)

case class ResumenFamilia(
  familia: String,
  tipo: String,
  celdas: Int,
  calculados: Int,
  excluidos: Int,
  maximo: Option[Double],
  mediana: Option[Double]
)

// Escritura por familia con reanudacion, y armado final ordenado. La familia es
// la unidad de entrega (regla 4: una familia entrega todas sus comparaciones
// o ninguna), de checkpoint y de paralelismo:
// las tres coinciden, y por eso "familia terminada" es simplemente "el archivo
// parcial existe".
object SalidaMasiva {

  val CABECERA = List("familia", "poi", "log10LR", "estado", "observaciones")

  // El paquete usa LF en todos sus archivos, pero DefaultCSVFormat de
  // scala-csv usa CRLF. Este implicito, mas cercano, lo tapa. Solo se pisa el
  // terminador de linea: el quoting queda en QUOTE_MINIMAL (el default de
  // DefaultCSVFormat) A PROPOSITO, no por omision. Es lo que ya produce el
  // bloque 1 (Runner.scala, SalidaMatrices.scala -- ninguno de los dos pisa
  // `quoting`) y lo que el modulo de referencia consume
  // (ver validation/out/resultados_genis.csv: sin comillas). QUOTE_MINIMAL
  // cita un campo solo si contiene coma, comilla o salto de linea, que es
  // justo lo que hace falta para `observaciones`, donde los motivos de
  // MapeoMasiva se separan con ", " y pueden traer coma.
  //
  // No-private a proposito: es el implicito mas cercano dentro de
  // SalidaMasiva y ademas se reusa desde RunnerMasiva.sondear, que escribe
  // su propio CSV con LF.
  // La alternativa -- declarar un segundo FormatoLF en RunnerMasiva -- es
  // el mismo terminador escrito dos veces, con el riesgo de que un dia
  // diverjan sin que nada lo note.
  implicit object FormatoLF extends DefaultCSVFormat {
    override val lineTerminator: String = "\n"
  }

  // NUNCA String.format: usa el locale por defecto de la JVM, y uno como
  // LC_NUMERIC=es_AR.UTF-8 produciria coma decimal, que el modulo de
  // referencia no lee.
  // Double.toString es independiente del locale por definicion.
  def formatear(x: Double): String =
    if (x.isNegInfinity) "-Inf"
    else if (x.isPosInfinity) "Inf"
    else x.toString

  private def parcial(dir: File, familia: String) = new File(dir, s"$familia.csv")

  def yaHecha(dir: File, familia: String): Boolean = parcial(dir, familia).exists()

  // Escribe a .tmp y renombra. El rename es atomico, asi que la existencia del
  // archivo final significa "familia completa" sin ningun registro aparte.
  def escribirFamilia(dir: File, familia: String, filas: List[FilaMasiva]): Unit = {
    dir.mkdirs()
    val tmp = new File(dir, s"$familia.csv.tmp")
    val w = CSVWriter.open(tmp, "UTF-8")
    try filas.sortBy(_.poi).foreach(f => w.writeRow(List(
      f.familia, f.poi, f.log10LR.map(formatear).getOrElse(""), f.estado, f.observaciones)))
    finally w.close()
    val destino = parcial(dir, familia)
    // Files.move con REPLACE_EXISTING + ATOMIC_MOVE reemplaza el destino de
    // un solo golpe (rename() de POSIX es atomico incluso si el destino ya
    // existe). Un destino.delete() seguido de tmp.renameTo(destino)
    // abriria una ventana propia: si el proceso muriera justo entre el
    // delete y el renameTo, una familia YA COMPLETA quedaria sin archivo
    // final y la reanudacion la recalcularia entera. Explicito, portable, sin
    // esa ventana.
    try {
      Files.move(tmp.toPath, destino.toPath,
        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      ()
    } catch {
      case e: java.io.IOException =>
        throw new RuntimeException(
          s"no se pudo mover ${tmp.getPath} a ${destino.getPath}", e)
    }
  }

  // Concatena en orden de familia; dentro de cada parcial las filas ya estan
  // ordenadas por POI. Dos corridas con el mismo paquete dan el mismo archivo,
  // que es lo que hace comparable un MANIFIESTO.
  //
  // Una familia sin parcial NO se saltea en silencio: la regla 4 -- una
  // familia entrega todas sus comparaciones o ninguna -- se hace cumplir
  // aca. Se exige, ANTES de escribir una sola linea de
  // `destino`, que todas las `familias` pedidas tengan su parcial: si falta
  // alguna, se aborta ruidoso nombrandola, en vez de entregar un archivo mas
  // corto que parece completo. El cruce contra `familias.size * poi.size` lo
  // hace RunnerMasiva.correr, que es quien conoce cuantos POI hay; esta
  // funcion no los recibe y no le corresponde inventarlos.
  def armar(dirParcial: File, destino: File, familias: List[String]): Long = {
    val faltantes = familias.filterNot(fam => parcial(dirParcial, fam).exists())
    if (faltantes.nonEmpty)
      throw new IllegalArgumentException(
        "regla 4 violada -- falta la particion de: " + faltantes.sorted.mkString(", "))

    Option(destino.getParentFile).foreach(_.mkdirs())
    val w = CSVWriter.open(destino, "UTF-8")
    var escritas = 0L
    try {
      w.writeRow(CABECERA)
      familias.sorted.foreach { fam =>
        val f = parcial(dirParcial, fam)
        val r = CSVReader.open(f, "UTF-8")
        // r.foreach recorre con el iterador de CSVReader, fila por fila,
        // en vez de r.all() (que arma un List[List[String]] con el parcial
        // entero antes de escribir la primera linea). Con hasta 5000 filas
        // por parcial no cambia el resultado, pero es la version que no
        // paga esa materializacion de mas para nada.
        try r.foreach { fila => w.writeRow(fila); escritas += 1 } finally r.close()
      }
    } finally w.close()
    escritas
  }

  private def parseLog10LR(s: String): Option[Double] =
    Some(s).filter(_.nonEmpty).map {
      case "-Inf" => Double.NegativeInfinity
      case "Inf" => Double.PositiveInfinity
      case x => x.toDouble
    }

  // La mediana que se reporta es la ALTA: para N par es el mayor de los dos
  // valores centrales del ordenado, no el promedio de ambos. Es deliberado,
  // no un descuido -- esta columna es para leer una corrida sin abrir el
  // millon de filas (una senal de "algo anda raro en esta familia"), no un
  // valor que se publique, y con miles de celdas por familia la diferencia
  // entre las dos definiciones no tiene consecuencia practica.
  private def medianaAlta(ordenados: Seq[Double]): Option[Double] =
    if (ordenados.isEmpty) None else Some(ordenados(ordenados.size / 2))

  // Relee la entrega ya escrita, entera, en memoria. A proposito: el
  // consumidor de esta funcion es el puente, que necesita la
  // distribucion COMPLETA de log10LR por tipo de familia para calcular
  // deciles, no un agregado -- no hay forma de calcular un decil sin tener
  // todos los valores. Para 200 familias x 5000 POI (1 millon de filas) esto
  // es aceptable una sola vez por corrida; lo que no seria aceptable es que
  // `resumen` pasara por aca solo para reducir a siete numeros por familia,
  // que es exactamente por lo que `resumenDesdeArchivo` no usa esta funcion
  // y dobla sobre el archivo sin materializarlo.
  def leerFilas(origen: File): List[FilaMasiva] = {
    val r = CSVReader.open(origen, "UTF-8")
    try r.allWithHeaders().map { f =>
      FilaMasiva(f("familia"), f("poi"), parseLog10LR(f("log10LR")), f("estado"), f("observaciones"))
    } finally r.close()
  }

  // Resume una lista de filas ya en memoria. Para quien ya
  // tenga las filas en la mano por otro motivo; el orquestador usa
  // `resumenDesdeArchivo`, que hace lo mismo sin necesitar la lista completa.
  def resumen(filas: List[FilaMasiva], tipoDe: Map[String, String]): List[ResumenFamilia] =
    filas.groupBy(_.familia).toList.sortBy(_._1).map { case (fam, fs) =>
      val finitos = fs.flatMap(_.log10LR).filterNot(_.isInfinite).sorted
      ResumenFamilia(
        familia = fam,
        tipo = tipoDe.getOrElse(fam, ""),
        celdas = fs.size,
        calculados = fs.count(_.estado == "ok"),
        excluidos = fs.count(_.log10LR.exists(_.isNegInfinity)),
        maximo = finitos.lastOption,
        mediana = medianaAlta(finitos))
    }

  // Version que dobla sobre el archivo SIN materializarlo: usa el iterador
  // de CSVReader (una fila por vez, via iteratorWithHeaders) y acumula por
  // familia a medida que avanza, aprovechando que `armar` deja las familias
  // contiguas y ordenadas -- el mismo orden que promete el comentario de
  // clase. En ningun momento hay mas de una familia de valores en memoria a
  // la vez (hasta 5000 Double, no 1 millon de FilaMasiva con sus cuatro
  // campos de texto). Esta es la que usa el orquestador.
  def resumenDesdeArchivo(origen: File, tipoDe: Map[String, String]): List[ResumenFamilia] = {
    val r = CSVReader.open(origen, "UTF-8")
    try {
      val terminados = scala.collection.mutable.ListBuffer.empty[ResumenFamilia]

      var familiaActual: Option[String] = None
      var celdas = 0
      var calculados = 0
      var excluidos = 0
      val finitos = scala.collection.mutable.ListBuffer.empty[Double]

      def cerrarFamilia(fam: String): Unit = {
        val ordenados = finitos.sorted
        terminados += ResumenFamilia(
          familia = fam,
          tipo = tipoDe.getOrElse(fam, ""),
          celdas = celdas,
          calculados = calculados,
          excluidos = excluidos,
          maximo = ordenados.lastOption,
          mediana = medianaAlta(ordenados))
        ()
      }

      r.iteratorWithHeaders.foreach { f =>
        val fam = f("familia")
        if (!familiaActual.contains(fam)) {
          familiaActual.foreach(cerrarFamilia)
          familiaActual = Some(fam)
          celdas = 0; calculados = 0; excluidos = 0; finitos.clear()
        }
        val log = parseLog10LR(f("log10LR"))
        celdas += 1
        if (f("estado") == "ok") calculados += 1
        if (log.exists(_.isNegInfinity)) excluidos += 1
        log.filterNot(_.isInfinite).foreach(finitos += _)
      }
      familiaActual.foreach(cerrarFamilia)

      terminados.toList
    } finally r.close()
  }

  def escribirResumen(destino: File, r: List[ResumenFamilia]): Unit = {
    Option(destino.getParentFile).foreach(_.mkdirs())
    val w = CSVWriter.open(destino, "UTF-8")
    try {
      w.writeRow(List("familia", "tipo", "celdas", "calculados", "excluidos", "maximo", "mediana"))
      r.foreach(x => w.writeRow(List(x.familia, x.tipo, x.celdas.toString, x.calculados.toString,
        x.excluidos.toString, x.maximo.map(formatear).getOrElse(""),
        x.mediana.map(formatear).getOrElse(""))))
    } finally w.close()
  }
}
