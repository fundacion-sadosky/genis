package validation

import java.io.File

import com.github.tototoshi.csv.CSVReader

object Paquete {

  private def filas(dir: File, archivo: String): List[Map[String, String]] = {
    val lector = CSVReader.open(new File(dir, archivo), "UTF-8")
    try lector.allWithHeaders() finally lector.close()
  }

  // Un campo vacio se escribe como cadena vacia, nunca como NA.
  private def opt(f: Map[String, String], col: String): Option[String] =
    f.get(col).map(_.trim).filter(_.nonEmpty)

  private def req(f: Map[String, String], col: String): String =
    opt(f, col).getOrElse(
      throw new IllegalArgumentException(s"columna '$col' vacia o ausente en $f"))

  // Claves repetidas dentro de las filas de un mismo caso. Hay que
  // detectarlas ANTES de construir el Map de
  // frecuencias.csv, porque una fila repetida se pisa silenciosamente al
  // armar el Map y la informacion se pierde antes de que verificarInvariantes
  // la pueda mirar. mutacion.csv tiene el mismo riesgo aunque su resultado
  // sea una List, no un Map: dos celdas repetidas con el mismo valor pueden
  // dejar la fila sumando 1 igual, escondiendo que falta otra celda distinta.
  private def clavesRepetidas[A](filas: List[A])(clave: A => String): List[String] =
    filas.groupBy(clave).collect { case (k, vs) if vs.size > 1 => k }.toList.sorted

  def leer(dir: File): List[Caso] = {
    val pedigries = filas(dir, "pedigries.csv").groupBy(f => req(f, "caso"))
    val perfiles = filas(dir, "perfiles.csv").groupBy(f => req(f, "caso"))
    val frecuencias = filas(dir, "frecuencias.csv").groupBy(f => req(f, "caso"))
    // mutacion.csv es opcional: GENis no recibe matrices de mutacion como
    // entrada, asi que el paquete puede no traerla. Si la trae, se lee igual.
    val mutacion =
      if (new File(dir, "mutacion.csv").exists())
        filas(dir, "mutacion.csv").groupBy(f => req(f, "caso"))
      else Map.empty[String, List[Map[String, String]]]
    val modelos = filas(dir, "modelos.csv").groupBy(f => req(f, "caso"))

    filas(dir, "casos.csv").map { f =>
      val id = req(f, "caso")

      def hipotesis(h: String): Map[String, IndividuoPed] =
        pedigries.getOrElse(id, Nil)
          .filter(p => req(p, "hipotesis") == h)
          .map { p =>
            val alias = req(p, "individuo")
            alias -> IndividuoPed(alias, opt(p, "padre"), opt(p, "madre"), req(p, "sexo"))
          }.toMap

      val filasFrecuencias = frecuencias.getOrElse(id, Nil)
      val filasMatriz = mutacion.getOrElse(id, Nil)

      val duplicadosFrecuencias =
        clavesRepetidas(filasFrecuencias)(p => s"${req(p, "locus")}/${req(p, "alelo")}")
          .map(clave => s"$id/$clave: el alelo aparece mas de una vez en frecuencias.csv")

      val duplicadosMatriz =
        clavesRepetidas(filasMatriz)(p =>
          s"${req(p, "locus")}/${req(p, "sexo_progenitor")}/" +
            s"${req(p, "alelo_origen")}->${req(p, "alelo_destino")}")
          .map(clave => s"$id/$clave: la celda aparece mas de una vez en mutacion.csv")

      Caso(
        id = id,
        grupo = req(f, "grupo"),
        theta = opt(f, "theta").map(_.toDouble).getOrElse(0.0),
        h1 = hipotesis("H1"),
        h2 = hipotesis("H2"),
        perfiles = perfiles.getOrElse(id, Nil).map { p =>
          Genotipo(req(p, "individuo"), req(p, "locus"), opt(p, "alelo_1"), opt(p, "alelo_2"))
        },
        frecuencias = filasFrecuencias
          .groupBy(p => req(p, "locus"))
          .map { case (locus, fs) =>
            locus -> fs.map(p => req(p, "alelo") -> req(p, "frecuencia").toDouble).toMap
          },
        matriz = filasMatriz.map { p =>
          CeldaMutacion(req(p, "locus"), req(p, "sexo_progenitor"),
            req(p, "alelo_origen"), req(p, "alelo_destino"), req(p, "probabilidad").toDouble)
        },
        modelo = modelos.getOrElse(id, Nil).map { p =>
          ModeloDeclarado(req(p, "locus"), req(p, "sexo_progenitor"), req(p, "modelo"),
            req(p, "tasa").toDouble,
            opt(p, "tasa2").map(_.toDouble),
            opt(p, "rango").map(_.toDouble),
            req(p, "alelos").split("\\s+").toList,
            opt(p, "pasos").map(_.toInt))
        },
        duplicados = duplicadosFrecuencias ++ duplicadosMatriz,
        // Solo para informar; ver el comentario del campo en Caso.scala.
        lrReferencia = opt(f, "LR_referencia").map(_.toDouble),
        lrOtraConvencion = opt(f, "LR_otra_convencion").map(_.toDouble),
        // Columna vacia -> None, que es "no declara piso". NO se
        // convierte a 0.0: la ausencia de piso y un piso de 0 tienen que
        // quedar distinguibles, porque la ausencia es justo lo que mide
        // FRQ-02S. La lectura es por nombre de columna, no por indice.
        piso = opt(f, "piso").map(_.toDouble)
      )
    }
  }

