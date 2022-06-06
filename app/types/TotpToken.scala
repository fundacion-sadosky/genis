package types

case class TotpToken(override val text: String)
  extends ConstrainedText(text, TotpToken.validationRe)

object TotpToken {

  val validationRe = """^\d{6}$""".r

  implicit val reads = ConstrainedText.readsOf(TotpToken.apply)
  implicit val writes = ConstrainedText.writesOf[TotpToken]
  implicit val qsBinder = ConstrainedText.qsBinderOf(TotpToken.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(TotpToken.apply)

}
