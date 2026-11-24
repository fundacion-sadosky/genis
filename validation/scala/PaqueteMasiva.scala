package validation

import java.io.File

import com.github.tototoshi.csv.CSVReader

// Lee el paquete de busqueda masiva y verifica sus invariantes. Falla ruidoso:
// adivinar produciria un millon de numeros sobre una entrada mal entendida.
object PaqueteMasiva {

  private def filas(dir: File, archivo: String): List[Map[String, String]] = {
    val lector = CSVReader.open(new File(dir, archivo), "UTF-8")
    try lector.allWithHeaders() finally lector.close()
  }

  private def opt(f: Map[String, String], col: String): Option[String] =
    f.get(col).map(_.trim).filter(_.nonEmpty)

  private def req(f: Map[String, String], col: String): String =
    opt(f, col).getOrElse(
      throw new IllegalArgumentException(s"columna '$col' vacia o ausente en $f"))

  private def clavesRepetidas[A](fs: List[A])(clave: A => String): List[String] =
    fs.groupBy(clave).collect { case (k, vs) if vs.size > 1 => k }.toList.sorted

  private def exigir(cond: Boolean, mensaje: => String): Unit =
    if (!cond) throw new IllegalArgumentException(mensaje)

  def leer(dir: File): PaqueteMasivo = {
    val filasTipos = filas(dir, "tipos.csv")
    val filasPed = filas(dir, "pedigries.csv")
    val filasFam = filas(dir, "familias.csv")
    val filasPerf = filas(dir, "perfiles.csv")
    val filasPoi = filas(dir, "poi.csv")
    val filasFrec = filas(dir, "frecuencias.csv")

    exigir(clavesRepetidas(filasTipos)(p => req(p, "tipo")).isEmpty,
      s"tipos.csv: el tipo aparece mas de una vez: " +
        clavesRepetidas(filasTipos)(p => req(p, "tipo")).mkString(", "))
    exigir(clavesRepetidas(filasPed)(p => s"${req(p, "tipo")}/${req(p, "individuo")}").isEmpty,
      "pedigries.csv: hay un (tipo, individuo) repetido")
    exigir(clavesRepetidas(filasFam)(p => req(p, "familia")).isEmpty,
      "familias.csv: hay una familia repetida")
    exigir(clavesRepetidas(filasFrec)(p => s"${req(p, "locus")}/${req(p, "alelo")}").isEmpty,
      s"frecuencias.csv: el alelo aparece mas de una vez en " +
        clavesRepetidas(filasFrec)(p => s"${req(p, "locus")}/${req(p, "alelo")}").mkString(", "))
    exigir(clavesRepetidas(filasPerf)(p =>
      s"${req(p, "familia")}/${req(p, "individuo")}/${req(p, "locus")}").isEmpty,
      "perfiles.csv: hay un (familia, individuo, locus) repetido")
    exigir(clavesRepetidas(filasPoi)(p => s"${req(p, "poi")}/${req(p, "locus")}").isEmpty,
      "poi.csv: hay un (poi, locus) repetido")

    val porTipo = filasPed.groupBy(p => req(p, "tipo"))
    val tipos = filasTipos.map { f =>
      val id = req(f, "tipo")
      val individuos = porTipo.getOrElse(id, Nil).map { p =>
        val rol = req(p, "rol")
        exigir(Rol.validos.contains(rol),
          s"$id: rol desconocido '$rol' (validos: ${Rol.validos.toList.sorted.mkString(", ")})")
        val sexo = req(p, "sexo")
        exigir(sexo == "M" || sexo == "F", s"$id: sexo no representable '$sexo'")
        IndividuoMasiva(req(p, "individuo"), opt(p, "padre"), opt(p, "madre"), sexo, rol)
      }
      val bajoPrueba = individuos.filter(_.rol == Rol.BajoPrueba)
      exigir(bajoPrueba.size == 1,
        s"$id: el tipo declara ${bajoPrueba.size} individuos con rol bajo_prueba; tiene que " +
          "declarar exactamente uno")
      val aliases = individuos.map(_.alias).toSet
      val colgados = individuos.flatMap(i => List(i.padre, i.madre).flatten).filterNot(aliases)
      exigir(colgados.isEmpty,
        s"$id: referencia de padre/madre a un individuo que no existe: " +
          colgados.distinct.sorted.mkString(", "))
      id -> TipoPed(id, req(f, "etiqueta"), req(f, "grado"), individuos)
    }.toMap

    val perfilesPorFamilia = filasPerf.groupBy(p => req(p, "familia"))
    val familias = filasFam.map { f =>
      val id = req(f, "familia")
      val tipo = req(f, "tipo")
      exigir(tipos.contains(tipo), s"$id: apunta al tipo '$tipo', que tipos.csv no declara")
      val t = tipos(tipo)
      val gs = perfilesPorFamilia.getOrElse(id, Nil).map { p =>
        val ind = req(p, "individuo")
        exigir(ind != t.bajoPrueba,
          s"$id: perfiles.csv trae un genotipo del individuo bajo prueba ($ind); el individuo " +
            "bajo prueba nunca lleva perfil")
        exigir(t.tipificados.contains(ind),
          s"$id: perfiles.csv trae un genotipo de '$ind', que en el tipo $tipo no es tipificado")
        Genotipo(ind, req(p, "locus"), opt(p, "alelo_1"), opt(p, "alelo_2"))
      }
      FamiliaMasiva(id, tipo, gs)
    }

    val poi = filasPoi.groupBy(p => req(p, "poi")).toList.sortBy(_._1).map { case (id, ps) =>
      Poi(id, ps.map(p => Genotipo(id, req(p, "locus"), opt(p, "alelo_1"), opt(p, "alelo_2"))))
    }

    val frecuencias = filasFrec
      .groupBy(p => req(p, "locus"))
      .map { case (locus, ps) =>
        locus -> ps.map(p => req(p, "alelo") -> req(p, "frecuencia").toDouble).toMap
      }

    // modelos.csv solo existe en la condicion con mutacion.
    val modelo =
      if (!new File(dir, "modelos.csv").exists()) Nil
      else {
        val fs = filas(dir, "modelos.csv")
        exigir(clavesRepetidas(fs)(p =>
          s"${req(p, "locus")}/${req(p, "sexo_progenitor")}").isEmpty,
          "modelos.csv: hay un (locus, sexo_progenitor) repetido")
        fs.map { p =>
          ModeloDeclarado(
            locus = req(p, "locus"),
            sexoProgenitor = req(p, "sexo_progenitor"),
            modelo = req(p, "modelo"),
            tasa = req(p, "tasa").toDouble,
            tasa2 = opt(p, "tasa2").map(_.toDouble),
            rango = opt(p, "rango").map(_.toDouble),
            alelos = req(p, "alelos").split("\\s+").toList,
            pasos = opt(p, "pasos").map(_.toInt))
        }
      }

    // modelos.csv, cuando existe, tiene que cubrir EXACTAMENTE el panel x
    // {M,F}: solo se declaran los sexos M y F, nunca I mezclado con sexos
    // concretos. Un locus sin parametros de mutacion no se calcularia con el
    // modelo declarado; ese locus, o uno que sobra, queda detectado aca,
    // antes de que
    // ModeloMutacionGenis.desdeDeclarados o RunnerMasiva lo vean.
    if (modelo.nonEmpty) {
      val panel = frecuencias.keySet
      val esperado = panel.flatMap(locus => Set(locus -> "M", locus -> "F"))
      val declarado = modelo.map(m => (m.locus, m.sexoProgenitor)).toSet
      val faltan = (esperado -- declarado).toList.sorted
      val sobran = (declarado -- esperado).toList.sorted
      def listar(pares: List[(String, String)]): String =
        pares.map { case (locus, sexo) => s"$locus/$sexo" }.mkString(", ")
      exigir(faltan.isEmpty && sobran.isEmpty,
        "modelos.csv no cubre exactamente el panel x {M,F}" +
          (if (faltan.nonEmpty) s"; falta ${listar(faltan)}" else "") +
          (if (sobran.nonEmpty) s"; sobra ${listar(sobran)}" else ""))
    }

    val manifiesto = {
      val f = new File(dir, "MANIFIESTO.txt")
      if (f.exists()) {
        val fuente = scala.io.Source.fromFile(f, "UTF-8")
        try fuente.mkString finally fuente.close()
      } else ""
    }

    PaqueteMasivo(tipos, familias, poi, frecuencias, modelo, manifiesto)
  }
}
