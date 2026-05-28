package fixtures

import com.google.inject.AbstractModule
import matching.{MatchingProcessStatus, MatchingProcessStatusImpl}
import pedigree.*
import play.api.{Configuration, Environment}

/** Replaces pedigree.PedigreeModule and pedigree.MutationModule in integration tests.
 *  Binds no-op stubs for all pedigree services so the router can instantiate
 *  PedigreesController without requiring a real database or Bayesian engine. */
class StubPedigreeModule(env: Environment, conf: Configuration) extends AbstractModule:
  override def configure(): Unit =
    bind(classOf[BayesianNetworkService]).to(classOf[BayesianNetworkServiceStub])
    bind(classOf[PedigreeService]).to(classOf[PedigreeServiceStub])
    bind(classOf[PedigreeMatchesService]).to(classOf[PedigreeMatchesServiceStub])
    bind(classOf[PedigreeScenarioService]).to(classOf[PedigreeScenarioServiceStub])
    bind(classOf[PedigreeGenotypificationService]).to(classOf[PedigreeGenotypificationServiceStub])
    bind(classOf[PedCheckService]).to(classOf[PedCheckServiceStub])
    bind(classOf[PedigreeMatcher]).to(classOf[PedigreeMatcherStub])
    bind(classOf[MutationService]).to(classOf[MutationServiceStub])
    bind(classOf[profiledata.ProfileDataService]).to(classOf[profiledata.ProfileDataServiceStub])
    bind(classOf[MatchingProcessStatus]).to(classOf[MatchingProcessStatusImpl])
    bind(classOf[search.FullTextSearchService]).to(classOf[search.FullTextSearchServiceStub])
    ()
