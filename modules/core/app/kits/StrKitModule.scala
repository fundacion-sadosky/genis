package kits

import com.google.inject.AbstractModule
import pedigree.{MutationService, MutationServiceStub}

class StrKitModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[StrKitRepository]).to(classOf[StrKitRepositoryImpl])
    bind(classOf[StrKitService]).to(classOf[StrKitServiceImpl])
    bind(classOf[LocusRepository]).to(classOf[LocusRepositoryImpl])
    bind(classOf[LocusService]).to(classOf[LocusServiceImpl])
    bind(classOf[MutationService]).to(classOf[MutationServiceStub])
    // TODO: Migrar AnalysisType cuando se necesite (legacy: kits/AnalysisTypeRepository.scala, kits/AnalysisTypeService.scala)
    //  bind(classOf[AnalysisTypeRepository]).to(classOf[...])
    //  bind(classOf[AnalysisTypeService]).to(classOf[...])
  }
}
