package validation

// Tabla de anotaciones por caso, PROPIA del arnes -- no se lee de ningun
// archivo del paquete -- que completa la columna `observaciones` de
// resultados_genis.csv.
//
// El problema que resuelve: con `observaciones` vacia, dos filas con
// LR = "0" y estado = "ok" son textualmente identicas aunque una sea una
// exclusion mendeliana (PAR-17) y la otra el resultado de leer una banda
// unica con otra convencion que la referencia (SIL-01). El CSV es lo unico
// que el modulo de referencia lee, y no distingue esos dos ceros: la diferencia solo
// existe si esta escrita aca.
object Observaciones {

  // Reutilizado por PAR-17 y MUT-08: la misma situacion en dos casos.
  private val exclusionMendeliana =
    "Exclusion mendeliana sin modelo de mutacion activo (transmision " +
      "mendeliana exacta): el LR es exactamente cero."

  // Reutilizado por FRQ-02 y FRQ-03, los dos casos que declaran piso sobre un
  // panel con una frecuencia por debajo. Salen `ok` con un LR que en el CSV no
  // se distingue de cualquier otro; la anotacion deja escrito que hizo el
  // sistema con el piso y contra que control se lee. Ver el bloque del piso
  // en MapeoGenis.scala.
  private val notaPiso =
    "El caso declara piso (columna `piso` de casos.csv): GENis sube al piso " +
      "toda frecuencia del panel crudo que queda por debajo y despues " +
      "renormaliza el panel entero -- el alelo pisado incluido -- por la suma " +
      "resultante. FRQ-02S es el mismo caso sin piso declarado, y es el " +
      "control contra el que se lee este."

  val porCaso: Map[String, String] = Map(
    "PAR-17" -> exclusionMendeliana,
    "MUT-08" -> exclusionMendeliana,

    "SIL-01" ->
      ("GENis toma el genotipo tal como esta escrito ('1/1' y '3/3', " +
        "homocigotos): con esos genotipos el duo es una exclusion y el LR es 0. " +
        "El valor de referencia sigue otra convencion de lectura, la de banda " +
        "unica como a/a o a/silente."),

    "FRQ-02" -> notaPiso,
    "FRQ-03" -> notaPiso,

    "FRQ-02S" ->
      ("Control pareado de FRQ-02: mismo pedigri, mismos genotipos y el mismo " +
        "panel crudo, con la columna `piso` vacia -- el caso no declara piso y " +
        "el sistema no debe aplicar ninguno. Todo lo demas del calculo se " +
        "cancela entre los dos casos, asi que la diferencia entre ambos es la " +
        "logica del piso."),

    "INV-04" ->
      ("El unico individuo con genotipo en todo el caso es el padre alegado; " +
        "ni la madre ni el individuo cuestionado tienen datos, asi que no hay " +
        "comparacion informativa posible y el LR es 1. INV-02 es el caso de un " +
        "individuo sin datos agregado a una familia que si los tiene.")
  )

  // A diferencia de `porCaso`, esto NO es una tabla de ids: es una regla sobre
  // lo que el caso DECLARA, y por eso alcanza a cualquier caso futuro.
  //
  // Un caso puede declarar una tasa de mutacion de microvariante (`tasa2`)
  // sobre un modelo que no la usa. GENis la usa solo en el tipo 3
  // (stepwise_extendido, BayesianNetwork.getProbabityOfNonDiagonal caso
  // (3, ...)); el modelo a pasos (tipo 2) da probabilidad exactamente cero a
  // toda transicion entre grupos decimales distintos
  // (MutationService.stepDistance).
  //
  // Sin esta anotacion, un caso con tasa2 que viaja como stepwise llega al
  // otro lado como un LR calculado bajo un modelo que no es exactamente el
  // que el caso declaro, y nada en el CSV lo dice. Un caso que declara el
  // modelo extendido no se anota: su tasa2 se uso. Tampoco uno cuya escalera
  // tiene un solo grupo decimal (misma regla que modelo_contrato del lado R):
  // ahi tasa2 no tiene donde aplicarse y la matriz es la misma con o sin ella.
  def notaDeTasa2(c: Caso): Option[String] = {
    def gruposDecimales(alelos: List[String]): Int =
      alelos.flatMap(a => scala.util.Try(a.toDouble).toOption)
        .map(x => math.round((x - math.floor(x)) * 10)).distinct.size
    val sinLeer = c.modelo.filter(m =>
      m.modelo != "stepwise_extendido" && gruposDecimales(m.alelos) > 1)
    val declaradas = sinLeer.flatMap(_.tasa2).filter(_ != 0.0).distinct.sorted
    if (declaradas.isEmpty) None
    else Some(
      "El caso declara tasa de mutacion de microvariante (tasa2 = " +
        declaradas.map(t => java.lang.Double.toString(t)).mkString(", ") +
        ") sobre el modelo a pasos (tipo 2), que no la usa: ese modelo da " +
        "probabilidad cero a toda transicion entre grupos decimales distintos " +
        "(9 -> 9.3). El modelo que la usa es stepwise_extendido (tipo 3). El LR " +
        "de esta fila corresponde al modelo a pasos sin tasa de microvariante. " +
        "La matriz que se uso viaja en matrices_genis.csv.")
  }

  // Combina el motivo que ya trae un Resultado (NoSoportado/NoRepresentable/
  // Fallo dejan uno; Calculado deja cadena vacia) con la anotacion de esta
  // tabla, si el caso tiene una. Ninguno de los dos lados puede contener
  // '\n' ni '\r': romperia el campo CSV (scala-csv solo escapa comillas, no
  // saltos de linea). El separador " -- " no es una coma, asi que no
  // interfiere con ningun formateo numerico ni con el parser CSV.
  def combinar(caso: String, motivo: String): String =
    (motivo, porCaso.get(caso)) match {
      case ("", None)    => ""
      case (m, None)     => m
      case ("", Some(n)) => n
      case (m, Some(n))  => s"$m -- $n"
    }

  // Version que ve el caso entero, y por lo tanto puede aplicar tambien las
  // anotaciones DERIVADAS de lo que el caso declara (hoy: notaDeTasa2). Es la
  // que usa Runner.filaDe, y se apoya en la de arriba, por id.
  def combinar(c: Caso, motivo: String): String = {
    val base = combinar(c.id, motivo)
    notaDeTasa2(c) match {
      case None       => base
      case Some(nota) => if (base.isEmpty) nota else s"$base -- $nota"
    }
  }
}
