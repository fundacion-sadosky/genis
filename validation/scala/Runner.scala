package validation

import java.io.File

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}

// Corre el catalogo completo -- los casos con modelo de mutacion declarado
// incluidos -- y escribe resultados_genis.csv con una fila por caso
// informado, sea que el motor le haya dado un LR o no (ver Observaciones
// para el motivo en cada no_soportado/no_representable/error).
// Si el paquete trae ademas grilla.csv y
// paneles.csv, esta misma corrida vuelca tambien las matrices de mutacion
// -- las de la grilla y las de los casos con modelo, juntas en
// matrices_genis.csv -- porque volcarlas aparte, en otra corrida,
// desconectaria la matriz del LR que la uso (ver Corredor.volcarDelCaso).
object Runner {

  val CABECERA = List("caso", "sistema", "version", "LR", "log10LR", "estado", "observaciones")

  // El paquete usa terminador de linea LF en todos sus archivos, pero
  // DefaultCSVFormat de scala-csv usa "\r\n" (CRLF). Este implicito, definido
  // en el scope de
  // Runner, tapa al `defaultCSVFormat` que trae el package object de
  // scala-csv (un implicito de scope mas cercano gana), asi que
  // CSVWriter.open(...) mas abajo lo toma sin tener que pasarlo a mano.
  private implicit object FormatoDeSalida extends DefaultCSVFormat {
    override val lineTerminator: String = "\n"
  }

  private def formatear(lr: Double): String =
    if (lr == 0.0) "0"                    // exclusion: el cero exacto
    // No hay una rama con "%.1f".format(lr) para los LR enteros exactos, a
    // proposito. String.format usa el locale POR DEFECTO de la JVM, y con
    // LC_NUMERIC=es_AR.UTF-8 "%.1f".format(1.0) da "1,0", con COMA. La
    // entrega exige punto decimal: el modulo de referencia no lee un valor
    // con coma decimal. Double.toString es independiente del locale
    // por definicion (usa siempre punto) y ya da "2.5" y "1.0". No usar
    // String.format en ningun formateo numerico de este archivo.
    else lr.toString

  // Hash corto del commit actual, con sufijo -dirty si el arbol de trabajo
  // no esta limpio. `version` tiene que identificar univocamente que codigo
  // produjo los numeros de la entrega --
  // version del artefacto sola no alcanza, y el hash solo tampoco: los
  // numeros de un commit con archivos modificados encima no se reproducen
  // haciendo checkout de ese commit. El veredicto vale solo para las
  // versiones que la entrega declara.
  //
  // La obtencion es read-only (`git rev-parse` y `git status
  // --porcelain`, ninguno de los dos toca el repositorio) y TOLERANTE A
  // FALLOS: el arnes tiene que poder seguir produciendo una entrega aunque
  // se corra desde un tarball sin repositorio git o en una maquina sin el
  // binario `git` instalado. Si `git rev-parse` falla, se devuelve el
  // marcador explicito "sin-commit". Si el commit se pudo
  // obtener pero `git status --porcelain` falla por algun motivo, se
  // devuelve el hash solo, sin sufijo -dirty: es mejor un hash sin la
  // advertencia de suciedad que ninguna entrega, y la falla de `git status`
  // encima de un `git rev-parse` que si funciono es un escenario tan raro
  // que no vale la pena que tumbe la corrida.
  def hashDeCommit(): String =
    try {
      import scala.sys.process._
      val hash = Seq("git", "rev-parse", "--short", "HEAD").!!.trim
      if (hash.isEmpty) "sin-commit"
      else {
        // "Sucio" tiene que significar "el arbol NO reproduce este commit".
        // Un `git status --porcelain` pelado no sirve para eso: cuenta tambien
        // los archivos sin trackear, y con uno solo que no tenga que ver con
        // el calculo la marca quedaria encendida sin distinguir nada. Se mide
        // entonces en dos partes:
        //   - modificaciones de archivos YA trackeados, excluyendo la carpeta de
        //     salida: lo que el arnes genera no es entrada del calculo y cambia
        //     en cada corrida;
        //   - archivos SIN TRACKEAR dentro de validation/scala, que si son
        //     entrada: un .scala nuevo ahi entra a la compilacion sin estar en
        //     el commit, y la corrida dejaria de ser reproducible.
        val sucio =
          try {
            val trackeadosModificados = Seq(
              "git", "status", "--porcelain", "--untracked-files=no",
              "--", ".", ":(exclude)validation/out"
            ).!!.trim.nonEmpty
            val fuentesSinTrackear = Seq(
              "git", "status", "--porcelain", "--untracked-files=all",
              "--", "validation/scala"
            ).!!.split("\n").exists(_.startsWith("??"))
            trackeadosModificados || fuentesSinTrackear
          } catch { case _: Throwable => false }
        if (sucio) s"$hash-dirty" else hash
      }
    } catch {
      case _: Throwable => "sin-commit"
    }

