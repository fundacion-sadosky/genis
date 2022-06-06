package audit

import scala.util.Random
import scala.collection.immutable.Seq
import play.api.libs.json.Json
import play.api.libs.json.Writes
import play.api.libs.json.JsValue
import play.api.libs.json.JsString

case class Key(bytes: Seq[Byte]) {

  private val bytesToHex = { bytes: Seq[Byte] =>
    bytes.foldLeft("") {
      case (prev, byte) => prev + "%02x".format(byte)
    }
  }

  def asHexaString(): String = {
    (for (group <- bytes.grouped(4)) yield (bytesToHex(group))) mkString ("-")
  }

  override def toString(): String = asHexaString.take(17) + " ..."
}

object Key {

  private val hexToByte = { string: String =>
    Integer.parseInt(string, 16).toByte
  }

  def apply(s: String): Key = {

    val filteredString = s.filterNot(_ == '-')

    val g = for (group <- filteredString.grouped(2)) yield (hexToByte(group))
    val h = g.toList
    new Key(h)
  }

  def apply(random: Random): Key = {
    val key: Array[Byte] = new Array[Byte](32)
    random.nextBytes(key);
    new Key(key.toList)
  }

  implicit val writes = new Writes[Key] {
    def writes(key: Key): JsValue = JsString(key.asHexaString())
  }

}
