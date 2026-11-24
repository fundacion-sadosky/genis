package validation

// Modelo de datos del paquete de casos de lrval. Sin logica: es el
// resultado crudo de leer los CSV, antes de cualquier traduccion a los
// tipos de GENis.

case class IndividuoPed(
  alias: String,
  padre: Option[String],
  madre: Option[String],
  sexo: String // "M" | "F"
)

case class Genotipo(
  individuo: String,
  locus: String,
  alelo1: Option[String],
  alelo2: Option[String]
) {
  // Los alelos son CADENAS, no numeros: "9.3", "15.2", "silent".
  def alelos: List[String] = List(alelo1, alelo2).flatten
  def sinDatos: Boolean = alelo1.isEmpty && alelo2.isEmpty
  // Una sola banda observada: dropout. Distinto de homocigoto.
  def bandaUnica: Boolean = alelo1.isDefined != alelo2.isDefined
}

case class CeldaMutacion(
  locus: String,
  sexoProgenitor: String, // "M" | "F"
  aleloOrigen: String,
  aleloDestino: String,
  probabilidad: Double
)

case class ModeloDeclarado(
  locus: String,
  sexoProgenitor: String,
  modelo: String, // "equal" | "stepwise" | "stepwise_extendido"
  tasa: Double,
  tasa2: Option[Double],
  rango: Option[Double],
  alelos: List[String], // la escalera alelica completa
  // Cantidad maxima de pasos que el modelo cubre antes de truncar. Es un
  // parametro real del modelo de GENis que ningun motor de referencia expone.
  // Viaja declarado en el paquete: tomarlo de la amplitud de la escalera
  // seria inferir un parametro del sistema bajo validacion.
  pasos: Option[Int] = None
)

case class Caso(
  id: String,
  grupo: String,
  theta: Double,
  h1: Map[String, IndividuoPed],
  h2: Map[String, IndividuoPed],
  perfiles: List[Genotipo],
  frecuencias: Map[String, Map[String, Double]], // locus -> alelo -> frecuencia
  matriz: List[CeldaMutacion],
  modelo: List[ModeloDeclarado],
  // Claves repetidas detectadas en frecuencias.csv/mutacion.csv al construir
  // este caso, ya formateadas como mensaje de violacion. frecuencias y matriz
  // son Map/List construidos a partir de las filas crudas; una fila repetida
  // se pisa (frecuencias) o se puede leer sin que ningun otro invariante lo
  // note (matriz), asi que hay que capturarla en Paquete.leer, antes de que
  // se pierda, para que verificarInvariantes la pueda reportar. Default Nil:
  // aditivo para quien arma un Caso con `.copy(...)` a partir de uno leido.
  duplicados: List[String] = Nil,
  // LR_referencia de casos.csv, solo para INFORMAR la discrepancia en
  // validation/out/contraste.txt. El arnes NO lo usa para calcular -- eso
  // convertiria la validacion en una tautologia -- por eso vive en un campo
  // aparte, con default None, y ningun modulo de calculo (MapeoGenis,
  // Corredor) lo lee jamas.
  lrReferencia: Option[Double] = None,
  // LR_otra_convencion de casos.csv: el LR del mismo caso bajo la otra
  // convencion documentada (pedigri podado en vez de completo, genotipo
  // literal en vez de banda). Solo lo traen los casos donde las dos
  // convenciones dan numeros distintos, y como el anterior solo se usa para
  // informar: quien da exactamente este valor recibe CONVENCION y no FALLA.
  lrOtraConvencion: Option[Double] = None,
  // Piso de frecuencia declarado por el caso en casos.csv. Los dos casos que
  // lo ejercitan (FRQ-02 y FRQ-03) entregan el panel CRUDO y declaran el piso
  // aca, asi que este es el valor que hay que pasarle a GENis
  // (Corredor.correr / MapeoGenis.mapear).
  // Option, no Double: `None` significa "el caso no declara piso y el sistema
  // no debe aplicar ninguno", que NO es lo mismo que declarar 0.0. La
  // diferencia es medible -- FRQ-02S es el gemelo de FRQ-02 sin piso
  // declarado, y su unico proposito es detectar a un sistema que aplique un
  // piso propio no declarado. Default None: aditivo.
  piso: Option[Double] = None
) {
  def conModeloDeMutacion: Boolean = matriz.nonEmpty || modelo.nonEmpty

  // Individuos con al menos un alelo leido en algun locus.
  def tipificados: Set[String] =
    perfiles.filterNot(_.sinDatos).map(_.individuo).toSet
}
