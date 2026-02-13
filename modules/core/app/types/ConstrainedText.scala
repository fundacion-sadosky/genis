package types

import play.api.libs.json.{JsError, JsString, JsSuccess, JsValue, Reads, Writes}
import play.api.mvc.{PathBindable, QueryStringBindable}
import scala.util.matching.Regex

abstract class ConstrainedText(val text: String, val validationRe: Regex) extends Serializable {

  text match {
    case validationRe(_*) => ()
    case _ =>
      val message = s"Invalid string format for a ${this.getClass.getName} object, '$text' does not match pattern /${validationRe}/"
      throw new IllegalArgumentException(message)
  }

}

object ConstrainedText {

  def readsOf[T <: ConstrainedText](factory: String => T): Reads[T] = new Reads[T] {
    def reads(consJson: JsValue) = {
      try {
        JsSuccess(factory(consJson.as[String]))
      } catch {
        case err: IllegalArgumentException => JsError(err.getMessage)
      }
    }
  }

  def writesOf[T <: ConstrainedText]: Writes[T] = new Writes[T] {
    def writes(consTxt: T): JsValue = JsString(consTxt.text)
  }

  def qsBinderOf[T <: ConstrainedText](factory: String => T)(using stringBinder: QueryStringBindable[String]): QueryStringBindable[T] = new QueryStringBindable[T] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, T]] = {
      stringBinder.bind(key, params).map { consTxtEither =>
        consTxtEither.flatMap { consTxt =>
          try {
            Right(factory(consTxt))
          } catch {
            case err: IllegalArgumentException => Left(err.getMessage)
          }
        }
      }
    }

    override def unbind(key: String, consTxt: T): String = {
      stringBinder.unbind(key, consTxt.text)
    }
  }

  def pathBinderOf[T <: ConstrainedText](factory: String => T)(using stringBinder: PathBindable[String]): PathBindable[T] = new PathBindable[T] {
    override def bind(key: String, value: String): Either[String, T] = {
      stringBinder.bind(key, value).flatMap { consTxt =>
        try {
          Right(factory(consTxt))
        } catch {
          case err: IllegalArgumentException => Left(err.getMessage)
        }
      }
    }

    override def unbind(key: String, consTxt: T): String = {
      stringBinder.unbind(key, consTxt.text)
    }
  }

}
