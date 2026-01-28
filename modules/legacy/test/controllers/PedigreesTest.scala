package controllers

import org.bson.types.ObjectId
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import pedigree.{PedigreeMatchesService, _}
import play.api.libs.json.Json
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scenarios.ScenarioStatus
import scenarios.ScenarioStatus.ScenarioStatus
import specs.PdgSpec
import trace.{TracePedigree, TraceService}
import types.{MongoId, Sex}
import user.UserService

import scala.concurrent.Future

class PedigreesTest extends PdgSpec with MockitoSugar with Results {

  val id = 12345l

  val individuals = Seq(
    Individual(NodeAlias("Node1"), None, None, Sex.Male, None, false,None),
    Individual(NodeAlias("Node2"), None, None, Sex.Female, None, true,None)
  )

  val scenario = PedigreeScenario(MongoId(new ObjectId().toString), 55l, "scenario", "descripción", individuals, ScenarioStatus.Pending, "base")

  "Pedigrees controller" must {
    "change status to active - ok" in {

      val pedigreeGenogram = PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, "user", true)).thenReturn(Future.successful(Right(id)))
      when(pedigreeService.addGenogram(any[PedigreeGenogram])).thenReturn(Future.successful(Right(id)))

      val pedigreeGenotypificationService = mock[PedigreeGenotypificationService]
      when(pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id)).thenReturn(Future.successful(Right(id)))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, null, null, null, pedigreeGenotypificationService, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.toJson(pedigreeGenogram))
      val result: Future[Result] = target.changePedigreeStatus(id, "Active").apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id.toString
    }

    "change status to active - bad request changing status" in {

      val pedigreeGenogram = PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, "user", true)).thenReturn(Future.successful(Left("Error")))
      when(pedigreeService.addGenogram(any[PedigreeGenogram])).thenReturn(Future.successful(Right(id)))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.toJson(pedigreeGenogram))
      val result: Future[Result] = target.changePedigreeStatus(id, "Active").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to active - bad request adding genogram" in {

      val pedigreeGenogram = PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.addGenogram(any[PedigreeGenogram])).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.toJson(pedigreeGenogram))
      val result: Future[Result] = target.changePedigreeStatus(id, "Active").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to active - bad request generating genotypification" in {

      val pedigreeGenogram = PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)
      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.Active, "user", true)).thenReturn(Future.successful(Right(id)))
      when(pedigreeService.addGenogram(any[PedigreeGenogram])).thenReturn(Future.successful(Right(id)))

      val pedigreeGenotypificationService = mock[PedigreeGenotypificationService]
      when(pedigreeGenotypificationService.generateGenotypificationAndFindMatches(id)).thenReturn(Future.successful(Left("error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, null, null, null, pedigreeGenotypificationService, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.toJson(pedigreeGenogram))
      val result: Future[Result] = target.changePedigreeStatus(id, "Active").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to active - bad request input invalid" in {

      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))
      val target = new Pedigrees(null, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.toJson("Invalid"))
      val result: Future[Result] = target.changePedigreeStatus(id, "Active").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to under construction - ok" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.UnderConstruction, "user", true)).thenReturn(Future.successful(Right(id)))

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.deleteAllScenarios(id)).thenReturn(Future.successful(Right(id)))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, pedigreeScenarioService, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "UnderConstruction").apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id.toString
    }

    "change status to under construction - bad request deleting all scenarios" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.UnderConstruction, "user", true)).thenReturn(Future.successful(Right(id)))

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.deleteAllScenarios(id)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, pedigreeScenarioService, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "UnderConstruction").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to under construction - bad request changing status" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(id, PedigreeStatus.UnderConstruction, "user", true)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "user").withHeaders("X-SUPERUSER" -> "true").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "UnderConstruction").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to deleted - ok" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(any[Long], any[PedigreeStatus.Value], any[String], any[Boolean])).thenReturn(Future.successful(Right(id)))

      val pedigreeMatchesService = mock[PedigreeMatchesService]
      when(pedigreeMatchesService.deleteMatches(id)).thenReturn(Future.successful(Right(id)))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, pedigreeMatchesService, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "Deleted").apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id.toString
    }

    "change status to deleted - bad request changing status" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(any[Long], any[PedigreeStatus.Value], any[String], any[Boolean])).thenReturn(Future.successful(Left("error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "Deleted").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status to deleted - bad request deleting all matches" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(any[Long], any[PedigreeStatus.Value], any[String], any[Boolean])).thenReturn(Future.successful(Right(id)))

      val pedigreeMatchesService = mock[PedigreeMatchesService]
      when(pedigreeMatchesService.deleteMatches(id)).thenReturn(Future.successful(Left("Error")))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, pedigreeMatchesService, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.obj())
      val result: Future[Result] = target.changePedigreeStatus(id, "Deleted").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change status - bad request invalid status" in {

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.changePedigreeStatus(any[Long], any[PedigreeStatus.Value], any[String], any[Boolean])).thenReturn(Future.successful(Left("error")))
      when(pedigreeService.addGenogram(any[PedigreeGenogram])).thenReturn(Future.successful(Right(id)))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.toJson(individuals))
      val result: Future[Result] = target.changePedigreeStatus(id, "Invalid").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "get court cases - ok" in {
      val search = PedigreeSearch(0, 30, "pdg", false)

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getAllCourtCases(any[PedigreeSearch])).thenReturn(Future.successful(Seq()))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.getCourtCases().apply(request)

      status(result) mustBe OK
    }

    "get court cases - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.getCourtCases().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "get total court cases - ok" in {
      val search = PedigreeSearch(0, 30, "pdg", false)

      val pedigreeService = mock[PedigreeService]
      when(pedigreeService.getTotalCourtCases(any[PedigreeSearch])).thenReturn(Future.successful(5))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(pedigreeService, null, null, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.getTotalCourtCases().apply(request)

      status(result) mustBe OK
      header("X-PEDIGREES-LENGTH", result).get mustBe "5"
    }

    "get total court cases - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.getTotalCourtCases().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "determine if a pedigree is editable" in {
      val mockResult = true

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.allMatchesDiscarded(any[Long])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null)

      val result: Future[Result] = target.canEdit(1l).apply(FakeRequest())
      val jsonVal = contentAsJson(result).as[Boolean]

      status(result) mustBe OK
      jsonVal mustBe mockResult
    }

    "get scenarios by pedigree" in {
      val mockResult = Seq(scenario)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.getScenarios(any[Long])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null,null,null)

      val result: Future[Result] = target.getScenarios(scenario.pedigreeId).apply(FakeRequest())
      val jsonVal = contentAsJson(result).as[Seq[PedigreeScenario]]

      status(result) mustBe OK
      jsonVal mustBe mockResult
    }

    "create a scenario - ok" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.createScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))
      val traceService = mock[TraceService]
      when(traceService.addTracePedigree(any[TracePedigree])).thenReturn(Future.successful(Right(1L)))
      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null,null,traceService)

      val request = FakeRequest().withBody(Json.toJson(scenario)).withHeaders("X-USER" -> "tst-admin")
      val result: Future[Result] = target.createScenario().apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe scenario._id.id.toString
    }

    "create a scenario - bad format" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.createScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null
      )

      val request = FakeRequest().withBody(Json.obj()).withHeaders("X-USER" -> "tst-admin")
      val result: Future[Result] = target.createScenario().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "create a scenario - service error" in {
      val mockResult = Left("Error")

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.createScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.toJson(scenario)).withHeaders("X-USER" -> "tst-admin")
      val result: Future[Result] = target.createScenario().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "update a scenario - ok" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.toJson(scenario))
      val result: Future[Result] = target.updateScenario().apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe scenario._id.id.toString
    }

    "update a scenario - bad format" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj())
      val result: Future[Result] = target.updateScenario().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "update a scenario - service error" in {
      val mockResult = Left("Error")

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.updateScenario(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.toJson(scenario))
      val result: Future[Result] = target.updateScenario().apply(request)

      status(result) mustBe BAD_REQUEST
    }


    "change a scenario status - ok" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.changeScenarioStatus(any[PedigreeScenario], any[ScenarioStatus], any[String], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("tst-admin")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withBody(Json.toJson(scenario)).withHeaders("X-USER" -> "tst-admin").withHeaders("X-SUPERUSER" -> "true")

      val result: Future[Result] = target.changeScenarioStatus("Deleted").apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe scenario._id.id.toString
    }

    "change a scenario status - bad format" in {
      val mockResult = Right(scenario._id)

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.changeScenarioStatus(any[PedigreeScenario], any[ScenarioStatus], any[String], any[Boolean])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("id"->"1")).withHeaders("X-USER" -> "tst-admin").withHeaders("X-SUPERUSER" -> "true")
      val result: Future[Result] = target.changeScenarioStatus("Deleted").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "change a scenario status - service error" in {
      val mockResult = Left("Error")

      val pedigreeScenarioService = mock[PedigreeScenarioService]
      when(pedigreeScenarioService.changeScenarioStatus(any[PedigreeScenario], any[ScenarioStatus], any[String], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("tst-admin")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, pedigreeScenarioService, null, null, null, null, null, userService = userService)

      val request = FakeRequest().withBody(Json.toJson(scenario)).withHeaders("X-USER" -> "tst-admin").withHeaders("X-SUPERUSER" -> "true")
      val result: Future[Result] = target.changeScenarioStatus("Deleted").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "get matches by group - ok" in {
      val search = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.getMatchesByGroup(any[PedigreeMatchGroupSearch])).thenReturn(Future.successful(Seq()))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.getMatchesByGroup().apply(request)

      status(result) mustBe OK
    }

    "get matches by group - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.getMatchesByGroup().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "find matches by pedigree match card search - ok" in {
      val search = PedigreeMatchCardSearch("user", true, "pedigree", 1, 30, None, None, None)

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.getMatches(search)).thenReturn(Future.successful(Seq()))
      val userService = mock[UserService]
      when(userService.isSuperUser("user")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.findMatches().apply(request)

      status(result) mustBe OK
    }

    "find matches by pedigree match card search - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.findMatches().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "count matches by group - ok" in {
      val search = PedigreeMatchGroupSearch("pdg", true, "12", "pedigree", PedigreeMatchKind.DirectLink, 0, 30, "profileStatus", true)

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.countMatchesByGroup(any[PedigreeMatchGroupSearch])).thenReturn(Future.successful(5))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.countMatchesByGroup().apply(request)

      status(result) mustBe OK
      header("X-MATCHES-LENGTH", result).get mustBe "5"
    }

    "count matches by group - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.countMatchesByGroup().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "discard a match - ok" in {
      val id = "58ac62e6ebb12c3bfcc1afac"
      val mockResult = Right(id)

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.discard(any[String], any[String], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false")
      val result: Future[Result] = target.discard(id).apply(request)

      status(result) mustBe OK
      header("X-CREATED-ID", result).get mustBe id
    }

    "discard a match - service error" in {
      val mockResult = Left("Error")

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.discard(any[String], any[String], any[Boolean])).thenReturn(Future.successful(mockResult))
      val userService = mock[UserService]
      when(userService.isSuperUser("geneticist")).thenReturn(Future.successful((false)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val request = FakeRequest().withHeaders("X-USER" -> "geneticist").withHeaders("X-SUPERUSER" -> "false")
      val result: Future[Result] = target.discard("58ac62e6ebb12c3bfcc1afac").apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "count matches - ok" in {
      val search = PedigreeMatchCardSearch("pdg", true, "pedigree", 0, 30)

      val pedigreeMatchService = mock[PedigreeMatchesService]
      when(pedigreeMatchService.countMatches(any[PedigreeMatchCardSearch])).thenReturn(Future.successful(5))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val target = new Pedigrees(null, null, pedigreeMatchService, null, null, null, null, userService = userService)

      val jsRequest = Json.toJson(search)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.countMatches().apply(request)

      status(result) mustBe OK
      header("X-MATCHES-LENGTH", result).get mustBe "5"
    }

    "count matches - bad request" in {
      val target = new Pedigrees(null, null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.countMatches().apply(request)

      status(result) mustBe BAD_REQUEST
    }

    "get lr" in {
      val mockResult = 1.0

      val bayesianNetworkService = mock[BayesianNetworkService]
      when(bayesianNetworkService.calculateProbability(any[PedigreeScenario])).thenReturn(Future.successful(mockResult))

      val target = new Pedigrees(null, null, null, bayesianNetworkService, null, null, null)

      val request = FakeRequest().withBody(Json.toJson(scenario))
      val result: Future[Result] = target.getLR().apply(request)
      val jsonVal = contentAsJson(result).as[Double]

      status(result) mustBe OK
      jsonVal mustBe mockResult
    }
  }
}
