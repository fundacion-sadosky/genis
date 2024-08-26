package types

case class FreeText(override val text: String)
  extends ConstrainedText(text, FreeText.validationRe)

object FreeText {

  val validationRe = "^.+$".r

  implicit val reads = ConstrainedText.readsOf(FreeText.apply)
  implicit val writes = ConstrainedText.writesOf[FreeText]
  implicit val qsBinder = ConstrainedText.qsBinderOf(FreeText.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(FreeText.apply)

}
