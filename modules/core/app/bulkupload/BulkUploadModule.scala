package bulkupload

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.api.{Configuration, Environment}
import profiledata.{ImportToProfileData, SlickImportToProfileData}

class BulkUploadModule(environment: Environment, conf: Configuration) extends AbstractModule:
  override def configure(): Unit =
    val ppGcDummy = conf.getOptional[String]("protoprofile.globalCode.dummy").getOrElse("XX-X-XXXX-")
    bind(classOf[String]).annotatedWith(Names.named("protoProfileGcDummy")).toInstance(ppGcDummy)

    val country  = conf.getOptional[String]("laboratory.country").getOrElse(conf.getOptional[String]("country").getOrElse("AR"))
    val province = conf.getOptional[String]("laboratory.province").getOrElse(conf.getOptional[String]("province").getOrElse("C"))
    bind(classOf[String]).annotatedWith(Names.named("country")).toInstance(country)
    bind(classOf[String]).annotatedWith(Names.named("province")).toInstance(province)

    bind(classOf[BulkUploadService]).to(classOf[BulkUploadServiceImpl])
    bind(classOf[ProtoProfileRepository]).to(classOf[SlickProtoProfileRepository])
    bind(classOf[ImportToProfileData]).to(classOf[SlickImportToProfileData])