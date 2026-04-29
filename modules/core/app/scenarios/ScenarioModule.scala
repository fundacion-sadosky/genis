package scenarios

import com.google.inject.AbstractModule

class ScenarioModule extends AbstractModule:
  override protected def configure(): Unit =
    bind(classOf[ScenarioService]).to(classOf[ScenarioServiceStub])
    bind(classOf[ScenarioRepository]).to(classOf[ScenarioRepositoryStub])
