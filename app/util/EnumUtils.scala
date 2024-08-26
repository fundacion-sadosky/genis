package util

import play.api.libs.json._
import scala.language.implicitConversions

object EnumJsonUtils {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = {
    
    class SerializableReads extends Reads[E#Value] with Serializable { // we need a serializable read so Spark Works
      def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) => {
          try {
            JsSuccess(enum.withName(s))
          } catch {
            case _: NoSuchElementException =>
              JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
          }
        }
        case _ => JsError("String value expected")
      }
    }

    new SerializableReads
    
//    val pepe = new Reads[E#Value] {
//      def reads(json: JsValue): JsResult[E#Value] = json match {
//        case JsString(s) => {
//          try {
//            JsSuccess(enum.withName(s))
//          } catch {
//            case _: NoSuchElementException =>
//              JsError("Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
//          }
//        }
//        case _ => JsError("String value expected")
//      }
//    }
//    pepe
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] =
    new Writes[E#Value] {
      def writes(v: E#Value): JsValue = JsString(v.toString)
    }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }
}
