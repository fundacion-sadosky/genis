package types

case class Email(override val text: String)
  extends ConstrainedText(text, Email.validationRe)

object Email {

  val validationRe = """^[_\w-\+]+(\.[_\w-]+)*@[\w-]+(\.\w+)*(\.[A-Za-z]{2,})$""".r

  implicit val reads = ConstrainedText.readsOf(Email.apply)
  implicit val writes = ConstrainedText.writesOf[Email]
  implicit val qsBinder = ConstrainedText.qsBinderOf(Email.apply)
  implicit val pathBinder = ConstrainedText.pathBinderOf(Email.apply)

}