  def filaDe(c: Caso, r: Resultado, version: String): List[String] = {
    val (lr, estado, motivo) = r match {
      case Calculado(v, _)     => (formatear(v), "ok", "")
      case NoSoportado(m)      => ("", "no_soportado", m)
      case NoRepresentable(m)  => ("", "no_representable", m)
      case Fallo(m)            => ("", "error", m)
    }
    // El motivo que ya deja Corredor (vacio para Calculado) se
    // completa con la tabla de anotaciones propia del arnes, para que dos
    // filas con el mismo LR ("0") no queden indistinguibles cuando una es
    // una exclusion mendeliana y la otra sale de otra convencion de lectura
    // (ver Observaciones).
    val obs = Observaciones.combinar(c, motivo)
    List(c.id, "GENis", version, lr, "", estado, obs)
  }

  // En modo grilla el marcador es un rotulo interno nada mas: ParametrosMutacion
  // lo necesita para armar el nombre de variable que consulta el motor, pero
  // no identifica ningun locus real del catalogo, por eso la columna `locus`
  // del archivo de salida va vacia a proposito (ver el `locus = ""` mas abajo).
  private val LOCUS_DE_GRILLA = "L1"

  // Vuelca la matriz de mutacion de cada fila de la
  // grilla. No corre ningun LR: es la medicion del nivel del modelo, y se
  // entrega sola.
  private def grillaVolcada(dirPaquete: File): List[MatrizVolcada] = {
    val (paneles, grilla) = GrillaMutacion.leer(dirPaquete)
    val fallas = GrillaMutacion.verificarInvariantes(paneles, grilla)
    if (fallas.nonEmpty)
      throw new IllegalStateException(
        s"la grilla viola sus invariantes:\n${fallas.map("  - " + _).mkString("\n")}")

    grilla.map { f =>
      val universoP = paneles(f.panelUniverso)
      val calibracionP = paneles(f.panelCalibracion)
      val nombres = (universoP.alelos ++ calibracionP.alelos).toSet
      val codigos = MapeoGenis.mapearAlelos(nombres) match {
        case Right(m) => m
        case Left(motivo) => throw new IllegalStateException(s"${f.id}: $motivo")
      }
      val grafia = codigos.map { case (nombre, codigo) => codigo -> nombre }

      VolcadoMatrices.volcar(
        origen = "grilla",
        id = f.id,
        locus = "",
        p = ParametrosMutacion(
          locus = LOCUS_DE_GRILLA,
          sexo = f.sexo,
          modelo = f.modelo,
          tasa = f.tasa,
          tasa2 = f.tasa2,
          rango = f.rango,
          pasos = f.pasos.getOrElse(0),
          kiRedondeado = f.kiRedondeado),
        universo = universoP.alelos.map(codigos),
        calibracion = calibracionP.alelos.map(codigos),
        grafia = grafia)
    }
  }

  def correrGrilla(dirPaquete: File, dirSalida: File): Int = {
    val matrices = grillaVolcada(dirPaquete)
    SalidaMatrices.escribir(dirSalida, matrices)
    println(s"Volcadas ${matrices.size} matrices de grilla en ${dirSalida.getAbsolutePath}")
    matrices.size
  }

