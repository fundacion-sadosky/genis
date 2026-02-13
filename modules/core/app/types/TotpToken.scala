package types

case class TotpToken(override val text: String)
  extends ConstrainedText(text, TotpToken.validationRe)

object TotpToken {

  val validationRe = """^(\d{6})$""".r

  given reads: play.api.libs.json.Reads[TotpToken] = ConstrainedText.readsOf(TotpToken.apply)
  given writes: play.api.libs.json.Writes[TotpToken] = ConstrainedText.writesOf[TotpToken]
  given qsBinder(using stringBinder: play.api.mvc.QueryStringBindable[String]): play.api.mvc.QueryStringBindable[TotpToken] =
    ConstrainedText.qsBinderOf(TotpToken.apply)
  given pathBinder(using stringBinder: play.api.mvc.PathBindable[String]): play.api.mvc.PathBindable[TotpToken] =
    ConstrainedText.pathBinderOf(TotpToken.apply)

}
