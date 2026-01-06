package pedigree

import play.api.libs.json.Json

case class MutationModel(id: Long,name: String,mutationType:Long,active:Boolean,ignoreSex:Boolean, cantSaltos:Long)
object MutationModel {
  implicit val mutationModel = Json.format[MutationModel]
}
case class MutationModelFull(header:MutationModel,parameters:List[MutationModelParameter])
object MutationModelFull {
  implicit val mutationModelFull = Json.format[MutationModelFull]
}
//case class MutationModelParameterFull(i1: Long, i2: Long, l:String, s:String, r1:Option[scala.math.BigDecimal],
//                                      r2:Option[scala.math.BigDecimal], r3:Option[scala.math.BigDecimal])
//object MutationModelParameterFull {
//  implicit val mutationModelParameterFull = Json.format[MutationModelParameterFull]
//
//}
//case class MutationModelFullUpdate(header:MutationModel,parameters:List[MutationModelParameterFull])
//object MutationModelFullUpdate {
//  implicit val mutationModelFullUpdate = Json.format[MutationModelFullUpdate]
//}