  // No-private a proposito: PaqueteSintetico (bloque masiva) la reusa para
  // FIRMAR el manifiesto del paquete sintetico que genera, en vez de
  // reimplementar el mismo hash. RunnerMasiva verifica ese MANIFIESTO.txt
  // antes de correr.
  def sha256(f: File): String = {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val buf = new Array[Byte](8192)
    val in = new java.io.FileInputStream(f)
    try {
      var n = in.read(buf)
      while (n > 0) { md.update(buf, 0, n); n = in.read(buf) }
    } finally in.close()
    md.digest().map(b => "%02x".format(b & 0xff)).mkString
  }

  // MANIFIESTO.txt: formato sha256sum ("<hash>  <archivo>"), con lineas de
  // comentario que empiezan con '#'. Solo cubre los archivos regulares del
  // nivel superior; fam/ queda fuera a proposito.
  def verificarManifiesto(dir: File): List[String] = {
    val manifiesto = new File(dir, "MANIFIESTO.txt")
    if (!manifiesto.exists()) return List("MANIFIESTO.txt: no existe en el paquete")

    val fuente = scala.io.Source.fromFile(manifiesto, "UTF-8")
    val lineas = try fuente.getLines().toList finally fuente.close()

    val entradas = lineas
      .map(_.trim)
      .filter(l => l.nonEmpty && !l.startsWith("#"))
      .flatMap { l =>
        l.split("\\s+", 2) match {
          case Array(hash, nombre) => Some((nombre.trim, hash))
          case _ => None
        }
      }

    if (entradas.isEmpty) List("MANIFIESTO.txt: no tiene entradas legibles")
    else entradas.flatMap { case (nombre, esperado) =>
      val f = new File(dir, nombre)
      if (!f.exists()) Some(s"$nombre: listado en el manifiesto pero ausente del paquete")
      else {
        val obtenido = sha256(f)
        if (obtenido == esperado) None
        else Some(s"$nombre: SHA-256 no coincide (esperado $esperado, obtenido $obtenido)")
      }
    }
  }

  // Tolerancia de los invariantes de suma. El paquete de referencia declara
  // desvios maximos de 5.6e-16 (frecuencias) y 4.4e-16 (matrices); 1e-12 deja
  // margen sobrado sin dejar pasar un error real de construccion.
  private val TOLERANCIA = 1e-12

