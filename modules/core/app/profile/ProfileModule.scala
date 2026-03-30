package profile

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import com.typesafe.config.ConfigObject

class ProfileModule(environment: Environment, conf: Configuration) extends AbstractModule {

  val getLabelsSets: Profile.LabelSets = {
    val setsConf = conf.underlying.getObject("labels")
    val sets = setsConf.keySet().toArray().map(_.toString)

    sets.foldLeft(Map.empty: Profile.LabelSets) { (setPrevMap, set) =>
      val labelsConf = setsConf.toConfig().getObject(set)
      val labels = labelsConf.keySet().toArray().map(_.toString)

      val lMap = labels.foldLeft(Map.empty: Map[String, Label]) { (labelPrevMap, label) =>
        val labelConf = labelsConf.toConfig().getObject(label)
        Map(label -> Label(label, labelConf.toConfig().getString("caption"))) ++ labelPrevMap
      }
      Map(set -> lMap) ++ setPrevMap
    }
  }

  override protected def configure(): Unit = {

    bind(new TypeLiteral[Profile.LabelSets]() {}).annotatedWith(Names.named("labelsSet")).toInstance(getLabelsSets)

    bind(classOf[ProfileRepository]).to(classOf[MongoProfileRepository])

    val exportProfilesPageSize = conf.get[Int]("exportProfilesPageSize")
    bind(classOf[Int]).annotatedWith(Names.named("exportProfilesPageSize")).toInstance(exportProfilesPageSize)

    val exportProfilesPath = conf.get[String]("exportProfilesPath")
    bind(classOf[String]).annotatedWith(Names.named("exportProfilesPath")).toInstance(exportProfilesPath)

    val limsArchivesPath = conf.get[String]("limsArchivesPath")
    bind(classOf[String]).annotatedWith(Names.named("limsArchivesPath")).toInstance(limsArchivesPath)

    val generateLimsFiles = conf.getOptional[String]("generateLimsFiles").contains("true")
    bind(classOf[Boolean]).annotatedWith(Names.named("generateLimsFiles")).toInstance(generateLimsFiles)

    val labCode = conf.get[String]("laboratory.code")
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode)

    // Stub bindings for dependencies not yet migrated
    bind(classOf[connections.InterconnectionService]).to(classOf[connections.InterconnectionServiceStub])
    bind(classOf[inbox.NotificationService]).to(classOf[inbox.NoOpNotificationService])
    bind(classOf[kits.AnalysisTypeService]).to(classOf[kits.AnalysisTypeServiceStub])
    bind(classOf[kits.LocusService]).to(classOf[kits.LocusServiceStub])
    bind(classOf[kits.QualityParamsProvider]).to(classOf[kits.QualityParamsProviderStub])
    bind(classOf[matching.MatchingAlgorithmService]).to(classOf[matching.MatchingAlgorithmServiceStub])
    bind(classOf[matching.MatchingRepository]).to(classOf[matching.MatchingRepositoryStub])
    bind(classOf[matching.MatchingService]).to(classOf[matching.MatchingServiceStub])
    bind(classOf[pedigree.PedigreeService]).to(classOf[pedigree.PedigreeServiceStub])
    bind(classOf[probability.ProbabilityService]).to(classOf[probability.ProbabilityServiceStub])
    bind(classOf[profiledata.ProfileDataRepository]).to(classOf[profiledata.ProfileDataRepositoryStub])
    bind(classOf[profiledata.ProfileDataService]).to(classOf[profiledata.ProfileDataServiceStub])
    bind(classOf[trace.TraceService]).to(classOf[trace.TraceServiceStub])

    bind(classOf[ProfileService]).to(classOf[ProfileServiceImpl])
    bind(classOf[ProfileExporterService]).to(classOf[ProfileExporterServiceImpl])
    bind(classOf[LimsArchivesExporterService]).to(classOf[LimsArchivesExporterServiceImpl])
  }
}
