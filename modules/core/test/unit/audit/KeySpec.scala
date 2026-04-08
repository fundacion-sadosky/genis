package unit.audit

import java.security.SecureRandom

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import audit.Key

class KeySpec extends AnyWordSpec with Matchers {

  "Key" must {

    "generate a random 32-byte key" in {
      val key = Key(new SecureRandom())
      key.bytes.length mustBe 32
    }

    "format toString as abbreviated hex with ellipsis" in {
      val key = Key(new SecureRandom())
      key.toString must fullyMatch regex """^([a-f\d]{8}-){1}[a-f\d]{8} \.\.\.$"""
    }

    "produce full hex string via asHexaString" in {
      val key = Key(new SecureRandom())
      val hex = key.asHexaString()
      // 32 bytes → 8 groups of 4 bytes, separated by '-'
      hex must fullyMatch regex """^([a-f\d]{8}-){7}[a-f\d]{8}$"""
    }

    "round-trip through hex string" in {
      val original = Key(new SecureRandom())
      val hex      = original.asHexaString()
      val restored = Key(hex)
      restored mustBe original
    }

    "create a zero key from a zero byte array" in {
      val zeroKey = Key(Seq.fill(32)(0.toByte))
      zeroKey.bytes.forall(_ == 0.toByte) mustBe true
    }
  }
}
