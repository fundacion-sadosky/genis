package audit

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.Configuration
import play.api.libs.concurrent.Akka
import akka.actor.ActorSystem
import play.api.Application
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Named

class SignerServiceProvider @Inject() (
    akkaSystem: ActorSystem,
    logRepository: OperationLogRepository,
    @Named("randomAlg") randomAlg: String,
    @Named("lotSize") lotSize: Int) extends Provider[PEOSignerService] {

  override def get(): PEOSignerService = {
    new AkkaPEOSignerService(akkaSystem, logRepository, randomAlg, lotSize)
  }

}

class OperationLogModule(conf: Configuration, actorSystem: ActorSystem) extends AbstractModule {
  override protected def configure() {

    bind(classOf[PEOSignerService]).annotatedWith(Names.named("logEntrySigner")).toProvider(classOf[SignerServiceProvider])
    bind(classOf[PEOSignerService]).annotatedWith(Names.named("profileSigner")).toProvider(classOf[SignerServiceProvider])

    val hmacAlg = conf.getString("hmac.algorithm").get
    bind(classOf[String]).annotatedWith(Names.named("hmacAlg")).toInstance(hmacAlg);

    val randomAlg = conf.getString("random.algorithm").get
    bind(classOf[String]).annotatedWith(Names.named("randomAlg")).toInstance(randomAlg);

    val bufferSize = conf.getInt("buffer.size").get
    bind(classOf[Int]).annotatedWith(Names.named("bufferSize")).toInstance(bufferSize);

    val lotSize = conf.getInt("lot.size").get
    bind(classOf[Int]).annotatedWith(Names.named("lotSize")).toInstance(lotSize);

    val chunkSize = conf.getInt("chunk.size").get
    bind(classOf[Int]).annotatedWith(Names.named("chunkSize")).toInstance(chunkSize);

    bind(classOf[OperationLogService]).to(classOf[PeoOperationLogService])

    bind(classOf[OperationLogRepository]).to(classOf[SlickOperationLogRepository])

    ()
  }

}
