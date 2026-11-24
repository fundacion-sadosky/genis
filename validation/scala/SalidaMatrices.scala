package validation

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

// Escribe los dos archivos de matrices que vuelven a lrval:
// matrices_genis.csv y contexto_matrices.csv.

object SalidaMatrices {

  // Mismo implicito que el de Runner, por el mismo motivo: la entrega
  // usa terminador de linea LF y el DefaultCSVFormat de scala-csv usa
  // CRLF. El implicito tiene que estar en ESTE scope: el de Runner no sirve,
  // porque los implicitos se resuelven donde esta la llamada a
  // CSVWriter.open. Importa doble aca: el manifiesto se hashea sobre estos
  // archivos, asi que un \r cambia el hash.
  private implicit object FormatoDeSalida extends DefaultCSVFormat {
    override val lineTerminator: String = "\n"
  }

  val CABECERA_MATRICES = List(
    "origen", "id", "locus", "sexo", "alelo_origen", "alelo_destino", "probabilidad")

  val CABECERA_CONTEXTO = List(
    "origen", "id", "locus", "sexo", "alelo", "ki", "en_calibracion", "en_universo")

  // La representacion mas corta que reconstruye exactamente el mismo Double.
  // No se usa "%.17g": alcanza y sobra con el round-trip de la JVM, y de paso
  // no ensucia con ceros los valores que son exactos (0.995 se escribe
  // "0.995"). Del otro lado, as.numeric de R lo lee sin perder nada.
  def formatear(x: Double): String = java.lang.Double.toString(x)

  private def si(b: Boolean): String = if (b) "si" else "no"

  def escribir(dir: File, matrices: List[MatrizVolcada]): Unit = {
    dir.mkdirs()

    val wm = CSVWriter.open(new File(dir, "matrices_genis.csv"), "UTF-8")
    try {
      wm.writeRow(CABECERA_MATRICES)
      matrices.flatMap(_.celdas).foreach { c =>
        wm.writeRow(List(c.origen, c.id, c.locus, c.sexo,
          c.aleloOrigen, c.aleloDestino, formatear(c.probabilidad)))
      }
    } finally wm.close()

    val wc = CSVWriter.open(new File(dir, "contexto_matrices.csv"), "UTF-8")
    try {
      wc.writeRow(CABECERA_CONTEXTO)
      matrices.flatMap(_.contexto).foreach { x =>
        wc.writeRow(List(x.origen, x.id, x.locus, x.sexo, x.alelo,
          x.ki.bigDecimal.toPlainString, si(x.enCalibracion), si(x.enUniverso)))
      }
    } finally wc.close()
  }
}
