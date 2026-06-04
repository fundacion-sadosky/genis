package matching

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import configdata.{MtConfiguration, MtRegion}

import scala.jdk.CollectionConverters.*

class MatchingModule(environment: Environment, conf: Configuration) extends AbstractModule {

  private def buildMtConfig(): MtConfiguration = {
    val regionsConf = conf.underlying.getObject("mt.regions")
    val regionNames = regionsConf.keySet().asScala.toList
    val regions = regionNames.map { key =>
      val list = regionsConf.toConfig.getIntList(key).asScala.toList.map(_.toInt)
      MtRegion(key, (list(0), list(1)))
    }
    val ignorePoints = conf.underlying.getIntList("mt.ignore").asScala.toList.map(_.toInt)
    MtConfiguration(regions, ignorePoints)
  }

  override protected def configure(): Unit = {
    val mtConfig = buildMtConfig()
    bind(classOf[MtConfiguration])
      .annotatedWith(Names.named("mtConfig"))
      .toInstance(mtConfig)

    val defaultAssignee = conf.getOptional[String]("defaultAssignee").getOrElse("")
    bind(classOf[String])
      .annotatedWith(Names.named("defaultAssignee"))
      .toInstance(defaultAssignee)

    bind(classOf[MatchingRepository]).to(classOf[MongoMatchingRepository])
    bind(classOf[MatchingService]).to(classOf[MatchingServiceImpl])
    bind(classOf[MatchingAlgorithmService]).to(classOf[MatchingAlgorithmServiceImpl])
    bind(classOf[MatchingCalculatorService]).to(classOf[MatchingCalculatorServiceImpl])
    ()
  }
}