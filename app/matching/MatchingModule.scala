package matching

import com.google.inject.AbstractModule
import play.api.Configuration
import com.google.inject.name.Names
import configdata.{MtConfiguration, MtRegion}

class MatchingModule(conf: Configuration) extends AbstractModule {
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
    val mongoUri = conf.getString("mongodb.uri").get
    bind(classOf[String]).annotatedWith(Names.named("mongoUri")).toInstance(mongoUri)
    bind(classOf[MtConfiguration]).annotatedWith(Names.named("mtConfig")).toInstance(getMtConfig)
    bind(classOf[MatchingRepository]).to(classOf[MongoMatchingRepository])
    bind(classOf[MatchingProcessStatus]).to(classOf[MatchingProcessStatusImpl])
    val defaultAssignee = conf.getString("defaultAssignee").get
    bind(classOf[String]).annotatedWith(Names.named("defaultAssignee")).toInstance(defaultAssignee)
    val updateLr = conf.getString("updateLr").contains("true")
    bind(classOf[Boolean]).annotatedWith(Names.named("updateLr")).toInstance(updateLr)
/*
    val limsArchivesPath = conf.getString("limsArchivesPath").get
    bind(classOf[String]).annotatedWith(Names.named("limsArchivesPath")).toInstance(limsArchivesPath)
    val generateLimsFiles = conf.getString("generateLimsFiles").contains("true")
    bind(classOf[Boolean]).annotatedWith(Names.named("generateLimsFiles")).toInstance(generateLimsFiles)
*/
    bind(classOf[MatchingService]).to(classOf[MatchingServiceSparkImpl])
    bind(classOf[MatchingCalculatorService]).to(classOf[MatchingCalculatorServiceImpl])
    ()
  }
  
}