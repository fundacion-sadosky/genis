package profile

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import com.google.inject.name.Names
import play.api.Configuration
import com.typesafe.config.ConfigObject
import com.typesafe.config.impl.ConfigString

class ProfileModule(conf: Configuration) extends AbstractModule {
  
  val getLabelsSets = {
    val setsConf = conf.getObject("labels").get
    val sets = setsConf.keySet().toArray().map(_.toString)

    sets.foldLeft(Map.empty: Profile.LabelSets)({ (setPrevMap, set) =>

      val labelsConf = setsConf.toConfig().getObject(set)
      val labels = labelsConf.keySet().toArray().map(_.toString)

      val lMap = labels.foldLeft(Map.empty: Map[String, Label]) { (labelPrevMap, label) =>
        val labelConf = labelsConf.toConfig().getObject(label)
        Map(label -> Label(label, labelConf.toConfig().getString("caption"))) ++ labelPrevMap
      }
      Map(set -> lMap) ++ setPrevMap
    })

  }
  
  override protected def configure() {
      
    bind(new TypeLiteral[Profile.LabelSets](){}).annotatedWith(Names.named("labelsSet")).toInstance(getLabelsSets);
    
//    bind(classOf[ProfileRepository]).to(classOf[MongoProfileRepository])

    // Temporal bind for couchdb
    bind(classOf[CouchProfileRepository]).to(classOf[CouchProfileRepository])
    bind(classOf[ProfileRepository]).to(classOf[MiddleProfileRepository])

    val exportProfilesPageSize = conf.getInt("exportProfilesPageSize").get
    bind(classOf[Int]).annotatedWith(Names.named("exportProfilesPageSize")).toInstance(exportProfilesPageSize)

    val exportProfilesPath = conf.getString("exportProfilesPath").get
    bind(classOf[String]).annotatedWith(Names.named("exportProfilesPath")).toInstance(exportProfilesPath)
    val limsArchivesPath = conf.getString("limsArchivesPath").get
    bind(classOf[String]).annotatedWith(Names.named("limsArchivesPath")).toInstance(limsArchivesPath)
    val generateLimsFiles = conf.getString("generateLimsFiles").contains("true")
    bind(classOf[Boolean]).annotatedWith(Names.named("generateLimsFiles")).toInstance(generateLimsFiles)
    val labCode = conf.getString("laboratory.code").get
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode);

    bind(classOf[ProfileService]).to(classOf[ProfileServiceImpl])
    bind(classOf[ProfileExporterService]).to(classOf[ProfileExporterServiceImpl])
    bind(classOf[LimsArchivesExporterService]).to(classOf[LimsArchivesExporterServiceImpl])
  }

}
