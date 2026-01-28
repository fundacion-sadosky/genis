package connections

import play.api.libs.json.Json

case class InferiorInstance(
                       url: String,
                       laboratory: String =""
                           )

object InferiorInstance {
  implicit val format = Json.format[InferiorInstance]
}
case class InferiorInstanceFull(
                                 id: Long,
                                 url: String,
                                 connectivity: String,
                                 idStatus: Long,
                                 laboratory: String =""
                               )

object InferiorInstanceFull {
  implicit val format = Json.format[InferiorInstanceFull]
}
case class InferiorInstanceStatus(
                                 id: Long,
                                 description: String
                               )
object InferiorInstanceStatus {
  implicit val format = Json.format[InferiorInstanceStatus]
}