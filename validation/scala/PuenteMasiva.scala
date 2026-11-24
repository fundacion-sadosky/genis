package validation

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import pedigree.BayesianNetwork

case class Muestra(
  familia: String, poi: String, tipo: String, decil: Int, log10Busqueda: Double)

case class CeldaPuente(
  familia: String, poi: String, tipo: String, decil: Int,
  log10Busqueda: Double, log10Escenario: Double, diferencia: Double, estado: String)

// El puente entre los dos caminos de calculo de GENis: el de busqueda, por el
// que corre el escenario, y el de escenario (calculateProbability), que es el
// que recorren los casos del catalogo del bloque 1.
//
// La tolerancia se INFORMA, no se impone: el bloque 2 no emite veredictos por
// LR individual. Lo que se publica es el maximo y los percentiles de la
// diferencia.
object PuenteMasiva {

  // Decil reservado para el estrato de exclusiones (ver muestrearExcluidas):
  // no es un decil real de la distribucion de log10LR finitos -- es una
  // marca para que quien lea puente_masiva.csv distinga esa fila de las de
  // los deciles 1-10, sin agregar una columna nueva al CSV.
  val DECIL_EXCLUSION: Int = 0

  // Estratifica por tipo de pedigri y por decil del log10LR QUE LA PROPIA
  // CORRIDA OBSERVO. Estratificar por el resultado propio no usa la
  // referencia, asi que la corrida sigue siendo ciega, y mete celdas de
  // la cola -- donde vive el comportamiento interesante -- en vez de solo del
  // bulto, que es lo que daria un muestreo uniforme sobre un millon de
  // comparaciones casi todas no emparentadas.
  //
  // Ignora las celdas con log10LR infinito A PROPOSITO: la exclusion total
  // (-Inf) no tiene un "decil" que la distinga de otra exclusion -- todas
  // caen en el mismo valor -- y se muestrea aparte, con su propio estrato
  // categorico (ver muestrearExcluidas). Mezclar las dos cosas aca
  // esconderia justamente la cola mas extrema del resultado.
  def muestrear(
    filas: List[FilaMasiva], tipoDe: Map[String, String], k: Int, semilla: Long
  ): List[Muestra] = {
    val utiles = filas.filter(f => f.estado == "ok" && f.log10LR.exists(v => !v.isInfinite))
    utiles.groupBy(f => tipoDe.getOrElse(f.familia, "")).toList.sortBy(_._1).flatMap {
      case (tipo, fs) =>
        val ordenadas = fs.sortBy(_.log10LR.get)
        val n = ordenadas.size
        (0 until 10).toList.flatMap { d =>
          val desde = (n.toLong * d / 10).toInt
          val hasta = (n.toLong * (d + 1) / 10).toInt
          val bloque = ordenadas.slice(desde, hasta)
          if (bloque.isEmpty) Nil
          else {
            val r = new scala.util.Random(semilla + d + tipo.hashCode)
            r.shuffle(bloque).take(k).map(f =>
              Muestra(f.familia, f.poi, tipo, d + 1, f.log10LR.get))
          }
        }
    }
  }

  // El estrato de exclusiones. La exclusion
  // mendeliana total es el resultado mas consecuente en una causa judicial,
  // y `muestrear` la saca del universo muestreable a proposito (no tiene
  // decil). Sin este estrato el puente no podria detectar que los dos
  // caminos discrepen sobre si una celda es exclusion -- una diferencia
  // mayor que cualquier diferencia numerica entre dos LR finitos.
  //
  // El offset de semilla (1000) no pisa ninguno de los 0-9 que usa
  // `muestrear` para sus deciles: las dos poblaciones se muestrean con
  // barajados independientes aunque compartan la semilla base.
  def muestrearExcluidas(
    filas: List[FilaMasiva], tipoDe: Map[String, String], k: Int, semilla: Long
  ): List[Muestra] = {
    val excluidas = filas.filter(f => f.estado == "ok" && f.log10LR.contains(Double.NegativeInfinity))
    excluidas.groupBy(f => tipoDe.getOrElse(f.familia, "")).toList.sortBy(_._1).flatMap {
      case (tipo, fs) =>
        if (fs.isEmpty) Nil
        else {
          val r = new scala.util.Random(semilla + 1000 + tipo.hashCode)
          r.shuffle(fs).take(k).map(f =>
            Muestra(f.familia, f.poi, tipo, DECIL_EXCLUSION, Double.NegativeInfinity))
        }
    }
  }

  // Un tipo entero puede quedar sin ninguna celda muestreada (ni de decil
  // ni de exclusion) sin que nada lo note. Esta funcion cruza los tipos
  // declarados contra los muestreados; quien orquesta (RunnerMasiva.correr)
  // lo declara en condiciones_masiva.txt en vez de dejar que se pierda en
  // silencio.
  def tiposSinMuestra(tipoDe: Map[String, String], muestras: List[Muestra]): List[String] = {
    val declarados = tipoDe.values.toSet
    val muestreados = muestras.map(_.tipo).toSet
    (declarados -- muestreados).toList.sorted
  }

