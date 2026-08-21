package probability

import com.google.inject.{AbstractModule, TypeLiteral}
import com.google.inject.name.Names
import play.api.Configuration

class ProbabilityModule(conf: Configuration) extends AbstractModule {

  val getCalculationTypes = {
    val calculationConf = conf.getObject("calculation").get
    val calculationTypes = calculationConf.keySet().toArray().map(_.toString)
    var map = Map.empty[String,String]
    calculationTypes.foreach(key => map += (key -> calculationConf.toConfig().getString(key)))
    map
  }

  override protected def configure() {

    bind(new TypeLiteral[Map[String, String]]() {}).annotatedWith(Names.named("calculationTypes")).toInstance(getCalculationTypes)
    bind(classOf[ProbabilityService]).to(classOf[ProbabilityServiceImpl])
    bind(classOf[CalculationTypeService]).to(classOf[CalculationTypeServiceImpl])
    ()
  }

}
