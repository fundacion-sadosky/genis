package pedigrees

import org.bson.types.ObjectId
import pedigree._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import scenarios.ScenarioStatus
import specs.PdgSpec
import types.{MongoId, SampleCode, Sex}

import scala.concurrent.Await
import scala.concurrent.duration._
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor

class PedigreeScenarioRepositoryTest extends PdgSpec {
  val pedigreeScenarios = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("pedigreeScenarios")), Duration(10, SECONDS))
  val duration = Duration(10, SECONDS)

  val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None))
  val pedigreeId = 555
  val scenario = PedigreeScenario(MongoId(new ObjectId().toString), pedigreeId, "scenario", "descripción", individuals, ScenarioStatus.Pending, "base")


  "A Pedigree Scenario repository" must {

    "create a scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      val result = Await.result(repository.create(scenario), duration)
      result mustBe Right(scenario._id)

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "not create a duplicated scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      Await.result(repository.create(scenario), duration)
      val result = Await.result(repository.create(scenario), duration)
      result.isLeft mustBe true
      Await.result(pedigreeScenarios.drop(false), duration)
    }
    ////data base E11000

    "update a scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      Await.result(pedigreeScenarios.insert(scenario), duration)
      val result = Await.result(repository.update(scenario), duration)
      result mustBe Right(scenario._id)

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "upsert a scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      val result = Await.result(repository.update(scenario), duration)
      result mustBe Right(scenario._id)

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "update a scenario with lr = 0" in {
      val scenario = PedigreeScenario(MongoId(new ObjectId().toString), pedigreeId, "scenario", "descripción", individuals, ScenarioStatus.Pending, "base", false, Some(0.0.toString))

      val repository = new MongoPedigreeScenarioRepository

      val id = Await.result(repository.update(scenario), duration).right.get
      val result = Await.result(pedigreeScenarios.find(Json.obj("_id" -> id)).one[PedigreeScenario], duration).get

      result._id mustBe id
      result.isProcessing mustBe false
      result.lr mustBe Some("0.0")

      Await.result(pedigreeScenarios.drop(false), duration)
    }


    "get a scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      Await.result(pedigreeScenarios.insert(scenario), duration)
      val result = Await.result(repository.get(scenario._id), duration)
      result mustBe Some(scenario)

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "get a non-existent scenario" in {
      val repository = new MongoPedigreeScenarioRepository

      val result = Await.result(repository.get(scenario._id), duration)
      result mustBe None
    }

    "get scenarios by pedigree" in {
      val repository = new MongoPedigreeScenarioRepository

      val scenario2 = PedigreeScenario(MongoId(new ObjectId().toString), 14l, "scenario", "descripción", individuals, ScenarioStatus.Pending, "base")
      Await.result(pedigreeScenarios.insert(scenario), duration)
      Await.result(pedigreeScenarios.insert(scenario2), duration)
      val result = Await.result(repository.getByPedigree(pedigreeId), duration)

      result.size mustBe 1
      result(0) mustBe scenario

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "get scenarios by pedigree and not deleted" in {
      val repository = new MongoPedigreeScenarioRepository

      val scenario2 = PedigreeScenario(MongoId(new ObjectId().toString), pedigreeId, "scenario", "descripción", individuals, ScenarioStatus.Deleted, "base")
      Await.result(pedigreeScenarios.insert(scenario), duration)
      Await.result(pedigreeScenarios.insert(scenario2), duration)
      val result = Await.result(repository.getByPedigree(pedigreeId), duration)

      result.size mustBe 1
      result(0) mustBe scenario

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "change a scenario status" in {
      val repository = new MongoPedigreeScenarioRepository

      Await.result(pedigreeScenarios.insert(scenario), duration)
      val result = Await.result(repository.changeStatus(scenario._id, ScenarioStatus.Validated), duration)
      result mustBe Right(scenario._id)

      Await.result(pedigreeScenarios.drop(false), duration)
    }

    "delete all scenarios by pedigree id" in {
      val repository = new MongoPedigreeScenarioRepository

      Await.result(pedigreeScenarios.insert(scenario), duration)

      val result = Await.result(repository.deleteAll(scenario.pedigreeId), duration)
      val scenariosList = Await.result(pedigreeScenarios.find(Json.obj("pedigreeId" -> scenario.pedigreeId)).cursor[PedigreeScenario]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeScenario]]()), duration)

      result mustBe Right(scenario.pedigreeId)
      scenariosList.length mustBe 0
    }

  }
}

