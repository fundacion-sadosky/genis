package security

import java.util.Calendar
import org.jboss.aerogear.security.otp.Totp
import org.jboss.aerogear.security.otp.api.Clock
import javax.inject.{Inject, Named, Singleton}
import play.api.Logger
import types.TotpToken

abstract class OTPService {
  def validate(otp: TotpToken, userSecret: String): Boolean
}

@Singleton
class OTPServiceImpl @Inject() (
    @Named("otpInterval") val intervalSec: Int,
    @Named("otpFut") val futIntervals: Int,
    @Named("optPast") val pastIntervals: Int
) extends OTPService {

  private class FixedTimeClock(val currentTimeMillis: Long, val interval: Int = 30) extends Clock {
    override def getCurrentInterval(): Long = {
      (this.currentTimeMillis / 1000) / this.interval
    }
  }

  private val logger: Logger = Logger(this.getClass)

  override def validate(otp: TotpToken, userSecret: String): Boolean = {
    val now = Calendar.getInstance().getTimeInMillis

    val results = for (offset <- -pastIntervals to futIntervals) yield {
      val time = now + offset * intervalSec * 1000L
      val clock = new FixedTimeClock(time, intervalSec)
      val totp = new Totp(userSecret, clock)
      totp.verify(otp.text)
    }

    results.exists(identity)
  }
}
