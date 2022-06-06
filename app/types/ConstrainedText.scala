package types

import play.api.libs.json.JsError
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsValue
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable
import scala.util.matching.Regex

abstract class ConstrainedText(val text: String, val validationRe: Regex) extends Serializable {

  text match {
    case validationRe(_*) => ()
    case _ =>
      val message = "Invalid string format for a %1s object, '%2s' does not match pattern /%3s/"
        .format(this.getClass().getName(), text, validationRe)
      throw new IllegalArgumentException(message)
  }

}

object ConstrainedText {

  def readsOf[T <: ConstrainedText](factory: (String => T)): Reads[T] = new Reads[T] {
    def reads(consJson: JsValue) = {
      try {
        JsSuccess(factory(consJson.as[String]))
      } catch {
        case err: IllegalArgumentException => JsError(err.getMessage())
      }
    }

  }

  def writesOf[T <: ConstrainedText](): Writes[T] = new Writes[T] {
    def writes(consTxt: T): JsValue = JsString(consTxt.text)
  }

  def qsBinderOf[T <: ConstrainedText](factory: (String => T))(implicit stringBinder: QueryStringBindable[String]): QueryStringBindable[T] = new QueryStringBindable[T] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      stringBinder.bind(key, params) map { consTxtEither =>
        consTxtEither.right flatMap { consTxt =>
          try {
            Right(factory(consTxt))
          } catch {
            case err: IllegalArgumentException => Left(err.getMessage())
          }
        }
      }
    }

    override def unbind(key: String, consTxt: T): String = {
      stringBinder.unbind(key, consTxt.text)
    }
  }

  def pathBinderOf[T <: ConstrainedText](factory: (String => T))(implicit stringBinder: PathBindable[String]) = new PathBindable[T] {
    override def bind(key: String, value: String): Either[String, T] = {
      stringBinder.bind(key, value).right flatMap { consTxt =>
        try {
          Right(factory(consTxt))
        } catch {
          case err: IllegalArgumentException => Left(err.getMessage())
        }
      }
    }

    override def unbind(key: String, consTxt: T): String = {
      stringBinder.unbind(key, consTxt.text)
    }
  }

}
