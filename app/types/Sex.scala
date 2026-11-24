package types

import util.PlayEnumUtils

object Sex extends Enumeration {
  
  type Sex = Value
  
  val Female, Male, Unknown = Value
  
  // @transient: un valor de esta Enumeration (p.ej. Individual.sex) arrastra,
  // al serializarse, el objeto Sex entero -- y con el, este Format, que no es
  // serializable (play.api.libs.json.Format$$anon$3). Hace falta porque
  // PedigreeSparkMatcher captura Array[Individual] en el closure de Spark
  // (sin esto: SparkException: Task not serializable). El Format
  // no necesita sobrevivir una serializacion: es puro, solo cierra sobre
  // `Sex`, y `lazy` lo reconstruye solo si algo lo vuelve a pedir del lado
  // deserializado.
  @transient implicit lazy val sexFormat = PlayEnumUtils.enumFormat(Sex)
}