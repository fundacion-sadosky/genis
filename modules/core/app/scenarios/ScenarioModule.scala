package scenarios

import com.google.inject.AbstractModule

class ScenarioModule extends AbstractModule:
  override protected def configure(): Unit =
    bind(classOf[ScenarioService]).to(classOf[ScenarioServiceImpl])
    bind(classOf[ScenarioRepository]).to(classOf[MongoScenarioRepository])
