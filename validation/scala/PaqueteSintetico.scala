package validation

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

import scala.util.Random

// Genera un paquete masivo chico, valido y determinista. NO produce entrega:
// valida la plomeria, nunca los numeros.
object PaqueteSintetico {

  private implicit object FormatoLF extends DefaultCSVFormat {
    override val lineTerminator: String = "\n"
  }

  case class Forma(tipos: Int, familias: Int, poi: Int, loci: Int, conMutacion: Boolean)

  val CHICO = Forma(tipos = 3, familias = 4, poi = 20, loci = 4, conMutacion = false)
  val CHICO_CON_MUTACION = CHICO.copy(conMutacion = true)

  // Escalera entera de cuatro alelos: sirve igual para equal y para stepwise.
  private val ALELOS = List("10", "11", "12", "13")
  private val FRECS = Map("10" -> 0.4, "11" -> 0.3, "12" -> 0.2, "13" -> 0.1)

  // Tres topologias que cubren lo que el mapeo tiene que saber traducir:
  // fundadores tipificados, fundadores NO tipificados, y un hermano.
  private val TOPOLOGIAS: List[TipoPed] = List(
    TipoPed("t1", "padre y madre tipificados", "progenitores", List(
      IndividuoMasiva("AF", None, None, "M", Rol.Tipificado),
      IndividuoMasiva("MO", None, None, "F", Rol.Tipificado),
      IndividuoMasiva("MP", Some("AF"), Some("MO"), "M", Rol.BajoPrueba))),
    TipoPed("t2", "dos abuelos paternos", "abuelos", List(
      IndividuoMasiva("AP", None, None, "M", Rol.Tipificado),
      IndividuoMasiva("AM", None, None, "F", Rol.Tipificado),
      IndividuoMasiva("PA", Some("AP"), Some("AM"), "M", Rol.NoTipificado),
      IndividuoMasiva("MA", None, None, "F", Rol.NoTipificado),
      IndividuoMasiva("MP", Some("PA"), Some("MA"), "M", Rol.BajoPrueba))),
    TipoPed("t3", "un hermano", "hermanos", List(
      IndividuoMasiva("PP", None, None, "M", Rol.NoTipificado),
      IndividuoMasiva("MM", None, None, "F", Rol.NoTipificado),
      IndividuoMasiva("HE", Some("PP"), Some("MM"), "M", Rol.Tipificado),
      IndividuoMasiva("MP", Some("PP"), Some("MM"), "M", Rol.BajoPrueba))))

  private def loci(n: Int): List[String] = (1 to n).map(i => s"L$i").toList

  // Simula la familia: fundadores al azar del panel, no fundadores heredando
  // un alelo de cada progenitor. Devuelve individuo -> locus -> (a1, a2).
  private def simular(
    t: TipoPed, ls: List[String], r: Random
  ): Map[String, Map[String, (String, String)]] = {
    val orden = ordenTopologico(t)
    orden.foldLeft(Map.empty[String, Map[String, (String, String)]]) { (acc, i) =>
      val porLocus = ls.map { l =>
        val a1 = i.padre.map(p => unoDe(acc(p)(l), r)).getOrElse(sortear(r))
        val a2 = i.madre.map(m => unoDe(acc(m)(l), r)).getOrElse(sortear(r))
        l -> (a1, a2)
      }.toMap
      acc + (i.alias -> porLocus)
    }
  }

  // Progenitores antes que hijos. Las topologias son chicas y aciclicas.
  private def ordenTopologico(t: TipoPed): List[IndividuoMasiva] = {
    def profundidad(i: IndividuoMasiva): Int = {
      val padres = List(i.padre, i.madre).flatten
        .flatMap(a => t.individuos.find(_.alias == a))
      if (padres.isEmpty) 0 else padres.map(profundidad).max + 1
    }
    t.individuos.sortBy(profundidad)
  }

