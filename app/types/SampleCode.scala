package types

case class SampleCode(override val text: String)
  extends ConstrainedText(text, SampleCode.validationRe)

object SampleCode {

  val validationRe = """^[A-Z]{2,3}-[A-Z]-[A-Z]+-\d+$""".r

  implicit val reads = ConstrainedText.readsOf(SampleCode.apply)
  implicit val writes = ConstrainedText.writesOf[SampleCode]
  implicit val qsBinder = ConstrainedText.qsBinderOf(SampleCode.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(SampleCode.apply)

}
