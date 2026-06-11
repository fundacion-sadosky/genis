package integration.controllers

import fixtures.{StubLdapHealthService, StubProbabilityService}
import org.scalatestplus.play.*
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import pedigree.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import probability.{ProbabilityModule, ProbabilityService}
import security.{StubRoleRepository, StubUserRepository, UserRepository}
import user.{LdapHealthService, RoleRepository, UsersModule}

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class MutationControllerTest extends PlaySpec with GuiceOneAppPerTest {

  private val sampleModel = MutationModel(
    id = 7L, name = "Stepwise", mutationType = 2L,
    active = true, ignoreSex = false, cantSaltos = 1L
  )

  private val sampleParam = MutationModelParameter(
    id = 1L, idMutationModel = 7L, locus = "D3S1358", sex = "F",
    mutationRate = Some(BigDecimal("0.001")),
    mutationRange = Some(BigDecimal("0.5")),
    mutationRateMicrovariant = None
  )

  private val sampleFull = MutationModelFull(sampleModel, List(sampleParam))

  // Stub configurable: una sola variante con datos típicos, suficiente para cubrir
  // happy paths + path "Nil → NotFound" del controller.
  private val stubMutationService: MutationService = new MutationService {
    override def getAllMutationModelType(): Future[List[MutationModelType]] =
      Future.successful(List(MutationModelType(2L, "Stepwise")))
    override def getAllMutationModels(): Future[List[MutationModel]] =
      Future.successful(List(sampleModel))
    override def getActiveMutationModels(): Future[List[MutationModel]] =
      Future.successful(List(sampleModel))
    override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] =
      Future.successful(Right(()))
    override def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]] =
      Future.successful(Right(42L))
    override def updateMutationModel(f: MutationModelFull): Future[Either[String, Unit]] =
      Future.successful(Right(()))
    override def getMutationModel(id: Option[Long]): Future[Option[MutationModelFull]] =
      if id.contains(7L) then Future.successful(Some(sampleFull))
      else Future.successful(None)
    override def getMutatitionModelParameters(id: Long): Future[List[MutationModelParameter]] =
      Future.successful(List(sampleParam))
    override def getMutationModelData(
      m: Option[MutationModel],
      markers: List[String]
    ): Future[Option[List[(MutationModelParameter, List[MutationModelKi], MutationModel)]]] =
      Future.successful(None)
    override def addLocus(full: kits.FullLocus): Future[Either[String, Unit]] =
      Future.successful(Right(()))
    override def generateKis(m: MutationModel): Future[Unit] = Future.successful(())
    override def refreshAllKis(): Future[Unit] = Future.successful(())
    override def refreshAllKisSecuential(): Future[Unit] = Future.successful(())
    override def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]] =
      Future.successful(List(MutationDefaultParam("D3S1358", "F", Some(BigDecimal("0.001")))))
    override def getAllLocusAlleles(): Future[List[(String, Double)]] =
      Future.successful(List(("D3S1358", 10.0)))
    override def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]] =
      Future.successful(Right(list.size))
    override def generateN(profiles: Array[profile.Profile], m: Option[MutationModel]): Future[Either[String, Unit]] =
      Future.successful(Right(()))
    override def getAllPossibleAllelesByLocus(): Future[Map[String, List[Double]]] =
      Future.successful(Map.empty)
  }

  // Stub que devuelve Nil para verificar el path "NotFound" del controller.
  private val emptyMutationService: MutationService = new MutationService {
    override def getAllMutationModelType(): Future[List[MutationModelType]] = Future.successful(Nil)
    override def getAllMutationModels(): Future[List[MutationModel]] = Future.successful(Nil)
    override def getActiveMutationModels(): Future[List[MutationModel]] = Future.successful(Nil)
    override def deleteMutationModelById(id: Long): Future[Either[String, Unit]] = Future.successful(Right(()))
    override def insertMutationModel(row: MutationModelFull): Future[Either[String, Long]] = Future.successful(Right(0L))
    override def updateMutationModel(f: MutationModelFull): Future[Either[String, Unit]] = Future.successful(Right(()))
    override def getMutationModel(id: Option[Long]): Future[Option[MutationModelFull]] = Future.successful(None)
    override def getMutatitionModelParameters(id: Long): Future[List[MutationModelParameter]] = Future.successful(Nil)
    override def getMutationModelData(m: Option[MutationModel], markers: List[String]) = Future.successful(None)
    override def addLocus(full: kits.FullLocus): Future[Either[String, Unit]] = Future.successful(Right(()))
    override def generateKis(m: MutationModel): Future[Unit] = Future.successful(())
    override def refreshAllKis(): Future[Unit] = Future.successful(())
    override def refreshAllKisSecuential(): Future[Unit] = Future.successful(())
    override def getAllMutationDefaultParameters(): Future[List[MutationDefaultParam]] = Future.successful(Nil)
    override def getAllLocusAlleles(): Future[List[(String, Double)]] = Future.successful(Nil)
    override def saveLocusAlleles(list: List[(String, Double)]): Future[Either[String, Int]] = Future.successful(Right(0))
    override def generateN(profiles: Array[profile.Profile], m: Option[MutationModel]): Future[Either[String, Unit]] = Future.successful(Right(()))
    override def getAllPossibleAllelesByLocus(): Future[Map[String, List[Double]]] = Future.successful(Map.empty)
  }

  // Por defecto la app usa el stub con datos; los tests "vacío → 404" sobreescriben con emptyMutationService.
  override def fakeApplication(): Application = appWith(stubMutationService)

  private def appWith(mutationStub: MutationService): Application =
    GuiceApplicationBuilder()
      .disable[UsersModule]
      .disable[MutationModule]
      .disable[ProbabilityModule]
      .overrides(
        bind[UserRepository].to[StubUserRepository],
        bind[RoleRepository].to[StubRoleRepository],
        bind[MutationService].toInstance(mutationStub),
        new fixtures.StubProbabilityModule,
        bind[ExecutionContext].qualifiedWith("lrmix-context").toInstance(ExecutionContext.global),
        bind[LdapHealthService].toInstance(new StubLdapHealthService)
      )
      .configure("play.http.secret.key" -> "test-secret-key-for-testing-purposes-only-not-for-production-1234")
      .build()

  "MutationController GET endpoints" must {

    "return 200 OK + JSON array for GET /api/v2/mutation-models (con datos)" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-models")).get
      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      contentAsJson(result).as[JsArray].value must not be empty
    }

    "return 404 NotFound para GET /api/v2/mutation-models cuando no hay datos" in {
      val emptyApp = appWith(emptyMutationService)
      val result = route(emptyApp, FakeRequest(GET, "/api/v2/mutation-models")).get
      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "message").as[String] mustBe "No existe"
    }

    "return 200 OK para GET /api/v2/mutation-models/active" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-models/active")).get
      status(result) mustBe OK
    }

    "return 200 OK para GET /api/v2/mutation-models-types" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-models-types")).get
      status(result) mustBe OK
    }

    "return 200 OK para GET /api/v2/mutation-models/default-params" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-models/default-params")).get
      status(result) mustBe OK
    }

    "return 200 OK para GET /api/v2/mutation-model?id=7" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-model?id=7")).get
      status(result) mustBe OK
      (contentAsJson(result) \ "header" \ "id").as[Long] mustBe 7L
    }

    "return 404 NotFound para GET /api/v2/mutation-model?id=99 (no existe)" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-model?id=99")).get
      status(result) mustBe NOT_FOUND
    }

    "return 200 OK para GET /api/v2/mutation-model-parameters?id=7 (siempre lista)" in {
      val result = route(app, FakeRequest(GET, "/api/v2/mutation-model-parameters?id=7")).get
      status(result) mustBe OK
    }
  }

  "MutationController DELETE" must {

    "return 200 OK para DELETE /api/v2/mutation-models/:id (bug legacy preservado: siempre OK)" in {
      val result = route(app, FakeRequest(DELETE, "/api/v2/mutation-models/42")).get
      status(result) mustBe OK
    }
  }

  "MutationController POST /api/v2/mutation-models" must {

    "return 200 OK con id para body JSON válido" in {
      val body = Json.toJson(sampleFull)
      val result = route(app, FakeRequest(POST, "/api/v2/mutation-models").withJsonBody(body)).get
      status(result) mustBe OK
      (contentAsJson(result) \ "id").as[Long] mustBe 42L
    }

    "return 400 BadRequest si el JSON no valida contra MutationModelFull" in {
      val body = Json.obj("foo" -> "bar")  // falta header y parameters
      val result = route(app, FakeRequest(POST, "/api/v2/mutation-models").withJsonBody(body)).get
      status(result) mustBe BAD_REQUEST
    }

    "return 415 UnsupportedMediaType si Content-Type ≠ application/json (post fix I1)" in {
      val req = FakeRequest(POST, "/api/v2/mutation-models")
        .withHeaders("Content-Type" -> "text/plain")
        .withBody("este body no es JSON")
      val result = route(app, req).get
      status(result) mustBe UNSUPPORTED_MEDIA_TYPE
    }
  }

  "MutationController PUT /api/v2/mutation-models" must {

    "return 200 OK para body válido" in {
      val body = Json.toJson(sampleFull)
      val result = route(app, FakeRequest(PUT, "/api/v2/mutation-models").withJsonBody(body)).get
      status(result) mustBe OK
    }

    "aceptar body > 100KB (regression test C1: límite subido de default a 16MB)" in {
      // Default parse.json en Play 3 = play.http.parser.maxMemoryBuffer (100 KB).
      // Con parse.json(16MB) post-fix, ~200KB debe pasar.
      val bigName = "x" * 200000  // 200 KB en un solo campo
      val bigModel = sampleModel.copy(name = bigName)
      val body = Json.toJson(MutationModelFull(bigModel, List(sampleParam)))

      body.toString.length must be > 100000  // sanity check

      val result = route(app, FakeRequest(PUT, "/api/v2/mutation-models").withJsonBody(body)).get
      status(result) mustBe OK
    }
  }

  "MutationController PUT /api/v2/mutation-models-matrix" must {

    "return 200 OK para body válido (generateMatrix)" in {
      val body = Json.toJson(sampleFull)
      val result = route(app, FakeRequest(PUT, "/api/v2/mutation-models-matrix").withJsonBody(body)).get
      status(result) mustBe OK
    }
  }
}
