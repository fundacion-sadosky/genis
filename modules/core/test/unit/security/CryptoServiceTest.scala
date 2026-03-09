package unit.security

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import security.CryptoServiceImpl

class CryptoServiceTest extends AnyWordSpec with Matchers {

  val crypto = new CryptoServiceImpl(keyLength = 2048)

  "CryptoServiceImpl" must {

    "encrypt and decrypt roundtrip correctly" in {
      val credentials = crypto.generateRandomCredentials()
      val plainText   = "datos sensibles de prueba".getBytes("UTF-8")

      val encrypted = crypto.encrypt(plainText, credentials)
      val decrypted = crypto.decrypt(encrypted, credentials)

      new String(decrypted, "UTF-8") mustBe "datos sensibles de prueba"
    }

    "generate different credentials each time" in {
      val cred1 = crypto.generateRandomCredentials()
      val cred2 = crypto.generateRandomCredentials()

      cred1.key must not be cred2.key
      cred1.iv  must not be cred2.iv
    }

    "generate derived credentials from password" in {
      val cred = crypto.generateDerivatedCredentials("mi-password-seguro")

      cred.key must not be empty
      cred.iv  must not be empty
    }

    "generate same derived credentials for same password" in {
      val cred1 = crypto.generateDerivatedCredentials("misma-password")
      val cred2 = crypto.generateDerivatedCredentials("misma-password")

      // Con salt fijo (actual impl) el resultado es determinístico
      cred1.key mustBe cred2.key
    }

    "generate RSA key pair" in {
      val (pub, priv) = crypto.giveRsaKeys

      pub  must not be empty
      priv must not be empty
    }

    "give a TOTP secret (non-empty Base32 string)" in {
      val secret = crypto.giveTotpSecret
      secret must not be empty
    }
  }
}
