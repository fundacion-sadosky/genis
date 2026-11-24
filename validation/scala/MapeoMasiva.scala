package validation

import pedigree.{Individual, NodeAlias}
import profile.{Allele, AlleleValue, Profile}
import types.{SampleCode, Sex}

// Lo que consumen getGenotypification y getLR para UNA familia, ya traducido.
case class EntradaFamilia(
  familia: String,
  tipo: String,
  perfiles: Array[Profile],
  genograma: Array[Individual],
  bajoPrueba: String
)

// Traduce el paquete masivo a los tipos de GENis. Reusa de MapeoGenis todo lo
// que no cambia de escala; lo propio es la traduccion de `rol`, que el
// bloque 1 no tiene (ahi el individuo bajo prueba se deduce comparando las
// dos hipotesis, con DeteccionPI).
//
// El panel es UNO para todo el escenario, asi que alelos, frecuencias y
// universo se calculan una sola vez por paquete y no por familia.
object MapeoMasiva {

  // Los codigos que devuelve son unicos DENTRO de una EntradaFamilia (el
  // indice se reinicia en cada llamada a familia()), no entre familias: dos
  // familias del mismo tipo reciben los mismos SampleCode para sus
  // tipificados. Hoy eso no colisiona porque el motor se llama una vez por
  // familia (cada corrida ve un solo genograma y sus perfiles a la vez) y
  // los POI usan el rango disjunto 500000+. Si el orquestador llegara a
  // combinar mas de una familia en una sola llamada al motor, esta funcion
  // habria que revisarla primero.
  private def codigoDe(indice: Int): SampleCode = SampleCode(f"AR-C-MAS-$indice%06d")

  def alelos(p: PaqueteMasivo): Either[String, Map[String, Double]] =
    MapeoGenis.mapearAlelos(
      p.frecuencias.values.flatMap(_.keySet).toSet ++
        p.familias.flatMap(_.perfiles.flatMap(_.alelos)).toSet ++
        p.poi.flatMap(_.genotipos.flatMap(_.alelos)).toSet ++
        p.modelo.flatMap(_.alelos).toSet)

  def frecuencias(
    p: PaqueteMasivo, alelos: Map[String, Double]
  ): Map[String, Map[Double, Double]] =
    p.frecuencias.map { case (locus, panel) =>
      locus -> (panel.map { case (n, f) => alelos(n) -> f } + (-1.0 -> MapeoGenis.PISO_INERTE))
    }

  def universo(p: PaqueteMasivo, alelos: Map[String, Double]): Map[String, List[Double]] =
    p.frecuencias.map { case (locus, panel) => locus -> panel.keys.toList.map(alelos).sorted }

  def familia(
    p: PaqueteMasivo, f: FamiliaMasiva, alelos: Map[String, Double]
  ): Either[String, EntradaFamilia] = {
    val t = p.tipos(f.tipo)

    // Dropout: no hay forma de escribir "se observo una banda, la otra no se
    // leyo", asi que el caso no se puede cargar. GENis no lee un solo alelo
    // como homocigoto: lo excluye del calculo, sin marginalizar. Mismo
    // criterio que el bloque 1.
    val conDropout = f.perfiles.filter(_.bandaUnica)
    if (conDropout.nonEmpty)
      return Left(s"${f.familia}: dropout no representable en GENis " +
        s"(${conDropout.map(g => s"${g.individuo}/${g.locus}").mkString(", ")})")

    val invalidos = t.individuos.map(_.alias).filterNot(_.matches("""^[a-zA-Z0-9\-]{1,15}$"""))
    if (invalidos.nonEmpty)
      return Left(s"${f.familia}: alias no validos para NodeAlias: ${invalidos.sorted.mkString(", ")}")

    val codigos = t.tipificados.zipWithIndex.map { case (a, i) => a -> codigoDe(i + 1) }.toMap

    val perfiles = t.tipificados.map { individuo =>
      val porLocus = f.perfiles
        .filter(g => g.individuo == individuo && !g.sinDatos)
        .map(g => g.locus -> g.alelos.map(n => Allele(BigDecimal(alelos(n))): AlleleValue))
        .toMap
      Profile(null, codigos(individuo), null, null, null,
        Map(MapeoGenis.TIPO_DE_ANALISIS.id -> porLocus), None, None, None, None)
    }.toArray

    // A diferencia de MapeoGenis.mapear (paso 5, que rechaza con Left
    // cualquier sexo que no sea M/F), aca no se revalida: la invariante ya
    // la garantiza PaqueteMasiva.leer al parsear pedigries.csv
    // (`exigir(sexo == "M" || sexo == "F", ...)`).
    // En el bloque 1, Paquete.leer no valida el sexo, por eso MapeoGenis
    // necesita su propia guarda; duplicarla aca seria codigo inalcanzable.
    val genograma = t.individuos.sortBy(_.alias).map { i =>
      Individual(
        NodeAlias(i.alias),
        i.padre.map(NodeAlias(_)),
        i.madre.map(NodeAlias(_)),
        if (i.sexo == "M") Sex.Male else Sex.Female,
        codigos.get(i.alias),
        i.rol == Rol.BajoPrueba,
        None)
    }.toArray

    Right(EntradaFamilia(f.familia, f.tipo, perfiles, genograma, t.bajoPrueba))
  }

  // El POI llega al motor como un Profile suelto, sin nodo en el genograma:
  // getLR lo cruza contra la genotipificacion del desaparecido.
  def perfilDePoi(
    poi: Poi, alelos: Map[String, Double], indice: Int
  ): Either[String, Profile] = {
    val conDropout = poi.genotipos.filter(_.bandaUnica)
    if (conDropout.nonEmpty)
      return Left(s"${poi.poi}: dropout no representable en GENis " +
        s"(${conDropout.map(_.locus).sorted.mkString(", ")})")
    val porLocus = poi.genotipos
      .filterNot(_.sinDatos)
      .map(g => g.locus -> g.alelos.map(n => Allele(BigDecimal(alelos(n))): AlleleValue))
      .toMap
    Right(Profile(null, codigoDe(500000 + indice), null, null, null,
      Map(MapeoGenis.TIPO_DE_ANALISIS.id -> porLocus), None, None, None, None))
  }
}
