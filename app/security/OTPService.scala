package security

import java.util.Calendar
import org.jboss.aerogear.security.otp.Totp
import javax.inject.Singleton
import play.api.Logger
import types.TotpToken
import javax.inject.Inject
import javax.inject.Named
import org.jboss.aerogear.security.otp.api.Clock

abstract class OTPService {
  def validate(otp: TotpToken, userSecret: String): Boolean
}

@Singleton
class OTPServiceImpl @Inject() (
    @Named("otpInterval") val intervalSec: Int,
    @Named("otpFut") val futIntervals: Int,
    @Named("optPast") val pastIntervals: Int) extends OTPService {

  private class FixedTimeClock(val currentTimeMillis: Long, val interval: Int = 30) extends Clock {

    override def getCurrentInterval(): Long = {
      (this.currentTimeMillis / 1000) / this.interval;
    }

  }

  val logger: Logger = Logger(this.getClass())

  override def validate(otp: TotpToken, userSecret: String): Boolean = {
    val now = Calendar.getInstance().getTimeInMillis

    val rr = for (a <- -pastIntervals to futIntervals) yield {
      val time = now + a * intervalSec * 1000L
      val clock = new FixedTimeClock(time, intervalSec)
      val totp = new Totp(userSecret, clock)
      totp.verify(otp.text)
    }

    rr.exists { b => b }
  }
}