  private def sortear(r: Random): String = ALELOS(r.nextInt(ALELOS.size))
  private def unoDe(g: (String, String), r: Random): String = if (r.nextBoolean()) g._1 else g._2

  private def escribirCsv(dir: File, nombre: String, cabecera: List[String],
                          filas: List[List[String]]): Unit = {
    val w = CSVWriter.open(new File(dir, nombre), "UTF-8")
    try { w.writeRow(cabecera); filas.foreach(w.writeRow) } finally w.close()
  }

  def escribir(dir: File, forma: Forma, semilla: Long): Unit = {
    dir.mkdirs()
    val r = new Random(semilla)
    val ls = loci(forma.loci)
    val tipos = TOPOLOGIAS.take(forma.tipos)

    escribirCsv(dir, "tipos.csv", List("tipo", "etiqueta", "grado"),
      tipos.map(t => List(t.tipo, t.etiqueta, t.grado)))

    escribirCsv(dir, "pedigries.csv",
      List("tipo", "individuo", "padre", "madre", "sexo", "rol"),
      tipos.flatMap(t => t.individuos.map(i =>
        List(t.tipo, i.alias, i.padre.getOrElse(""), i.madre.getOrElse(""), i.sexo, i.rol))))

    val familias = (1 to forma.familias).map(n => (f"F$n%03d", tipos(n % tipos.size))).toList
    escribirCsv(dir, "familias.csv", List("familia", "tipo"),
      familias.map { case (f, t) => List(f, t.tipo) })

    escribirCsv(dir, "perfiles.csv",
      List("familia", "individuo", "locus", "alelo_1", "alelo_2"),
      familias.flatMap { case (f, t) =>
        val g = simular(t, ls, r)
        t.tipificados.flatMap(ind => ls.map(l =>
          List(f, ind, l, g(ind)(l)._1, g(ind)(l)._2)))
      })

    escribirCsv(dir, "poi.csv", List("poi", "locus", "alelo_1", "alelo_2"),
      (1 to forma.poi).toList.flatMap(n => ls.map(l =>
        List(f"P$n%04d", l, sortear(r), sortear(r)))))

    escribirCsv(dir, "frecuencias.csv", List("locus", "alelo", "frecuencia"),
      ls.flatMap(l => ALELOS.map(a => List(l, a, FRECS(a).toString))))

    if (forma.conMutacion) {
      escribirCsv(dir, "modelos.csv",
        List("locus", "sexo_progenitor", "modelo", "tasa", "tasa2", "rango", "alelos", "pasos"),
        ls.flatMap(l => List(
          List(l, "M", "stepwise", "0.001584", "0", "0.5", ALELOS.mkString(" "), "22"),
          List(l, "F", "stepwise", "0.000463", "0", "0.5", ALELOS.mkString(" "), "22"))))
    }

    escribirCsv(dir, "resultados.csv",
      List("familia", "poi", "log10LR", "estado", "observaciones"), Nil)

    // Firmado real, no solo dos comentarios: RunnerMasiva.correr/sondear
    // llaman a Paquete.verificarManifiesto ANTES de leer el paquete, igual
    // que el bloque 1, y un manifiesto sin entradas no verifica
    // ("MANIFIESTO.txt: no tiene entradas legibles"). Se listan los
    // archivos YA escritos hasta aca -- por eso el MANIFIESTO se escribe al
    // final -- y se reusa Paquete.sha256, el mismo hash que verifica el
    // lado de lectura.
    val archivos = dir.listFiles().filter(_.isFile).toList.sortBy(_.getName)
    val w = new java.io.PrintWriter(new File(dir, "MANIFIESTO.txt"), "UTF-8")
    try {
      w.print("# Paquete SINTETICO de humo - no es una entrega\n")
      w.print(s"# forma=$forma semilla=$semilla\n")
      archivos.foreach(f => w.print(s"${Paquete.sha256(f)}  ${f.getName}\n"))
    } finally w.close()
  }
}
