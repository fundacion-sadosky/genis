package fixtures

import com.google.inject.AbstractModule

import kits.{AnalysisTypeService, AnalysisTypeServiceStub, LocusService, StrKitService}
import probability.{CalculationTypeService, ProbabilityService}

/** Reemplaza a kits.StrKitModule en los controller tests: bindea a stubs los
 *  servicios del dominio kits que el router necesita (StrKitService, LocusService,
 *  AnalysisTypeService). Permite `.disable[StrKitModule]` (cuyo impl real pega a la
 *  DB) sin dejar esos servicios sin binding, que rompe la creación del injector.
 *
 *  Punto único de mantenimiento: si el router gana una dependencia nueva del dominio
 *  kits, se agrega acá una sola vez en lugar de en cada test. */
class StubStrKitModule extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[StrKitService]).toInstance(new StubStrKitService)
    bind(classOf[LocusService]).toInstance(new LocusServiceStub)
    bind(classOf[AnalysisTypeService]).toInstance(new AnalysisTypeServiceStub)

/** Reemplaza a probability.ProbabilityModule en los controller tests: bindea a
 *  stubs ProbabilityService y CalculationTypeService (ambos requeridos por el
 *  grafo eager del router vía ScenarioService/BayesianNetworkService/etc.). */
class StubProbabilityModule extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[ProbabilityService]).toInstance(new StubProbabilityService)
    bind(classOf[CalculationTypeService]).toInstance(new CalculationTypeServiceStub)
