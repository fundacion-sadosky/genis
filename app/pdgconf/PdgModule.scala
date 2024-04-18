package pdgconf

import java.util.jar.{Attributes, Manifest}

import connections.InterconnectionModule
import akka.actor.ActorSystem
import audit.OperationLogModule
import bulkupload.BulkUploadModule
import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.name.Names
import configdata.ConfigDataModule
import disclaimer.DisclaimerModule
import inbox.NotificationModule
import kits.StrKitModule
import laboratories.LaboratoryModule
import matching.MatchingModule
import motive.MotiveModule
import pedigree.PedigreeModule
import play.api.Application
import play.api.libs.concurrent.Akka
import probability.ProbabilityModule
import profile.ProfileModule
import profiledata.ProfileDataModule
import reporting.ReportingModule
import scenarios.ScenarioModule
import search.SearchModule
import security.SecurityModule
import services.{CacheService, PlayCacheService}
import stats.StatsModule
import trace.TraceModule
import types.Mode
import user.UsersModule

import scala.collection.JavaConversions.{asScalaSet, enumerationAsScalaIterator}

class PdgModule(app: Application) extends AbstractModule {
  override protected def configure() {

    bind(new TypeLiteral[Map[String, String]]() {}).annotatedWith(Names.named("genisManifest")).toInstance(manifest)

    val mode: Mode = app.configuration.getString("mode") match {
      case Some("prod") => Mode.Prod
      case Some("sandbox") => Mode.Sandbox
      case Some("sandboxAzul") => Mode.SandboxAzul
      case Some("sandboxVerde") => Mode.SandboxVerde
      case Some("sandboxAmarilla") => Mode.SandboxAmarillo
      case Some(_) => throw new RuntimeException("esta mal definida la property en la conf " )
      case None => throw new RuntimeException("no esta definida la property en la conf")
    }

    bind(classOf[Mode]).toInstance(mode)

    bind(classOf[Application]).toInstance(app)

    bind(classOf[ActorSystem]).toInstance(Akka.system(app))

    bind(classOf[BodyDecryptFilter])

    bind(classOf[LogginFilter])

    bind(classOf[CacheService]).to(classOf[PlayCacheService])

    install(new ProfileModule(app.configuration))

    install(new MatchingModule(app.configuration))
    

    install(new ProbabilityModule(app.configuration))

    install(new ProfileDataModule)

    install(new ConfigDataModule(app.configuration))

    install(new NotificationModule)

    install(new StatsModule)

    install(new SearchModule)

    install(new LaboratoryModule(app.configuration))

    install(new BulkUploadModule(app.configuration))

    install(new SecurityModule(app.configuration.getConfig("security").get))

    install(new OperationLogModule(app.configuration.getConfig("operationLog").get, Akka.system(app)))
    install(new UsersModule(app, app.configuration.getConfig("ldap.default").get))

    install(new PedigreeModule(app.configuration))

    install(new ScenarioModule)

    install(new StrKitModule)

    install(new TraceModule)


    install(new InterconnectionModule(app.configuration.getConfig("instanceInterconnection").get,app.configuration))

    install(new DisclaimerModule)

    install(new MotiveModule)

    install(new ReportingModule)

    ()
  }

  private val isGenisManifest = (manifest: Manifest) => {
    val atts = manifest.getMainAttributes
    atts.getValue(Attributes.Name.IMPLEMENTATION_VENDOR) == "ar.org.fundacionsadosky" &&
      atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE) == "genis"
  }

  private val manifest = {
    this.getClass.getClassLoader.getResources("META-INF/MANIFEST.MF")
      .map { r => new Manifest(r.openStream()) }
      .find(isGenisManifest)
      .fold { Map.empty[String, String] } { manifest =>
        manifest.getMainAttributes
          .entrySet
          .map { case entry => (entry.getKey.toString() -> entry.getValue.toString()) }
          .toMap
      }
  }

}
