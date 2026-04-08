package audit

import javax.inject.{Inject, Named, Provider}

import org.apache.pekko.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}

class SignerServiceProvider @Inject()(
  actorSystem:   ActorSystem,
  logRepository: OperationLogRepository,
  @Named("randomAlg") randomAlg: String,
  @Named("lotSize")   lotSize:   Int
) extends Provider[PEOSignerService] {
  override def get(): PEOSignerService =
    new AkkaPEOSignerService(actorSystem, logRepository, randomAlg, lotSize)
}

class OperationLogModule(environment: Environment, conf: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[OperationLogService]).to(classOf[PeoOperationLogService])
    bind(classOf[OperationLogRepository]).to(classOf[SlickOperationLogRepository])

    bind(classOf[PEOSignerService])
      .annotatedWith(Names.named("logEntrySigner"))
      .toProvider(classOf[SignerServiceProvider])
    bind(classOf[PEOSignerService])
      .annotatedWith(Names.named("profileSigner"))
      .toProvider(classOf[SignerServiceProvider])

    val hmacAlg   = conf.getOptional[String]("operationLog.hmac.algorithm").getOrElse("HmacSHA256")
    val randomAlg = conf.getOptional[String]("operationLog.random.algorithm").getOrElse("SHA1PRNG")
    val lotSize   = conf.getOptional[Int]("operationLog.lot.size").getOrElse(10000)
    val chunkSize = conf.getOptional[Int]("operationLog.chunk.size").getOrElse(100)
    val buildNo   = conf.getOptional[String]("operationLog.build.number").getOrElse("develop")

    bind(classOf[String]).annotatedWith(Names.named("hmacAlg")).toInstance(hmacAlg)
    bind(classOf[String]).annotatedWith(Names.named("randomAlg")).toInstance(randomAlg)
    bind(classOf[String]).annotatedWith(Names.named("buildNo")).toInstance(buildNo)
    bind(classOf[Int]).annotatedWith(Names.named("lotSize")).toInstance(lotSize)
    bind(classOf[Int]).annotatedWith(Names.named("chunkSize")).toInstance(chunkSize)

    ()
  }
}
