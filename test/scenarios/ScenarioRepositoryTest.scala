package scenarios


import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import specs.PdgSpec
import stubs.Stubs
import types.{MongoDate, MongoId}
import scala.concurrent.Await
import scala.concurrent.duration._
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor

class ScenarioRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val scenariosCollection = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("scenarios")), Duration(10, SECONDS))

  "A Scenario repository" must {
    "add new scenario into it and return entity name and id formatted" in {
      val scenario = Stubs.newScenario

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.add(scenario), duration)
      result must not be null
      result mustBe Right(scenario._id.id)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "add new scenario into it and return an error" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.add(scenario), duration)
      result must not be null
      result.isLeft mustBe true


      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve the scenario has been inserted" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample)
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
          x._id mustBe scenario._id
          x.description mustBe Some("Description A")
          x.state mustBe ScenarioStatus.Pending
          x.calculationScenario.sample mustBe scenario.calculationScenario.sample
          x.calculationScenario.prosecutor.unknowns mustBe 3
          x.calculationScenario.prosecutor.dropOut mustBe 0.4
          x.calculationScenario.defense.unknowns mustBe 1
          x.calculationScenario.defense.dropOut mustBe 0.5
          x.calculationScenario.stats.frequencyTable mustBe "A"
          x.calculationScenario.stats.probabilityModel mustBe "1"
          x.calculationScenario.stats.theta mustBe 1
          x.calculationScenario.stats.dropIn mustBe 0.4
          x.calculationScenario.stats.dropOut mustBe Some(0.5)
          x.isRestricted mustBe false
          x.result.get.total mustBe 0.0
          x.result.get.detailed.size mustBe 0
        }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get the scenario assigned to other genetist by superuser" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample)
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get("SuperGenetista", scenarioSearch, true), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0.0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve three scenarios" in {
      val scenario1 = Stubs.newScenario
      val scenario2 = Stubs.newScenario
      val scenario3 = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario1.calculationScenario.sample)
      Await.result(scenariosCollection.insert(scenario1), duration)
      Await.result(scenariosCollection.insert(scenario2), duration)
      Await.result(scenariosCollection.insert(scenario3), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario1.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 3

      scenarios.foreach(x => {
          x.state mustBe ScenarioStatus.Pending
          x.calculationScenario.prosecutor.unknowns mustBe 3
          x.calculationScenario.prosecutor.dropOut mustBe 0.4
          x.calculationScenario.defense.unknowns mustBe 1
          x.calculationScenario.defense.dropOut mustBe 0.5
          x.calculationScenario.stats.frequencyTable mustBe "A"
          x.calculationScenario.stats.probabilityModel mustBe "1"
          x.calculationScenario.stats.theta mustBe 1
          x.calculationScenario.stats.dropIn mustBe 0.4
          x.calculationScenario.stats.dropOut mustBe Some(0.5)
          x.isRestricted mustBe false
          x.result.get.total mustBe 0.0
          x.result.get.detailed.size mustBe 0
        }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve the scenario filtered by name" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample, Some("Scenario A"))
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0.0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve the scenario filtered by name case insensitive" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample, Some("scenario a"))
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0.0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve the scenario filtered by a short name case insensitive" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample, Some("sc"))
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0.0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "validate user in retrieve scenarios" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample)
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get("other_geneticist", scenarioSearch, false), duration)

      scenarios.length mustBe 0

      Await.result(scenariosCollection.drop(false), duration)
    }

    "delete the scenario rigth" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.delete(scenario.geneticist, scenario._id, false), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "delete the scenario rigth by super user" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.delete("SuperGenetist", scenario._id, true), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "validate user in delete scenario E0645" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.delete("other_geneticist", scenario._id, false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0645: El usuario no tiene permisos sobre el escenario.")

      Await.result(scenariosCollection.drop(false), duration)
    }

    "validate scenario that not exist left E0645" in {
      val scenario = Stubs.newScenario

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.validate(scenario.geneticist, scenario, false), duration)

      result must not be null
      result.isLeft mustBe false
    }

    "validate scenario that exist right" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.drop(false), duration)

      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.validate(scenario.geneticist, scenario, false), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      val scenarios: Seq[Scenario] = Await.result(scenariosCollection.find(Json.obj()).cursor[Scenario]().collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]()), duration)

      scenarios.length mustBe 1
      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Validated
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "validate scenario that exist right by superuser" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.drop(false), duration)

      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.validate("SuperGenetist", scenario, true), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      val scenarios: Seq[Scenario] = Await.result(scenariosCollection.find(Json.obj()).cursor[Scenario]().collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]()), duration)

      scenarios.length mustBe 1
      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.description mustBe Some("Description A")
        x.state mustBe ScenarioStatus.Validated
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

