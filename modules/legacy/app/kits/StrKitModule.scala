package kits

import com.google.inject.AbstractModule

class StrKitModule() extends AbstractModule {
  override protected def configure(): Unit = {

    bind(classOf[StrKitRepository]).to(classOf[SlickKitDataRepository])
    bind(classOf[StrKitService]).to(classOf[StrKitServiceImpl])

    bind(classOf[LocusService]).to(classOf[LocusServiceImpl])
    bind(classOf[LocusRepository]).to(classOf[SlickLocusRepository])

    bind(classOf[AnalysisTypeRepository]).to(classOf[SlickAnalysisTypeRepository])
    bind(classOf[AnalysisTypeService]).to(classOf[AnalysisTypeServiceImpl])

    ()
  }
}