package security

import com.google.inject.AbstractModule
import com.google.inject.name.Names

import play.api.{Configuration, Environment}
import services.{CacheService, PlayCacheService}

class SecurityModule(environment: Environment, conf: Configuration) extends AbstractModule {
  override protected def configure(): Unit = {

    bind(classOf[AuthService]).to(classOf[AuthServiceImpl])

    bind(classOf[OTPService]).to(classOf[OTPServiceImpl])

    bind(classOf[CryptoService]).to(classOf[CryptoServiceImpl])

    // CacheService real
    bind(classOf[CacheService]).to(classOf[PlayCacheService])

    // RoleService real (UserRepository se bindea en UsersModule)
    bind(classOf[RoleService]).to(classOf[RoleServiceImpl])
    bind(classOf[InferiorInstanceRepository]).to(classOf[InferiorInstanceRepositoryStub])
    bind(classOf[ConnectionRepository]).to(classOf[ConnectionRepositoryStub])

    val keyLength = conf.get[Int]("rsa.keyLength")
    bind(classOf[Int]).annotatedWith(Names.named("keyLength")).toInstance(keyLength)

    val tokenExpTimes = conf.get[Int]("token.expirationTime")
    bind(classOf[Int]).annotatedWith(Names.named("tokenExpTime")).toInstance(tokenExpTimes)

    val credentialsExpTime = conf.get[Int]("credentials.expirationTime")
    bind(classOf[Int]).annotatedWith(Names.named("credentialsExpTime")).toInstance(credentialsExpTime)

    val otpInterval = conf.get[Int]("otp.interval")
    bind(classOf[Int]).annotatedWith(Names.named("otpInterval")).toInstance(otpInterval)

    val otpFut = conf.get[Int]("otp.futures")
    bind(classOf[Int]).annotatedWith(Names.named("otpFut")).toInstance(otpFut)

    val otpPast = conf.get[Int]("otp.pasts")
    bind(classOf[Int]).annotatedWith(Names.named("optPast")).toInstance(otpPast)
  }

}
