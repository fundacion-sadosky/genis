package security

import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import types.TotpToken

abstract class OTPService {
  def validate(otp: TotpToken, userSecret: String): Boolean
}

// TODO: Migrar implementación real desde app/security/OTPService.scala
// Requiere dependencia: "org.jboss.aerogear" % "aerogear-otp-java" % "1.0.0"
@Singleton
class OTPServiceImpl @Inject() (
    @Named("otpInterval") val intervalSec: Int,
    @Named("otpFut") val futIntervals: Int,
    @Named("optPast") val pastIntervals: Int) extends OTPService {

  private val logger: Logger = Logger(this.getClass)

  override def validate(otp: TotpToken, userSecret: String): Boolean = {
    // STUB: siempre valida true por ahora
    logger.warn("OTPService.validate es un STUB - siempre retorna true")
    true
  }
}
