package pedigree

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import configdata.{MtConfiguration, MtRegion}
import matching.{MatchingProcessStatus, MatchingProcessStatusImpl}
import play.api.{Configuration, Environment}

import scala.jdk.CollectionConverters.*

class PedigreeModule(env: Environment, conf: Configuration) extends AbstractModule:

  private def getMtConfig: MtConfiguration =
    val mtRegionsConf = conf.underlying.getObject("mt.regions")
    val regionNames   = mtRegionsConf.keySet().asScala.toList
    val regions = regionNames.map { key =>
      val list = mtRegionsConf.toConfig.getIntList(key).asScala.toList.map(_.toInt)
      MtRegion(key, (list(0), list(1)))
    }
    val ignorePoints = conf.underlying.getIntList("mt.ignore").asScala.toList.map(_.toInt)
    MtConfiguration(regions, ignorePoints)

  override def configure(): Unit =
    bind(classOf[MtConfiguration]).annotatedWith(Names.named("mtConfig")).toInstance(getMtConfig)
    bind(classOf[PedigreeRepository]).to(classOf[MongoPedigreeRepository])
    bind(classOf[PedigreeDataRepository]).to(classOf[SlickPedigreeDataRepository])
    bind(classOf[PedigreeMatchesRepository]).to(classOf[MongoPedigreeMatchesRepository])
    bind(classOf[PedigreeGenotypificationRepository]).to(classOf[MongoPedigreeGenotypificationRepository])
    bind(classOf[PedigreeScenarioRepository]).to(classOf[MongoPedigreeScenarioRepository])
    bind(classOf[BayesianNetworkService]).to(classOf[BayesianNetworkServiceImpl])
    bind(classOf[PedigreeMatcher]).to(classOf[PedigreeMatcherImpl])
    bind(classOf[PedigreeMatchesService]).to(classOf[PedigreeMatchesServiceImpl])
    bind(classOf[MatchingProcessStatus]).to(classOf[MatchingProcessStatusImpl])
    bind(classOf[PedigreeScenarioService]).to(classOf[PedigreeScenarioServiceImpl])
    bind(classOf[PedigreeGenotypificationService]).to(classOf[PedigreeGenotypificationServiceImpl])
    bind(classOf[PedCheckService]).to(classOf[PedCheckServiceImpl])
    bind(classOf[PedCheckRepository]).to(classOf[SlickPedCheckRepository])
    bind(classOf[PedigreeService]).to(classOf[PedigreeServiceImpl])
    bind(classOf[search.FullTextSearchService]).to(classOf[search.FullTextSearchServiceStub])
    bind(classOf[String]).annotatedWith(Names.named("exportProfilesPath")).toInstance("")
