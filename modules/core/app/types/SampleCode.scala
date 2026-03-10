package types

import play.api.libs.json.{Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}

case class SampleCode(override val text: String)
  extends ConstrainedText(text, SampleCode.validationRe)

object SampleCode {
  val validationRe = """^[A-Z]{2,3}-[A-Z]-[A-Z]+-\d+$""".r

  // Scala 3: implicit val without explicit type is an error (was a warning in Scala 2).
  implicit val reads: Reads[SampleCode] = ConstrainedText.readsOf(SampleCode.apply)
  implicit val writes: Writes[SampleCode] = ConstrainedText.writesOf[SampleCode]
  implicit val qsBinder: QueryStringBindable[SampleCode] = ConstrainedText.qsBinderOf(SampleCode.apply)
  implicit val pathBinder: PathBindable[SampleCode] = ConstrainedText.pathBinderOf(SampleCode.apply)
}