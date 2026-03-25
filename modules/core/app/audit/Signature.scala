package audit

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Renamed from 'Serializable' to avoid clash with java.io.Serializable
trait Stringifiable {
  def stringify: String
}

trait Signature extends Stringifiable {
  val signature: Key

  def computeSignature(key: Key): Key = Signature.computeSignature(stringify, key)
}

object Signature {
  private val mac = Mac.getInstance("HmacSHA256")

  def computeSignature(txt: String, key: Key): Key = {
    val secretKey = new SecretKeySpec(key.bytes.toArray, "HmacSHA256")
    val m = mac.clone().asInstanceOf[Mac]
    m.init(secretKey)
    Key(m.doFinal(txt.getBytes("UTF-8")).toList)
  }
}
