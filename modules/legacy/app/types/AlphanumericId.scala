package types

case class AlphanumericId(override val text: String)
  extends ConstrainedText(text, AlphanumericId.validationRe)

object AlphanumericId {

  val validationRe = """^\w{2,}$""".r

  implicit val reads = ConstrainedText.readsOf(AlphanumericId.apply)
  implicit val writes = ConstrainedText.writesOf[AlphanumericId]
  implicit val qsBinder = ConstrainedText.qsBinderOf(AlphanumericId.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(AlphanumericId.apply)

}
