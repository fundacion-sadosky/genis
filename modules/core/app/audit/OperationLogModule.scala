package audit

import java.util.jar.{Attributes, Manifest}
import javax.inject.{Inject, Named, Provider, Singleton}

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

import org.apache.pekko.actor.ActorSystem
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.JdbcBackend.Database

class SignerServiceProvider @Inject()(
  actorSystem:   ActorSystem,
  logRepository: OperationLogRepository,
  @Named("randomAlg") randomAlg: String,
  @Named("lotSize")   lotSize:   Int
) extends Provider[PEOSignerService] {
  override def get(): PEOSignerService =
    new AkkaPEOSignerService(actorSystem, logRepository, randomAlg, lotSize)
}

@Singleton
class LogDbProvider @Inject()(lifecycle: ApplicationLifecycle) extends Provider[Database] {
  private lazy val db = Database.forConfig("slick.dbs.logDb.db")
  lifecycle.addStopHook(() => Future.successful(db.close()))
  override def get(): Database = db
}

class OperationLogModule(environment: Environment, conf: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[Database])
      .annotatedWith(Names.named("logDb"))
      .toProvider(classOf[LogDbProvider])
      .asEagerSingleton()

    bind(classOf[OperationLogService]).to(classOf[PeoOperationLogService])
    bind(classOf[OperationLogRepository]).to(classOf[SlickOperationLogRepository])

    bind(classOf[PEOSignerService])
      .annotatedWith(Names.named("logEntrySigner"))
      .toProvider(classOf[SignerServiceProvider])
    bind(classOf[PEOSignerService])
      .annotatedWith(Names.named("profileSigner"))
      .toProvider(classOf[SignerServiceProvider])

    // Legacy flat keys are the source of truth; operationLog.* serves as fallback for new installs.
    // Note: `hmac.algorithm` is intentionally NOT read — Signature hardcodes HmacSHA256
    // (matches legacy and would invalidate the entire signature chain if changed mid-flight).
    val randomAlg = conf.getOptional[String]("random.algorithm")
      .orElse(conf.getOptional[String]("operationLog.random.algorithm"))
      .getOrElse("SHA1PRNG")
    val lotSize   = conf.getOptional[Int]("lot.size")
      .orElse(conf.getOptional[Int]("operationLog.lot.size"))
      .getOrElse(10000)
    val chunkSize = conf.getOptional[Int]("chunk.size")
      .orElse(conf.getOptional[Int]("operationLog.chunk.size"))
      .getOrElse(100)

    // Build number is sourced from the GENis jar's MANIFEST.MF (Git-Head-Rev attribute)
    // to keep parity with legacy PdgModule/LogginFilter. Config + env override available
    // as fallback for dev runs where no jar manifest is present.
    val buildNo = OperationLogModule.readBuildNoFromManifest()
      .orElse(conf.getOptional[String]("operationLog.build.number"))
      .getOrElse("develop")

    bind(classOf[String]).annotatedWith(Names.named("randomAlg")).toInstance(randomAlg)
    bind(classOf[String]).annotatedWith(Names.named("buildNo")).toInstance(buildNo)
    bind(classOf[Int]).annotatedWith(Names.named("lotSize")).toInstance(lotSize)
    bind(classOf[Int]).annotatedWith(Names.named("chunkSize")).toInstance(chunkSize)

    ()
  }
}

object OperationLogModule {

  /** Reads MANIFEST.MF attributes from the GENis jar — mirrors legacy PdgModule.manifest. */
  private def genisManifest(): Map[String, String] = {
    val resources = this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF").asScala
    resources
      .map(r => new Manifest(r.openStream()))
      .find { m =>
        val atts = m.getMainAttributes
        atts.getValue(Attributes.Name.IMPLEMENTATION_VENDOR) == "ar.org.fundacionsadosky" &&
          atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE)  == "genis"
      }
      .map(_.getMainAttributes.asScala.toMap.collect {
        case (k, v) => k.toString -> v.toString
      })
      .getOrElse(Map.empty[String, String])
  }

  /** Returns the Git commit hash from MANIFEST.MF, if present. */
  def readBuildNoFromManifest(): Option[String] =
    genisManifest().get("Git-Head-Rev")
}
