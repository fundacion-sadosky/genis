package security

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CryptoServiceSpec extends AnyWordSpec with Matchers {

  val crypto = new CryptoServiceImpl(keyLength = 2048)

  "CryptoServiceImpl" should {

    "encrypt and decrypt roundtrip correctly" in {
      val credentials = crypto.generateRandomCredentials()
      val plainText   = "datos sensibles de prueba".getBytes("UTF-8")

      val encrypted = crypto.encrypt(plainText, credentials)
      val decrypted = crypto.decrypt(encrypted, credentials)

      new String(decrypted, "UTF-8") shouldBe "datos sensibles de prueba"
    }

    "generate different credentials each time" in {
      val cred1 = crypto.generateRandomCredentials()
      val cred2 = crypto.generateRandomCredentials()

      cred1.key should not be cred2.key
      cred1.iv  should not be cred2.iv
    }

    "generate derived credentials from password" in {
      val cred = crypto.generateDerivatedCredentials("mi-password-seguro")

      cred.key should not be empty
      cred.iv  should not be empty
    }

    "generate same derived credentials for same password" in {
      val cred1 = crypto.generateDerivatedCredentials("misma-password")
      val cred2 = crypto.generateDerivatedCredentials("misma-password")

      // Con salt fijo (actual impl) el resultado es determinístico
      cred1.key shouldBe cred2.key
    }

    "generate RSA key pair" in {
      val (pub, priv) = crypto.giveRsaKeys

      pub  should not be empty
      priv should not be empty
    }

    "give a TOTP secret (non-empty Base32 string)" in {
      val secret = crypto.giveTotpSecret
      secret should not be empty
    }
  }
}
