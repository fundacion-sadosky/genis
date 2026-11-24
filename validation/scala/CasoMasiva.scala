package validation

// Modelo de datos del paquete de busqueda masiva. Sin logica: es el resultado
// crudo de leer los CSV, antes de cualquier traduccion a los tipos de GENis.
//
// Reusa Genotipo y ModeloDeclarado de Caso.scala: los alelos siguen siendo
// CADENAS ("9.3", "15.2"), y modelos.csv del bloque 2 es el del bloque 1 sin
// la columna `caso`.

object Rol {
  val BajoPrueba = "bajo_prueba"
  val Tipificado = "tipificado"
  val NoTipificado = "no_tipificado"
  val validos: Set[String] = Set(BajoPrueba, Tipificado, NoTipificado)
}

case class IndividuoMasiva(
  alias: String,
  padre: Option[String],
  madre: Option[String],
  sexo: String, // "M" | "F"
  rol: String   // ver Rol
)

case class TipoPed(
  tipo: String,
  etiqueta: String,
  grado: String,
  individuos: List[IndividuoMasiva]
) {
  def bajoPrueba: String = individuos.find(_.rol == Rol.BajoPrueba).get.alias
  def tipificados: List[String] = individuos.filter(_.rol == Rol.Tipificado).map(_.alias).sorted
}

case class FamiliaMasiva(
  familia: String,
  tipo: String,
  perfiles: List[Genotipo]
)

// La nomina. `genotipos` usa Genotipo, cuyo campo `individuo` lleva el id del
// POI: la nomina no pertenece a ninguna familia.
case class Poi(
  poi: String,
  genotipos: List[Genotipo]
)

case class PaqueteMasivo(
  tipos: Map[String, TipoPed],
  familias: List[FamiliaMasiva],
  poi: List[Poi],
  frecuencias: Map[String, Map[String, Double]], // locus -> alelo -> frecuencia
  modelo: List[ModeloDeclarado],                 // vacio en la condicion sin mutacion
  manifiesto: String                             // contenido crudo de MANIFIESTO.txt
) {
  def conMutacion: Boolean = modelo.nonEmpty
}