  // Corre cada celda muestreada por calculateProbability, con el POI adosado al
  // nodo bajo prueba. Es el camino del bloque 1: resuelve el pedigri en cada
  // llamada, que es justamente por lo que la muestra es chica.
  def contrastar(
    p: PaqueteMasivo,
    alelos: Map[String, Double],
    frecuencias: Map[String, Map[Double, Double]],
    universo: Map[String, List[Double]],
    modelo: (Option[Long], Option[List[BayesianNetwork.MutationModelData]]),
    muestras: List[Muestra]
  ): List[CeldaPuente] = {
    val familiaDe = p.familias.map(f => f.familia -> f).toMap
    val poiDe = p.poi.map(x => x.poi -> x).toMap
    val (tipoModelo, datosModelo) = modelo

    // Una linea por celda, a stdout. Sin esto `contrastar` es la ultima fase
    // ciega del arnes, y es la mas cara de todas: con mutacion, una celda de
    // un tipo con hermanos cuesta del orden de genotipificar la familia
    // completa, y el puente entero son horas. El bucle es secuencial a
    // proposito (cada celda pide mucha memoria y dos en paralelo no entran),
    // asi que el contador es fiable y no hace falta nada atomico.
    val total = muestras.size
    val t0Lote = System.currentTimeMillis()
    def avisar(m: Muestra, i: Int, t0: Long, estado: String): Unit = {
      val ahora = System.currentTimeMillis()
      val seg = (ahora - t0) / 1000.0
      val minLote = (ahora - t0Lote) / 60000.0
      val hechas = i + 1
      // Extrapolacion por caudal, igual que en el barrido de familias, y con
      // la misma advertencia: las celdas son de costo muy distinto segun el
      // tipo de pedigri, asi que sirve para ver si avanza, no como promesa.
      val minResta = minLote / hechas * (total - hechas)
      println(f"[puente] $hechas%d/$total%d ${m.tipo}%-18s ${m.familia}%-8s ${m.poi}%-9s " +
        f"decil ${m.decil}%2d $seg%7.1f s $estado%s | transcurrido $minLote%6.1f min | " +
        f"resta ~$minResta%6.1f min")
    }
    muestras.zipWithIndex.map { case (m, i) =>
      val t0 = System.currentTimeMillis()
      try {
        val e = MapeoMasiva.familia(p, familiaDe(m.familia), alelos).right.get
        val perfilPoi = MapeoMasiva.perfilDePoi(poiDe(m.poi), alelos, 900000 + i).right.get

        // El POI ocupa el nodo bajo prueba: bajo H1 el POI ES el desaparecido.
        // El nodo SIGUE unknown = true -- asi llega de MapeoMasiva.familia,
        // que ya marca `i.rol == Rol.BajoPrueba` como unknown -- y no se
        // toca. No es un detalle cosmetico: getQueryProfiles
        // (BayesianNetwork) arma la consulta filtrando
        // `genogram.filter(_.unknown)` y despues busca, DENTRO de ese
        // filtro, el Profile cuyo globalCode matchee
        // (`profiles.find(_.globalCode == individual.globalCode.get)`).
        // Es exactamente el patron del bloque 1
        // (MapeoGenis.mapear: el PI queda `unknown = true` Y con su perfil
        // en la lista). Si aca se pisara a `unknown = false`, el filtro no
        // encontraria ningun nodo, `queryProfiles` quedaria vacio y
        // `calculateProbability` (`queryProfiles.values.head`) lanzaria
        // NoSuchElementException en todas las celdas.
        val codigo = perfilPoi.globalCode
        val genograma = e.genograma.map { ind =>
          if (ind.alias.text == e.bajoPrueba) ind.copy(globalCode = Some(codigo))
          else ind
        }
        val perfiles = e.perfiles :+ perfilPoi

        val (lr, _) = BayesianNetwork.calculateProbability(
          profiles = perfiles,
          genogram = genograma,
          frequencyTable = frecuencias,
          analysisType = MapeoGenis.TIPO_DE_ANALISIS,
          linkage = Map.empty,
          verbose = false,
          mutationModelType = tipoModelo,
          mutationModelData = datosModelo,
          seenAlleles = universo,
          locusRangeMap = Map.empty)

        val escenario = if (lr == 0.0) Double.NegativeInfinity else math.log10(lr)
        // -Inf - (-Inf) da NaN en IEEE754: sin este caso especial, una
        // celda del estrato de exclusiones donde los dos caminos SI
        // coinciden (el resultado esperado, y el que interesa detectar)
        // escribiria `diferencia = NaN`, que a simple vista en el CSV se
        // lee como un error y no como el acuerdo categorico que en
        // realidad es.
        val diferencia =
          if (m.log10Busqueda.isNegInfinity && escenario.isNegInfinity) 0.0
          else m.log10Busqueda - escenario
        avisar(m, i, t0, "ok")
        CeldaPuente(m.familia, m.poi, m.tipo, m.decil, m.log10Busqueda, escenario, diferencia, "ok")
      } catch {
        case ex: Throwable =>
          // La celda que falla tambien se informa: en una fase de horas, una
          // que revienta en silencio se descubre recien al abrir el CSV.
          avisar(m, i, t0, ex.getClass.getSimpleName)
          CeldaPuente(m.familia, m.poi, m.tipo, m.decil, m.log10Busqueda, Double.NaN, Double.NaN,
            s"error: ${ex.getClass.getSimpleName}")
      }
    }
  }

  def escribir(destino: File, celdas: List[CeldaPuente]): Unit = {
    Option(destino.getParentFile).foreach(_.mkdirs())
    val w = CSVWriter.open(destino, "UTF-8")(SalidaMasiva.FormatoLF)
    try {
      w.writeRow(List("familia", "poi", "tipo", "decil", "log10LR_busqueda",
        "log10LR_escenario", "diferencia", "estado"))
      celdas.foreach(c => w.writeRow(List(c.familia, c.poi, c.tipo, c.decil.toString,
        SalidaMasiva.formatear(c.log10Busqueda), SalidaMasiva.formatear(c.log10Escenario),
        SalidaMasiva.formatear(c.diferencia), c.estado)))
    } finally w.close()
  }
}