  // La linea de progreso no vuelca el Resultado entero: con modelo de
  // mutacion, `Calculado.matrices` trae la matriz completa y la linea
  // ocuparia miles de caracteres. La matriz ya viaja, bien
  // formateada, en matrices_genis.csv; aca alcanza con decir cuantas hay.
  private def resumen(r: Resultado): String = r match {
    case Calculado(lr, Nil)      => s"Calculado($lr)"
    case Calculado(lr, _ :: Nil) => s"Calculado($lr, 1 matriz)"
    case Calculado(lr, ms)       => s"Calculado($lr, ${ms.size} matrices)"
    case otro                    => otro.toString
  }

  def main(args: Array[String]): Unit = {
    val dirPaquete = new File(if (args.length > 0) args(0) else "validation/cases")
    val dirSalida = new File(if (args.length > 1) args(1) else "validation/out")
    dirSalida.mkdirs()

    val fallasManifiesto = Paquete.verificarManifiesto(dirPaquete)
    if (fallasManifiesto.nonEmpty) {
      println("MANIFIESTO.txt no verifica; se aborta la corrida:")
      fallasManifiesto.foreach(f => println(s"  - $f"))
      sys.exit(1)
    }

    val casos = Paquete.leer(dirPaquete)
    val fallasInvariantes = Paquete.verificarInvariantes(casos)
    if (fallasInvariantes.nonEmpty) {
      println("El paquete viola sus propios invariantes; se aborta la corrida:")
      fallasInvariantes.foreach(f => println(s"  - $f"))
      sys.exit(1)
    }

    // version lleva tres datos: version del
    // artefacto, hash del commit, y la anotacion de que se
    // corrio por el motor directo.
    val version = s"${scala.util.Properties.propOrElse("genis.version", "5.1.15")}" +
      s"-${hashDeCommit()}-motor-directo"
    // No se filtra por modelo de mutacion. Los casos que traen matriz
    // explicita y no modelo declarado no se pueden correr -- GENis no recibe
    // matrices -- y salen NoRepresentable por la via normal.
    val aCorrer = casos
    println(s"Corriendo ${aCorrer.size} casos " +
      s"(${casos.count(_.modelo.nonEmpty)} con modelo de mutacion declarado)")

    // Se corre una sola vez por caso y el (Caso, Resultado) se conserva: lo
    // necesitan tanto filaDe como Contraste.evaluar,
    // y correr Corredor.correr dos veces por caso solo para separar esas
    // responsabilidades seria desperdiciar el motor bayesiano por estetica.
    // Se pasa el piso que declara el caso en casos.csv, o el piso inerte si
    // no declara ninguno. Esta linea es la que hace que la logica del piso
    // de GENis se ejecute: `Corredor.correr(c)` a secas toma siempre el
    // default inerte. El paquete entrega
    // el panel CRUDO en los casos que declaran piso, asi que declararselo a
    // GENis aca no lo cuenta dos veces -- lo cuenta la unica vez.
    val corridos: List[(Caso, Resultado)] = aCorrer.map { c =>
      val r = Corredor.correr(c, c.piso.getOrElse(MapeoGenis.PISO_INERTE))
      println(f"  ${c.id}%-9s ${resumen(r)}")
      c -> r
    }

    val filas = corridos.map { case (c, r) => filaDe(c, r, version) }

    val salida = new File(dirSalida, "resultados_genis.csv")
    val escritor = CSVWriter.open(salida, "UTF-8")
    try {
      escritor.writeRow(CABECERA)
      filas.foreach(escritor.writeRow)
    } finally escritor.close()

    // Se devuelve una copia del manifiesto contra el que se corrio.
    java.nio.file.Files.copy(
      new File(dirPaquete, "MANIFIESTO.txt").toPath,
      new File(dirSalida, "MANIFIESTO.txt").toPath,
      java.nio.file.StandardCopyOption.REPLACE_EXISTING)

    // Constancia LOCAL de contra que paquete se corrio. NO viaja en la entrega
    // -- los seis archivos que se entregan se copian por nombre -- y por eso no
    // toca el formato de la entrega ni el manifiesto que lrval verifica.
    //
    // Para que existe: el paquete de casos vive del lado de lrval y se pasa por
    // argumento, asi que la corrida puede no haber usado validation/cases.
    // Quien contraste el archivo entregado contra "el catalogo" tiene
    // que leer el catalogo que la corrida uso de verdad; si asume una ruta
    // fija, compara la entrega contra un paquete distinto y el desacuerdo
    // aparece como una diferencia de conteo sin explicacion.
    val constancia = new java.io.PrintWriter(
      new File(dirSalida, "PAQUETE-USADO.txt"), "UTF-8")
    try constancia.print(dirPaquete.getAbsolutePath + "\n")
    finally constancia.close()

    println(s"\nEscrito: ${salida.getAbsolutePath} (${filas.size} filas)")

    // Informe de contraste contra LR_referencia, fuera del CSV de entrega:
    // el arnes no usa esa columna para calcular, pero informa la diferencia
    // por su cuenta, sin depender del clasificador del modulo de referencia.
    // Solo tiene sentido para los casos efectivamente
    // Calculado: no_soportado/no_representable/error no tienen LR con el
    // que contrastar.
    val filasContraste = corridos.collect { case (c, Calculado(v, _)) => Contraste.evaluar(c, v) }
    val contrasteTxt = new File(dirSalida, "contraste.txt")
    val escritorContraste = new java.io.PrintWriter(contrasteTxt, "UTF-8")
    try escritorContraste.print(Contraste.informe(filasContraste)) finally escritorContraste.close()
    println(s"Escrito: ${contrasteTxt.getAbsolutePath} (${filasContraste.size} casos calculados)")


    // El volcado de matrices va DESPUES de la entrega de casos y no aborta la
    // corrida: son dos mediciones independientes. Una grilla mal
    // formada no puede llevarse puesta una entrega de casos que ya se
    // calculo bien. Se informa, se sigue, y la corrida termina en 1.
    //
    // De ahi el orden de lo que viene: primero se CALCULA la grilla (sin
    // escribir ni salir), despues se ESCRIBE todo lo que haya -- las matrices
    // de los casos, que ya estaban calculadas desde antes de mirar la grilla,
    // y las de la grilla si salieron -- y recien al final se sale con codigo
    // distinto de cero si corresponde. Nada que ya este calculado se pierde
    // por una falla posterior.
    val hayGrilla = new File(dirPaquete, "grilla.csv").exists()
    val hayPaneles = new File(dirPaquete, "paneles.csv").exists()
    val (deGrilla, codigoDeSalida) =
      if (hayGrilla && hayPaneles) {
        try { (grillaVolcada(dirPaquete), 0) }
        catch {
          case ex: Throwable =>
            println(s"El volcado de matrices fallo: ${ex.getMessage}")
            println("La entrega de casos de arriba NO se ve afectada.")
            (List.empty[MatrizVolcada], 1)
        }
      } else if (hayGrilla != hayPaneles) {
        println("El paquete trae grilla.csv o paneles.csv pero no los dos; " +
          "se omite el volcado de matrices")
        (List.empty[MatrizVolcada], 1)
      } else {
        println("El paquete no trae grilla.csv; se omite el volcado de matrices")
        (List.empty[MatrizVolcada], 0)
      }

    // Las matrices de los casos van al MISMO par de archivos que las de la
    // grilla, distinguidas por la columna `origen`: se escriben las dos
    // procedencias juntas, de una sola vez.
    val matricesDeCasos = corridos.collect { case (_, Calculado(_, ms)) => ms }.flatten
    val matrices = deGrilla ++ matricesDeCasos
    if (matrices.nonEmpty) {
      SalidaMatrices.escribir(dirSalida, matrices)
      println(s"Escritas ${matrices.size} matrices " +
        s"(${deGrilla.size} de grilla, ${matricesDeCasos.size} de casos)")
    }

    // Ultimo, cuando ya no queda nada por escribir.
    if (codigoDeSalida != 0) sys.exit(codigoDeSalida)
  }
}
