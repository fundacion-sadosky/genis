package security

import com.google.inject.AbstractModule
import com.google.inject.name.Names

import play.api.Configuration

class SecurityModule(conf: Configuration) extends AbstractModule {
  override protected def configure() {

    bind(classOf[AuthService]).to(classOf[AuthServiceImpl])

    bind(classOf[OTPService]).to(classOf[OTPServiceImpl])

    bind(classOf[CryptoService]).to(classOf[CryptoServiceImpl])
    
    val keyLength = conf.getInt("rsa.keyLength").get
    bind(classOf[Int]).annotatedWith(Names.named("keyLength")).toInstance(keyLength)
    
    val tokenExpTimes = conf.getInt("token.expirationTime").get
    bind(classOf[Int]).annotatedWith(Names.named("tokenExpTime")).toInstance(tokenExpTimes)

    val credentialsExpTime = conf.getInt("credentials.expirationTime").get
    bind(classOf[Int]).annotatedWith(Names.named("credentialsExpTime")).toInstance(credentialsExpTime)

    val otpInterval = conf.getInt("otp.interval").get
    bind(classOf[Int]).annotatedWith(Names.named("otpInterval")).toInstance(otpInterval)
    
    val otpFut = conf.getInt("otp.futures").get
    bind(classOf[Int]).annotatedWith(Names.named("otpFut")).toInstance(otpFut)
    
    val otpPast = conf.getInt("otp.pasts").get
    bind(classOf[Int]).annotatedWith(Names.named("optPast")).toInstance(otpPast)
    
    ()
  }

}
