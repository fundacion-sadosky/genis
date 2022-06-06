package kits

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration._

class AnalysisTypeRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  "An AnalysisTypeRepository" must {
    "list all analysis types" in {
      val repository = new SlickAnalysisTypeRepository()

      val result = Await.result(repository.list(), duration)

      result.size mustBe 4
    }

    "get analysis type by name" in {
      val name = "Autosomal"

      val repository = new SlickAnalysisTypeRepository()

      val result = Await.result(repository.getByName(name), duration)

      result.isDefined mustBe true
      result.get.name mustBe name
    }

    "get none analysis type by name" in {
      val repository = new SlickAnalysisTypeRepository()

      val result = Await.result(repository.getByName("INVALID"), duration)

      result mustBe None
    }

    "get analysis type by id" in {
      val id = 1

      val repository = new SlickAnalysisTypeRepository()

      val result = Await.result(repository.getById(id), duration)

      result.isDefined mustBe true
      result.get.id mustBe id
    }

    "get none analysis type by id" in {
      val repository = new SlickAnalysisTypeRepository()

      val result = Await.result(repository.getById(99), duration)

      result mustBe None
    }
  }

}
