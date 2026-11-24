package validation

import kits.AnalysisType
import matching.{AleleRange, NewMatchingResult}
import pedigree.{Individual, NodeAlias}
import profile.{Allele, AlleleValue, Profile}
import types.{SampleCode, Sex}

// Los argumentos que consume BayesianNetwork.calculateProbability, ya
// traducidos desde un Caso.
case class EntradaGenis(
  perfiles: Array[Profile],
  genograma: Array[Individual],
  frecuencias: Map[String, Map[Double, Double]],
  universo: Map[String, List[Double]],
  pi: String,
  // Rango de alelos por locus para MatchingAlgorithm.convertAleles: sin el,
  // el motor usa 0..99 y convierte el codigo del silente. Ver rangoDeAlelos.
  rango: NewMatchingResult.AlleleMatchRange
)

// Traduce un Caso a los argumentos que espera
// BayesianNetwork.calculateProbability. Cada regla de aca existe por un
// desajuste concreto entre el vocabulario del paquete y el de GENis.
object MapeoGenis {

  // GENis guarda los alelos como Double. El paquete los da como cadenas, y
  // ahi "10" y "10.0" NO son el mismo alelo, asi que la traduccion tiene que
  // verificarse inyectiva.
  private val numerico = """^\d+(\.\d)?$""".r

  // El alelo silente no tiene numero. Se le da un codigo fuera de cualquier
  // escalera real, evitando -1.0 (piso de frecuencia) y -100.0 (alelo
  // residual que inyecta getNormalizedFrequencyTable).
  //
  // OJO: un codigo negativo no alcanza por si solo. El motor pasa los
  // perfiles de los familiares por MatchingAlgorithm.convertAleles, y con
  // locusRangeMap vacio usa el rango 0..99: todo alelo negativo se vuelve
  // OutOfLadderAllele, que las CPT representan como -1.0. Sin un rango
  // explicito, el silente de un familiar llegaria al motor como el piso de
  // frecuencia y el del PI como -7.0. Por eso la entrada lleva `rango`
  // (rangoDeAlelos) y Corredor se lo pasa al motor.
  val CODIGO_SILENTE: Double = -7.0

  // Rango por locus para convertAleles: el mismo 0..99 que el motor usa por
  // omision, con el extremo inferior bajado hasta el codigo del silente. No
  // recorta nada de lo que el rango por omision dejaba pasar (un alelo
  // observado fuera del panel sigue extendiendo el universo), solo suma el
  // silente.
  def rangoDeAlelos(c: Caso): NewMatchingResult.AlleleMatchRange =
    c.frecuencias.keys.map { locus =>
      locus -> AleleRange(BigDecimal(math.min(CODIGO_SILENTE, 0.0)), BigDecimal(99))
    }.toMap

  def mapearAlelos(nombres: Set[String]): Either[String, Map[String, Double]] = {
    val traducidos = nombres.toList.sorted.map {
      case n @ "silent" => Right(n -> CODIGO_SILENTE)
      case n if numerico.pattern.matcher(n).matches() => Right(n -> n.toDouble)
      case n => Left(s"alelo '$n' no representable en GENis (los alelos son Double)")
    }

    val errores = traducidos.collect { case Left(e) => e }
    if (errores.nonEmpty) return Left(errores.mkString("; "))

    val mapa = traducidos.collect { case Right(p) => p }.toMap
    if (mapa.values.toSet.size != mapa.size) {
      val colisiones = mapa.groupBy(_._2).filter(_._2.size > 1)
        .map { case (v, ns) => s"${ns.keys.toList.sorted.mkString("/")} -> $v" }
      Left(s"el mapeo de alelos no es inyectivo: ${colisiones.mkString(", ")}")
    } else Right(mapa)
  }

  // El piso de frecuencia que se le declara a GENis, y como lo usa el motor.
  //
  // GENis exige una entrada de alelo -1.0 con la frecuencia minima
  // (getFrequency la lee siempre). Ademas devuelve el piso para
  // todo alelo con frecuencia <= piso. getNormalizedFrequencyTable ya piso y
  // renormalizo el panel entero antes -- la entrada -1.0 incluida --, asi que
  // esa sustitucion es idempotente: el alelo pisado y el piso valen los dos
  // fmin/S. La entrada -1.0 no entra en la suma de esa renormalizacion
  // (flooredAlleles.filterKeys(_ != -1.0).values.sum), asi que el piso no
  // perturba la normalizacion.
  //
  // PISO_INERTE es el valor para los casos que NO declaran piso. Tiene que
  // quedar por debajo de la frecuencia mas chica de cualquier panel del
  // catalogo, para que la sustitucion de getFrequency no se dispare nunca;
  // 1e-300 lo garantiza sin depender del panel. La minima del catalogo es
  // 1e-08 (el alelo raro de FRQ-03) y sobra margen de casi 300 ordenes.
  //
  // Los dos casos que declaran piso (FRQ-02 y FRQ-03) lo traen en la columna
  // `piso` de casos.csv y entregan el panel CRUDO, asi que a GENis hay que
  // declararle ese valor y no el inerte: llega desde Runner.scala
  // (c.piso.getOrElse(PISO_INERTE)). La operacion que se mide con ellos es
  // "subir al piso toda frecuencia menor y despues dividir el panel entero
  // por la suma resultante". FRQ-02S es el gemelo de FRQ-02 sin piso
  // declarado: que salga exacto descarta que una diferencia venga de otra
  // parte del calculo.
  //
  // Cuidado con predecir 250 y 500 a mano. El LR de estos casos se reduce a
  // 1/(4*p_raro) y con p_raro = piso da esos dos enteros, pero el piso
  // renormalizado vale piso/S, con S la masa del panel despues de pisar
  // (1.000999 y 1.00049999): el LR es 250*S y 500*S, que es lo que devuelve
  // el motor.
  val PISO_INERTE: Double = 1e-300

