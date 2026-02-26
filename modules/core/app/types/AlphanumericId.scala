package types

case class AlphanumericId(override val text: String)
  extends ConstrainedText(text, AlphanumericId.validationRe)

object AlphanumericId {
  val validationRe = """^\w{2,}$""".r
  implicit val reads: play.api.libs.json.Reads[AlphanumericId] = ConstrainedText.readsOf(AlphanumericId.apply)
  implicit val writes: play.api.libs.json.Writes[AlphanumericId] = ConstrainedText.writesOf[AlphanumericId]
  implicit val qsBinder: play.api.mvc.QueryStringBindable[AlphanumericId] = ConstrainedText.qsBinderOf(AlphanumericId.apply)
  implicit val pathBinder: play.api.mvc.PathBindable[AlphanumericId] = ConstrainedText.pathBinderOf(AlphanumericId.apply)
}
