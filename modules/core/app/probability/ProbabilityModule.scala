package probability

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.name.Names
import play.api.{Configuration, Environment}

class ProbabilityModule(environment: Environment, conf: Configuration) extends AbstractModule {

  val getCalculationTypes: Map[String, String] = {
    val calculationConf = conf.underlying.getObject("calculation")
    val calculationTypes = calculationConf.keySet().toArray().map(_.toString)
    calculationTypes.foldLeft(Map.empty[String, String]) { (map, key) =>
      map + (key -> calculationConf.toConfig().getString(key))
    }
  }

  override protected def configure(): Unit = {
    bind(new TypeLiteral[Map[String, String]]() {})
      .annotatedWith(Names.named("calculationTypes"))
      .toInstance(getCalculationTypes)

    bind(classOf[ProbabilityService]).to(classOf[ProbabilityServiceImpl])
    bind(classOf[CalculationTypeService]).to(classOf[CalculationTypeServiceImpl])
    ()
  }
}
