package bulkupload

import com.google.inject.AbstractModule
import com.google.inject.name.Names

import play.api.Configuration

class BulkUploadModule(conf: Configuration) extends AbstractModule {
  override protected def configure() {

    val labCode = conf.getString("laboratory.code").get
    bind(classOf[String]).annotatedWith(Names.named("labCode")).toInstance(labCode);

    val ppgcd = conf.getString("protoprofile.globalCode.dummy").get
    bind(classOf[String]).annotatedWith(Names.named("protoProfileGcDummy")).toInstance(ppgcd)
    
    bind(classOf[ProtoProfileRepository]).to(classOf[SlickProtoProfileRepository])
    bind(classOf[BulkUploadService]).to(classOf[BulkUploadServiceImpl])
    ()
  }

}