package audit

import scala.collection.immutable.Seq
import play.api.libs.json.{JsString, JsValue, Writes}
import java.security.SecureRandom

case class Key(bytes: Seq[Byte]) {

  private val bytesToHex: Seq[Byte] => String = _.foldLeft("") {
    case (prev, byte) => prev + "%02x".format(byte)
  }

  def asHexaString(): String =
    (for (group <- bytes.grouped(4)) yield bytesToHex(group)).mkString("-")

  override def toString(): String = asHexaString().take(17) + " ..."
}

object Key {

  private val hexToByte: String => Byte = s => Integer.parseInt(s, 16).toByte

  def apply(s: String): Key = {
    val filtered = s.filterNot(_ == '-')
    Key(filtered.grouped(2).map(hexToByte).toList)
  }

  def apply(random: SecureRandom): Key = {
    val key: Array[Byte] = new Array[Byte](32)
    random.nextBytes(key)
    Key(key.toList)
  }

  given writes: Writes[Key] = new Writes[Key] {
    def writes(key: Key): JsValue = JsString(key.asHexaString())
  }
}
