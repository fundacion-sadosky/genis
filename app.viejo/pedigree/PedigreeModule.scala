package pedigree

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import configdata.{MtConfiguration, MtRegion}
import play.api.Configuration
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.Play.current
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import play.api.libs.ws._

class PedigreeModule(conf: Configuration) extends AbstractModule {
  val getMtConfig = {
    import collection.JavaConversions._

    val mtRegionsConf = conf.getObject("mt.regions").get
    val regionNames = mtRegionsConf.keySet().toArray().map(_.toString)
    var regions = List.empty[MtRegion]
    regionNames.foreach(key => {
      val list = mtRegionsConf.toConfig.getIntList(key).toList.map(_.toInt)
      regions :+= MtRegion(key, (list(0), list(1)))
    })
    val ignorePoints = conf.getIntList("mt.ignore").get.toList.map(_.toInt)
    MtConfiguration(regions, ignorePoints)
  }

  override protected def configure() {

    val exportProfilesPath = conf.getString("exportProfilesPath").get
    bind(classOf[String]).annotatedWith(Names.named("exportProfilesPath")).toInstance(exportProfilesPath)

    bind(classOf[MtConfiguration]).annotatedWith(Names.named("mtConfig")).toInstance(getMtConfig)

    bind(classOf[PedigreeService]).to(classOf[PedigreeServiceImpl])

    bind(classOf[PedigreeScenarioService]).to(classOf[PedigreeScenarioServiceImpl])

    bind(classOf[PedigreeMatchesService]).to(classOf[PedigreeMatchesServiceImpl])

    bind(classOf[PedigreeDataRepository]).to(classOf[SlickPedigreeDataRepository])

    bind(classOf[PedigreeRepository]).to(classOf[MongoPedigreeRepository])

    bind(classOf[BayesianNetworkService]).to(classOf[BayesianNetworkServiceImpl])

    bind(classOf[PedigreeScenarioRepository]).to(classOf[MongoPedigreeScenarioRepository])

    bind(classOf[PedigreeMatchesRepository]).to(classOf[MongoPedigreeMatchesRepository])

    bind(classOf[PedigreeGenotypificationService]).to(classOf[PedigreeGenotypificationServiceImpl])

    bind(classOf[PedigreeSparkMatcher]).to(classOf[PedigreeSparkMatcherImpl])

    bind(classOf[PedigreeGenotypificationRepository]).to(classOf[MongoPedigreeGenotypificationRepository])

    val defaultMutationRateI = conf.getString("mt.defaultMutationRateI").get
    val defaultMutationRateF = conf.getString("mt.defaultMutationRateF").get
    val defaultMutationRateM = conf.getString("mt.defaultMutationRateM").get
    val defaultMutationRange = conf.getString("mt.defaultMutationRange").get
    val defaultMutationRateMicrovariant = conf.getString("mt.defaultMutationRateMicrovariant").get
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateI")).toInstance(defaultMutationRateI)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateF")).toInstance(defaultMutationRateF)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateM")).toInstance(defaultMutationRateM)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRange")).toInstance(defaultMutationRange)
    bind(classOf[String]).annotatedWith(Names.named("defaultMutationRateMicrovariant")).toInstance(defaultMutationRateMicrovariant)

    bind(classOf[MutationService]).to(classOf[MutationServiceImpl])

    bind(classOf[MutationRepository]).to(classOf[SlickMutationRepository])

    bind(classOf[PedCheckService]).to(classOf[PedCheckServiceImpl])

    bind(classOf[PedCheckRepository]).to(classOf[SlickPedCheckRepository])

    ()
  }
  
}