  def tablaDeFrecuencias(
    c: Caso,
    alelos: Map[String, Double],
    piso: Double
  ): Map[String, Map[Double, Double]] =
    c.frecuencias.map { case (locus, panel) =>
      locus -> (panel.map { case (nombre, f) => alelos(nombre) -> f } + (-1.0 -> piso))
    }

  // El universo de alelos `n` es el soporte de las variables de genotipo de
  // los individuos NO tipificados, y ademas gobierna el descarte de
  // marcadores de getMarkersToFilter. Fijandolo en el panel del caso, la
  // unica aproximacion del motor desaparece y la corrida no depende del
  // estado de ninguna base.
  def universo(c: Caso, alelos: Map[String, Double]): Map[String, List[Double]] =
    c.frecuencias.map { case (locus, panel) =>
      locus -> panel.keys.toList.map(alelos).sorted
    }

  // El catalogo es STR autosomico y nada mas.
  val TIPO_DE_ANALISIS = AnalysisType(1, "Autosomal")

  // SampleCode valida contra ^[A-Z]{2,3}-[A-Z]-[A-Z]+-\d+$, asi que los
  // codigos se fabrican; el catalogo no trae ninguno.
  private def codigoDe(indice: Int): SampleCode = SampleCode(f"AR-C-VAL-$indice%04d")

  def mapear(c: Caso, piso: Double): Either[String, EntradaGenis] = {
    // 1. Dropout: no hay forma de escribir "se observo una banda, la otra no
    //    se leyo", asi que el caso no se puede cargar. GENis no lee un solo
    //    alelo como homocigoto: lo excluye del calculo y lo informa,
    //    aportando LR 1; no marginaliza sobre el alelo perdido.
    val conDropout = c.perfiles.filter(_.bandaUnica)
    if (conDropout.nonEmpty) {
      val donde = conDropout.map(g => s"${g.individuo}/${g.locus}").mkString(", ")
      return Left(s"dropout no representable en GENis: no hay forma de " +
        s"escribir una banda observada con la otra no leida ($donde)")
    }

    // 2. El PI, verificado.
    val pi = DeteccionPI.detectar(c) match {
      case Left(motivo) => return Left(motivo)
      case Right(p) => p
    }

    // 3. Alelos, panel por panel, con el mapeo unificado del caso.
    val nombres = c.frecuencias.values.flatMap(_.keySet).toSet ++
      c.perfiles.flatMap(_.alelos).toSet
    val alelos = MapeoGenis.mapearAlelos(nombres) match {
      case Left(motivo) => return Left(motivo)
      case Right(m) => m
    }

    // 4. Aliases: NodeAlias valida contra ^[a-zA-Z0-9\-]{1,15}$.
    val invalidos = c.h1.keys.filterNot(a => a.matches("""^[a-zA-Z0-9\-]{1,15}$"""))
    if (invalidos.nonEmpty)
      return Left(s"alias no validos para NodeAlias: ${invalidos.toList.sorted.mkString(", ")}")

    // 5. Sexo: "M"/"F" son los unicos valores que trae pedigries.csv hoy,
    //    pero la traduccion no puede caer en Female en silencio para
    //    cualquier otro valor. Un sexo mal traducido cambia que alelo
    //    hereda cada hijo: produce un LR plausible y equivocado, no un
    //    error ruidoso.
    val sexoInvalido = c.h1.values.toList.filterNot(i => i.sexo == "M" || i.sexo == "F")
    if (sexoInvalido.nonEmpty) {
      val donde = sexoInvalido.sortBy(_.alias).map(i => s"${i.alias}='${i.sexo}'").mkString(", ")
      return Left(s"sexo no representable en GENis (valores validos: M, F): $donde")
    }

    // 6. Un codigo por individuo tipificado.
    val tipificados = c.tipificados.toList.sorted
    val codigos = tipificados.zipWithIndex.map { case (a, i) => a -> codigoDe(i + 1) }.toMap

    val perfiles = tipificados.map { individuo =>
      val porLocus = c.perfiles
        .filter(g => g.individuo == individuo && !g.sinDatos)
        .map { g =>
          g.locus -> g.alelos.map(n => Allele(BigDecimal(alelos(n))): AlleleValue)
        }.toMap
      Profile(null, codigos(individuo), null, null, null,
        Map(TIPO_DE_ANALISIS.id -> porLocus), None, None, None, None)
    }.toArray

    // El chequeo del paso 5 ya garantizo que i.sexo es "M" o "F" para todo
    // individuo de h1, asi que esta traduccion es total en la practica.
    val genograma = c.h1.values.toList.sortBy(_.alias).map { i =>
      Individual(
        NodeAlias(i.alias),
        i.padre.map(NodeAlias(_)),
        i.madre.map(NodeAlias(_)),
        if (i.sexo == "M") Sex.Male else Sex.Female,
        codigos.get(i.alias),
        i.alias == pi,
        None)
    }.toArray

    Right(EntradaGenis(
      perfiles = perfiles,
      genograma = genograma,
      frecuencias = tablaDeFrecuencias(c, alelos, piso),
      universo = universo(c, alelos),
      pi = pi,
      rango = rangoDeAlelos(c)))
  }
}
