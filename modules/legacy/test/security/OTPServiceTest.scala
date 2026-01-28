package security

import org.scalatest.FlatSpec
import org.jboss.aerogear.security.otp.api.Base32
import org.jboss.aerogear.security.otp.Totp
import types.TotpToken

class OTPServiceTest extends FlatSpec {
  
  "OTPService" must 
    "validate a current pass" in {
      
      val otpService = new OTPServiceImpl(30,1,1)
      
      val secret = Base32.random()
      
      val otp = TotpToken(new Totp(secret).now())
      
      val result = otpService.validate(otp, secret)
      
      assert(result === true)
    }
  
}