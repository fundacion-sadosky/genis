package unit.security

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.jboss.aerogear.security.otp.Totp
import security.{CryptoServiceImpl, OTPServiceImpl}
import types.TotpToken

class OTPServiceTest extends AnyWordSpec with Matchers {

  // interval=30s, 1 intervalo de tolerancia en cada dirección
  val otpService = new OTPServiceImpl(intervalSec = 30, futIntervals = 1, pastIntervals = 1)
  val crypto     = new CryptoServiceImpl(keyLength = 2048)

  "OTPServiceImpl" must {

    "validate a correct current OTP" in {
      val secret  = crypto.giveTotpSecret
      val totp    = new Totp(secret)
      val token   = TotpToken(totp.now())

      otpService.validate(token, secret) mustBe true
    }

    "reject an invalid OTP" in {
      val secret = crypto.giveTotpSecret
      val bad    = TotpToken("000000")

      // Extremadamente improbable que 000000 sea el OTP correcto ahora
      otpService.validate(bad, secret) mustBe false
    }

    "reject OTP with wrong secret" in {
      val secret1 = crypto.giveTotpSecret
      val secret2 = crypto.giveTotpSecret
      val totp    = new Totp(secret1)
      val token   = TotpToken(totp.now())

      otpService.validate(token, secret2) mustBe false
    }
  }
}
