package bulkupload

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import profiledata.{ImportToProfileData, SlickImportToProfileData}

class BulkUploadModule(environment: Environment, configuration: Configuration) extends AbstractModule:
  override def configure(): Unit =
    val labCode = configuration.get[String]("laboratory.code")
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode)

    val country = configuration.get[String]("laboratory.country")
    bind(classOf[String]).annotatedWith(Names.named("country")).toInstance(country)

    val province = configuration.get[String]("laboratory.province")
    bind(classOf[String]).annotatedWith(Names.named("province")).toInstance(province)

    val ppgcd = configuration.get[String]("protoprofile.globalCode.dummy")
    bind(classOf[String]).annotatedWith(Names.named("protoProfileGcDummy")).toInstance(ppgcd)

    bind(classOf[ProtoProfileRepository]).to(classOf[SlickProtoProfileRepository])
    bind(classOf[BulkUploadService]).to(classOf[BulkUploadServiceImpl])
    bind(classOf[ImportToProfileData]).to(classOf[SlickImportToProfileData])
