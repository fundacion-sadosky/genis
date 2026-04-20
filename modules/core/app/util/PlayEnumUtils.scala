package util

import play.api.libs.json.*

object PlayEnumUtils:

  def enumReads[E <: Enumeration](e: E): Reads[e.Value] = new Reads[e.Value]:
    def reads(json: JsValue): JsResult[e.Value] = json match
      case JsString(s) =>
        try JsSuccess(e.withName(s))
        catch
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: '${e.getClass}', but it does not contain value: '$s'")
      case _ => JsError("String value expected")

  def enumWrites[E <: Enumeration](e: E): Writes[e.Value] = new Writes[e.Value]:
    def writes(v: e.Value): JsValue = JsString(v.toString)

  def enumFormat[E <: Enumeration](e: E): Format[e.Value] =
    Format(enumReads(e), enumWrites(e))
