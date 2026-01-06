package audit

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import scala.collection.immutable.Seq

trait Serializable {
  def stringify: String
}

trait Signature extends Serializable {
  val signature: Key

  def computeSignature(key: Key): Key = Signature.computeSignature(stringify, key)

}

object Signature {
  val mac = Mac.getInstance("HmacSHA256")
  def computeSignature(txt: String, key: Key): Key = {
    val secretKey = new SecretKeySpec(key.bytes.toArray, "HmacSHA256");
    val mac = Signature.mac.clone().asInstanceOf[Mac]
    mac.init(secretKey);
    Key(mac.doFinal(txt.getBytes("UTF-8")).toList)
  }
}