//    "validate user in scenario validation E0645" in {
//      val scenario = Stubs.newScenario
//
//      Await.result(scenariosCollection.insert(scenario), duration)
//
//      val repository = new MongoScenarioRepository
//
//      val result = Await.result(repository.validate("other_geneticist", scenario, false), duration)
//
//      result.isLeft mustBe true
//      result mustBe Left("E0645: El usuario no tiene permisos sobre el escenario.")
//
//      Await.result(scenariosCollection.drop(false), duration)
//    }

    "get the scenario has been inserted" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result: Option[Scenario] = Await.result(repository.get(scenario.geneticist, scenario._id, false), duration)

      result.isDefined mustBe true
      result.get mustBe scenario

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get the scenario has been inserted by superuser" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result: Option[Scenario] = Await.result(repository.get("SuperGenetist", scenario._id, true), duration)

      result.isDefined mustBe true
      result.get mustBe scenario

      Await.result(scenariosCollection.drop(false), duration)
    }

    "update scenario" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val scenarioUpdated = Scenario(scenario._id, "Nombre Actualizado", scenario.state, "Genetista", scenario.calculationScenario, scenario.date, true, scenario.result, Some("Descripcion"))

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.update(scenario.geneticist, scenarioUpdated, false), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      val scenarios: Seq[Scenario] = Await.result(scenariosCollection.find(Json.obj()).cursor[Scenario]().collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]()), duration)

      scenarios.length mustBe 1
      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.name mustBe "Nombre Actualizado"
        x.geneticist mustBe "Genetista"
        x.description mustBe Some("Descripcion")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe true
        x.result.get.total mustBe 0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "update scenario by superuser" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val scenarioUpdated = Scenario(scenario._id, "Nombre Actualizado", scenario.state, "Genetista", scenario.calculationScenario, scenario.date, true, scenario.result, Some("Descripcion"))

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.update("SuperGenetist", scenarioUpdated, true), duration)

      result must not be null
      result mustBe Right(scenario._id.id)

      val scenarios: Seq[Scenario] = Await.result(scenariosCollection.find(Json.obj()).cursor[Scenario]().collect[List](Int.MaxValue, Cursor.FailOnError[List[Scenario]]()), duration)

      scenarios.length mustBe 1
      scenarios.foreach(x => {
        x._id mustBe scenario._id
        x.name mustBe "Nombre Actualizado"
        x.geneticist mustBe "Genetista"
        x.description mustBe Some("Descripcion")
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenario.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe true
        x.result.get.total mustBe 0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "validate user in update scenario E0645" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val scenarioUpdated = Scenario(scenario._id, "Nombre Actualizado", scenario.state, "Genetista", scenario.calculationScenario, scenario.date, true, scenario.result, Some("Descripcion"))

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.update("other_geneticist", scenarioUpdated, false), duration)

      result.isLeft mustBe true
      result mustBe Left("E0645: El usuario no tiene permisos sobre el escenario.")

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get by profile in sample" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByProfile(scenario.calculationScenario.sample), duration)

      result must contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get by profile in hypothesis" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByProfile(scenario.calculationScenario.defense.unselected.head), duration)

      result must contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get no results by profile" in {
      val scenario = Scenario(MongoId(new ObjectId().toString), "Scenario A", ScenarioStatus.Validated, "Genetista", Stubs.calculationScenario, MongoDate(new Date()), false, Some(Stubs.lrResult), Some("Description A"))

      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByProfile(scenario.calculationScenario.sample), duration)

      result must not contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get by match" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByMatch(scenario.calculationScenario.sample,
                                scenario.calculationScenario.prosecutor.selected.head,
                                scenario.geneticist, false), duration)

      result must contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get by match and superuser" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByMatch(scenario.calculationScenario.sample,
        scenario.calculationScenario.prosecutor.selected.head,
        "SuperGenetist", true), duration)

      result must contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "get no results by match" in {
      val scenario = Stubs.newScenario
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val result = Await.result(repository.getByMatch(scenario.calculationScenario.sample,
                                scenario.calculationScenario.prosecutor.selected.head,
                                "other_geneticist", false), duration)

      result must not contain(scenario)

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve the scenario filtered by pending state" in {
      val scenarioPending = Stubs.newScenario
      val scenarioValidated = Scenario(MongoId(new ObjectId().toString), "Scenario B", ScenarioStatus.Validated, "Genetista", scenarioPending.calculationScenario, MongoDate(new Date()), false, Some(scenarioPending.result.get), Some("Description A"))
      val scenarioSearch = ScenarioSearch(scenarioPending.calculationScenario.sample, None, None, None, Some(false))
      Await.result(scenariosCollection.insert(scenarioPending), duration)
      Await.result(scenariosCollection.insert(scenarioValidated), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenarioPending.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 1

      scenarios.foreach(x => {
        x._id mustBe scenarioPending._id
        x.description mustBe Some("Description A")
        x.name mustBe "Scenario A"
        x.state mustBe ScenarioStatus.Pending
        x.calculationScenario.sample mustBe scenarioPending.calculationScenario.sample
        x.calculationScenario.prosecutor.unknowns mustBe 3
        x.calculationScenario.prosecutor.dropOut mustBe 0.4
        x.calculationScenario.defense.unknowns mustBe 1
        x.calculationScenario.defense.dropOut mustBe 0.5
        x.calculationScenario.stats.frequencyTable mustBe "A"
        x.calculationScenario.stats.probabilityModel mustBe "1"
        x.calculationScenario.stats.theta mustBe 1
        x.calculationScenario.stats.dropIn mustBe 0.4
        x.calculationScenario.stats.dropOut mustBe Some(0.5)
        x.isRestricted mustBe false
        x.result.get.total mustBe 0.0
        x.result.get.detailed.size mustBe 0
      }
      )

      Await.result(scenariosCollection.drop(false), duration)
    }

    "retrieve no scenario filtering by validated state" in {
      val scenario = Stubs.newScenario
      val scenarioSearch = ScenarioSearch(scenario.calculationScenario.sample, Some("Scenario A"), None, None, Some(true))
      Await.result(scenariosCollection.insert(scenario), duration)

      val repository = new MongoScenarioRepository

      val scenarios: Seq[Scenario] = Await.result(repository.get(scenario.geneticist, scenarioSearch, false), duration)
      scenarios must not be null

      scenarios.length mustBe 0

      Await.result(scenariosCollection.drop(false), duration)
    }

  }
}
