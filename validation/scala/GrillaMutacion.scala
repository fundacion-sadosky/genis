package validation

import java.io.File

import com.github.tototoshi.csv.CSVReader

// Lectura de la grilla de matrices que pide lrval. Es independiente del
// catalogo de casos: una fila de grilla no tiene pedigri ni genotipos, solo
// una parametrizacion y dos dominios de alelos.

case class Panel(
  id: String,
  alelos: List[String],              // en el orden declarado, con su grafia
  frecuencias: Map[String, Double]
)

case class FilaGrilla(
  id: String,
  panelCalibracion: String,          // dominio sobre el que se calibra el ki
  panelUniverso: String,             // dominio que define filas y columnas
  modelo: String,                    // "equal" | "stepwise" | "stepwise_extendido"
  tasa: Double,
  tasa2: Double,
  rango: Option[Double],
  pasos: Option[Int],
  sexo: String,                      // "M" | "F" | "I"
  kiRedondeado: Boolean,
  kiRedondeadoTexto: String,         // grafia cruda de la columna, para verificarInvariantes
  nota: String
)

object GrillaMutacion {

  private def filas(dir: File, archivo: String): List[Map[String, String]] = {
    val lector = CSVReader.open(new File(dir, archivo), "UTF-8")
    try lector.allWithHeaders() finally lector.close()
  }

  private def opt(f: Map[String, String], col: String): Option[String] =
    f.get(col).map(_.trim).filter(_.nonEmpty)

  private def req(f: Map[String, String], col: String): String =
    opt(f, col).getOrElse(
      throw new IllegalArgumentException(s"columna obligatoria vacia o ausente: $col"))

  def leer(dir: File): (Map[String, Panel], List[FilaGrilla]) = {
    val porPanel = filas(dir, "paneles.csv").groupBy(f => req(f, "panel"))
    val paneles = porPanel.map { case (id, fs) =>
      id -> Panel(
        id = id,
        alelos = fs.map(f => req(f, "alelo")),
        frecuencias = fs.map(f => req(f, "alelo") -> req(f, "frecuencia").toDouble).toMap)
    }

    val grilla = filas(dir, "grilla.csv").map { f =>
      val kiRedondeadoTexto = req(f, "ki_redondeado")
      FilaGrilla(
        id = req(f, "id"),
        panelCalibracion = req(f, "panel_calibracion"),
        panelUniverso = req(f, "panel_universo"),
        modelo = req(f, "modelo"),
        tasa = req(f, "tasa").toDouble,
        tasa2 = opt(f, "tasa2").map(_.toDouble).getOrElse(0.0),
        rango = opt(f, "rango").map(_.toDouble),
        pasos = opt(f, "pasos").map(_.toInt),
        sexo = req(f, "sexo"),
        // "si" es verdadero y CUALQUIER OTRA COSA es "no", en silencio. Eso
        // esta bien para el calculo (una grafia rara tiene que decidirse
        // hacia alguno de los dos lados), pero no para la validacion: la
        // grafia cruda se conserva en kiRedondeadoTexto para que
        // verificarInvariantes pueda denunciar una columna mal escrita en
        // vez de dejarla colapsar a "no" sin ningun aviso.
        kiRedondeado = kiRedondeadoTexto == "si",
        kiRedondeadoTexto = kiRedondeadoTexto,
        nota = opt(f, "nota").getOrElse(""))
    }

    (paneles, grilla)
  }

  // Todo lo que no se pueda verificar aca se descubre como un numero raro en
  // el informe de lrval, que es tarde. Mismo criterio que
  // Paquete.verificarInvariantes.
  def verificarInvariantes(
    paneles: Map[String, Panel],
    grilla: List[FilaGrilla]
  ): List[String] = {
    val repetidos = grilla.groupBy(_.id).collect {
      case (id, fs) if fs.size > 1 => s"$id: identificador de fila repetido (${fs.size} veces)"
    }.toList

    val panelesFaltantes = grilla.flatMap { f =>
      List(f.panelCalibracion, f.panelUniverso)
        .filterNot(paneles.contains)
        .map(p => s"${f.id}: nombra el panel $p, que no esta en paneles.csv")
    }

    val parametros = grilla.flatMap { f =>
      if (f.modelo == "stepwise" || f.modelo == "stepwise_extendido") {
        List(
          if (f.rango.isEmpty) Some(s"${f.id}: modelo ${f.modelo} sin rango") else None,
          if (f.pasos.isEmpty) Some(s"${f.id}: modelo ${f.modelo} sin pasos") else None
        ).flatten
      } else Nil
    }

    val sexos = grilla
      .filterNot(f => Set("M", "F", "I").contains(f.sexo))
      .map(f => s"${f.id}: sexo '${f.sexo}' no es M, F ni I")

    // 1. `modelo` contra las claves de ModeloMutacionGenis.TIPO. Sin esto un
    // "Stepwise" con mayuscula (o cualquier grafia que no sea exactamente
    // "equal"/"stepwise"/"stepwise_extendido") saltea en silencio el chequeo
    // de rango y pasos de arriba -- `parametros` solo mira las dos grafias
    // exactas -- para reventar mas tarde adentro de ModeloMutacionGenis.datos
    // (o de MapeoGenis) sin decir de que fila de la grilla se trataba.
    val modelosInvalidos = grilla
      .filterNot(f => ModeloMutacionGenis.TIPO.contains(f.modelo))
      .map(f => s"${f.id}: modelo '${f.modelo}' no es ninguno de " +
        ModeloMutacionGenis.TIPO.keys.toList.sorted.mkString(", "))

    // 2. `ki_redondeado` en {si, no}. Se lee como == "si" (GrillaMutacion.leer)
    // y CUALQUIER OTRA grafia significa "no" calladamente: el par de filas
    // exacta/redondeada (ej. GRL-001/GRL-002) colapsaria en dos matrices
    // identicas si una de las dos tuviera un typo en esta columna, y la
    // referencia informaria dos veces lo mismo: la medicion de la fila
    // redondeada desapareceria sin un solo error.
    val kiRedondeadoInvalido = grilla
      .filterNot(f => Set("si", "no").contains(f.kiRedondeadoTexto))
      .map(f => s"${f.id}: ki_redondeado '${f.kiRedondeadoTexto}' no es 'si' ni 'no'")

    // 3. Alelos repetidos dentro de un panel. Inflan el tamano del universo
    // (que el modelo Equal divide como n-1) y duplican celdas en la matriz
    // volcada, ademas de que `frecuencias` (un Map) se queda calladamente con
    // la ultima fila y descarta la anterior.
    val alelosRepetidos = paneles.toList.flatMap { case (id, panel) =>
      panel.alelos.groupBy(identity).collect {
        case (alelo, ocurrencias) if ocurrencias.size > 1 =>
          s"panel $id: alelo '$alelo' repetido (${ocurrencias.size} veces)"
      }
    }

    repetidos ++ panelesFaltantes ++ parametros ++ sexos ++
      modelosInvalidos ++ kiRedondeadoInvalido ++ alelosRepetidos
  }
}
