package audit

import java.security.SecureRandom
import scala.util.Random.javaRandomToRandom
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.collection.immutable.IndexedSeq

class KeyTest extends FlatSpec with Matchers {

  val hexa = """^([a-f\d]{8}-){1}[a-f\d]{8} \.\.\.$""".r

  "Key" should
    "create random keys" in {
      val rand = new SecureRandom
      val key = Key(rand).toString
      key should fullyMatch regex hexa
    }

  "Key" should
    "create keys from byte array" in {
      val key = new Key(IndexedSeq.fill(256 / 8)(0.toByte))
      key.asHexaString should equal("00000000-00000000-00000000-00000000-00000000-00000000-00000000-00000000")
    }

  "Key" should
    "has a nice to string implementation" in {
      val rand = new SecureRandom
      for (i <- 0 to 100) {
        val key = Key(rand).toString
        key should fullyMatch regex hexa
      }

    }
  "Key" should
    "be created from an string" in {
      val s = "a43d46a1-28bc08ce-11ece777-82f9a723-a43d46a1-28bc08ce-11ece777-82f9a723"

      val lk = Key(s)

      assert(s === lk.asHexaString)
    }
}