  def verificarInvariantes(casos: List[Caso]): List[String] = casos.flatMap { c =>

    val frecuenciasSuman = c.frecuencias.toList.flatMap { case (locus, panel) =>
      val suma = panel.values.sum
      if (math.abs(suma - 1.0) > TOLERANCIA)
        Some(s"${c.id}/$locus: las frecuencias no suman 1 (suman $suma)")
      else None
    }

    val frecuenciasEnRango = c.frecuencias.toList.flatMap { case (locus, panel) =>
      panel.collect { case (alelo, f) if f <= 0.0 || f > 1.0 =>
        s"${c.id}/$locus/$alelo: frecuencia $f fuera de (0, 1]"
      }
    }

    val filasSuman = c.matriz
      .groupBy(m => (m.locus, m.sexoProgenitor, m.aleloOrigen))
      .toList
      .flatMap { case ((locus, sexo, origen), celdas) =>
        val suma = celdas.map(_.probabilidad).sum
        if (math.abs(suma - 1.0) > TOLERANCIA)
          Some(s"${c.id}/$locus/$sexo/$origen: la fila de la matriz no suma 1 (suma $suma)")
        else None
      }

    // El alfabeto de la matriz tiene que ser el del panel de ese (caso, locus).
    val alfabetoCoincide = c.matriz.groupBy(_.locus).toList.flatMap { case (locus, celdas) =>
      val enMatriz = celdas.flatMap(m => List(m.aleloOrigen, m.aleloDestino)).toSet
      val enPanel = c.frecuencias.getOrElse(locus, Map.empty).keySet
      val sobran = enMatriz -- enPanel
      if (sobran.nonEmpty)
        Some(s"${c.id}/$locus: la matriz usa alelos que no estan en el panel: ${sobran.toList.sorted.mkString(", ")}")
      else None
    }

    // Un caso esta en mutacion.csv o en modelos.csv, nunca en los dos.
    val exclusion =
      if (c.matriz.nonEmpty && c.modelo.nonEmpty)
        List(s"${c.id}: aparece en mutacion.csv y en modelos.csv a la vez")
      else Nil

    // Todo individuo con genotipo tiene que aparecer en H1 y en H2.
    val tipificadosPresentes = c.tipificados.toList.sorted.flatMap { t =>
      if (!c.h1.contains(t)) Some(s"${c.id}: $t tiene genotipo pero no esta en H1")
      else if (!c.h2.contains(t)) Some(s"${c.id}: $t tiene genotipo pero no esta en H2")
      else None
    }

    // Integridad referencial de pedigries.csv: un alias
    // mencionado como padre o como madre tiene que existir como individuo de
    // esa misma hipotesis del mismo caso. DeteccionPI.detectar hace acceso
    // directo tipo mapa(alias) asumiendo que la referencia existe; si no,
    // mejor decirlo en la lectura que reventar con una excepcion cruda tres
    // capas mas arriba.
    def referenciasRotas(nombreHip: String, hip: Map[String, IndividuoPed]): List[String] =
      hip.values.toList.sortBy(_.alias).flatMap { i =>
        val padreRoto = i.padre.filter(a => !hip.contains(a)).map(a =>
          s"${c.id}/$nombreHip: ${i.alias} tiene padre '$a' que no existe en $nombreHip")
        val madreRota = i.madre.filter(a => !hip.contains(a)).map(a =>
          s"${c.id}/$nombreHip: ${i.alias} tiene madre '$a' que no existe en $nombreHip")
        padreRoto.toList ++ madreRota.toList
      }

    val pedigriesIntegros = referenciasRotas("H1", c.h1) ++ referenciasRotas("H2", c.h2)

    // Un alelo observado que el panel entregado no trae solo tiene frecuencia
    // si el caso declara piso: entra valiendo el piso y el panel se renormaliza.
    // Sin piso el caso no esta definido, y el arnes lo correria con el
    // piso inerte: un LR enorme y creible, sin ningun error que lo delate.
    val aleloFueraSinPiso =
      if (c.piso.isDefined) Nil
      else c.perfiles.flatMap { g =>
        val panel = c.frecuencias.getOrElse(g.locus, Map.empty)
        g.alelos.filterNot(panel.contains).map(a =>
          s"${c.id}/${g.locus}: el alelo $a de ${g.individuo} no esta en el panel y el caso no declara piso")
      }.distinct

    frecuenciasSuman ++ frecuenciasEnRango ++ filasSuman ++ alfabetoCoincide ++
      exclusion ++ tipificadosPresentes ++ pedigriesIntegros ++ aleloFueraSinPiso ++ c.duplicados
  }
}
