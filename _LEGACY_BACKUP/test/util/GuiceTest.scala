package util

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Key
import com.google.inject.name.Names
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import com.google.inject.PrivateModule
import com.google.inject.util.Modules

abstract class Repo {}
class NormalRepo extends Repo {}
class StashedRepo @Inject() extends NormalRepo {}

abstract class Service {
  def test: String
}

class ServiceImpl @Inject() (@Named("special") repo: Repo) extends Service {
  override def test = repo.getClass.getName
}

class Ctrl @Inject() (normalService: Service, @Named("stashed") stashedService: Service) {
  def tstNormal() = normalService.test
  def tstStashed() = stashedService.test
}

class StashModule extends PrivateModule {

  @Override protected def configure() = {
    bind(classOf[Repo]).annotatedWith(Names.named("special")).to(classOf[StashedRepo])
    bind(classOf[Service]).annotatedWith(Names.named("stashed")).to(classOf[ServiceImpl])
    expose(classOf[Service]).annotatedWith(Names.named("stashed"))
  }

}

class NormalModule extends PrivateModule {
  override protected def configure() {
    bind(classOf[Repo]).annotatedWith(Names.named("special")).to(classOf[NormalRepo])
    bind(classOf[Service]).to(classOf[ServiceImpl])
    expose(classOf[Service])
    ()
  }
}

class GuiceTest extends FlatSpec with Matchers {

  "guice injector" should
    "resolve proper instance of service" in {

      val module = new AbstractModule {
        override protected def configure() {
          install(Modules.combine(new NormalModule, new StashModule))
          bind(classOf[Repo]).to(classOf[NormalRepo])
          ()
        }
      }

      val injector = Guice.createInjector(module)
      
      
      println (injector.getBinding(classOf[Repo]))
      val ctrl = injector.getInstance(classOf[Ctrl])

      ctrl.tstNormal shouldEqual "util.NormalRepo"
      ctrl.tstStashed shouldEqual "util.StashedRepo"

      val repo = injector.getInstance(classOf[Repo])
    }

}