package types


case class SampleCode(override val text: String)
  extends ConstrainedText(text, SampleCode.validationRe)

object SampleCode {
  val validationRe = """^[A-Z]{2,3}-[A-Z]-[A-Z]+-\d+$""".r
  implicit val reads: play.api.libs.json.Reads[SampleCode] = ConstrainedText.readsOf(SampleCode.apply)
  implicit val writes: play.api.libs.json.Writes[SampleCode] = ConstrainedText.writesOf[SampleCode]
  implicit val qsBinder: play.api.mvc.QueryStringBindable[SampleCode] = ConstrainedText.qsBinderOf(SampleCode.apply)
  implicit val pathBinder: play.api.mvc.PathBindable[SampleCode] = ConstrainedText.pathBinderOf(SampleCode.apply)
}
