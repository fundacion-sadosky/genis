package connections


import java.util.Date

import configdata.{CategoryConfiguration, CategoryRepository, CategoryService}
import inbox.NotificationService
import kits.AnalysisType
import matching.{MatchResult, MatchingRepository, MongoMatchingRepository}
import mockws.MockWS
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSClient, WSRequestHolder, WSResponse}
import play.api.mvc.Action
import play.api.mvc.Results._
import profile.{Analysis, Profile, ProfileService}
import profiledata.{DeletedMotive, ProfileData, ProfileDataService}
import services.{CacheService, ProfileLabKey}
import specs.PdgSpec
import stubs.Stubs
import types.Permission.INF_INS_CRUD
import types.{AlphanumericId, Permission, SampleCode}
import user.{RoleService, UserService}

import scala.collection.immutable.HashMap
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}
import models.Tables.ExternalProfileDataRow
import trace.TraceService

class InterconnectionServiceTest extends PdgSpec with MockitoSugar {
  val logger: Logger = Logger(this.getClass())
  val approvalSearch = ProfileApprovalSearch(1,5)
  val akkaSystem = Akka.system
  val client = WS.client
  val timeoutInSeconds = 10
  val duration = Duration(timeoutInSeconds, SECONDS)
  val protocol = "http://"
  val status = "/status"
  val categoryTreeCombo = "/superior/category-tree-combo"
  val insertConnection = "/superior/connection"
  val localUrl = "pdg-devclient:9000"
  val uploadProfile = "/superior/profile"
  val labCode = "SHGB"
  val analisis = Json.fromJson[Analysis](Json.parse( """

                                                                           {
                                                                              "id":"3847f6d0-2014-433a-bc90-c4fc6e93a8af",
                                                                              "date": { "$date": "2014-01-01" },
                                                                              "kit":"Powerplex21",
                                                                              "genotypification":{
                                                                                 "TH01":[
                                                                                    7,
                                                                                    9
                                                                                 ],
                                                                                 "D1S1656":[
                                                                                    16
                                                                                 ],
                                                                                 "D2S1338":[
                                                                                    17
                                                                                 ],
                                                                                 "CSF1PO":[
                                                                                    11,
                                                                                    12
                                                                                 ],
                                                                                 "PentaD":[
                                                                                    9,
                                                                                    11
                                                                                 ],
                                                                                 "D18S51":[
                                                                                    12,
                                                                                    15
                                                                                 ],
                                                                                 "AMEL":[
                                                                                    "X",
                                                                                    "Y"
                                                                                 ],
                                                                                 "D8S1179":[
                                                                                    11,
                                                                                    14
                                                                                 ],
                                                                                 "D3S1358":[
                                                                                    15,
                                                                                    16
                                                                                 ],
                                                                                 "D7S820":[
                                                                                    11,
                                                                                    12
                                                                                 ],
                                                                                 "D19S433":[
                                                                                    13,
                                                                                    14
                                                                                 ],
                                                                                 "FGA":[
                                                                                    23,
                                                                                    24
                                                                                 ],
                                                                                 "D16S539":[
                                                                                    11,
                                                                                    13
                                                                                 ],
                                                                                 "D13S317":[
                                                                                    9,
                                                                                    13
                                                                                 ],
                                                                                 "vWA":[
                                                                                    16,
                                                                                    18
                                                                                 ],
                                                                                 "D5S818":[
                                                                                    11,
                                                                                    12
                                                                                 ],
                                                                                 "D6S1043":[
                                                                                    11,
                                                                                    14
                                                                                 ],
                                                                                 "TPOX":[
                                                                                    8,
                                                                                    11
                                                                                 ],
                                                                                 "D12S391":[
                                                                                    19,
                                                                                    20
                                                                                 ],
                                                                                 "D21S11":[
                                                                                    31,
                                                                                    31.2
                                                                                 ],
                                                                                 "PentaE":[
                                                                                    11,
                                                                                    12
                                                                                 ]
                                                                              }
                                                                           }
          """)).get

  val cacheService = mock[CacheService]
  val profileDataServiceMock = mock[ProfileDataService]

  def interconnectionServiceFactory(connectionRepository: ConnectionRepository, inferiorInstanceRepository: InferiorInstanceRepository,
                                    wsClient: WSClient): InterconnectionServiceImpl = {
    new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository], mock[SuperiorInstanceProfileApprovalRepository], wsClient, null, null, null, null,
      null , protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,
      profileDataServiceMock,
      null,
      null,
      null,
      null,
      "tst-admin",
      "1 seconds",
      "1 seconds",
      "1 seconds",
      "1 seconds",
      1000,
      cacheService
    )
  }

  "Interconnection service " must {

    "get connections ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)
      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("192.168.0.1", "192.168.0.2"))))

      val result = Await.result(interconnectionService.getConnections(), duration);

      result mustBe Right(Connection("192.168.0.1", "192.168.0.2"))
    }

    "get connections no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Left("Db Error")))

      val result = Await.result(interconnectionService.getConnections(), duration);

      result mustBe Left("Db Error")
    }

    "update connections no ok E0708" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(connectionRepository.updateConnections(Connection("A", "B"))).
        thenReturn(Future.successful(Left("Db Error")))

      val result = Await.result(interconnectionService.updateConnections(Connection("A", "B")), duration);

      result.isLeft mustBe true

    }

    "update connections ok" in {
      val ws = MockWS {
        case ("GET", "http://localhost:9000/status") => Action {
          Ok
        }
      }
      val connectionRepository = mock[ConnectionRepository]
      val mockWsResponse: WSResponse = mock[WSResponse]
      val holder: WSRequestHolder = mock[WSRequestHolder]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val dummyUrl = "localhost:9000"
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)
      when(connectionRepository.updateConnections(Connection(dummyUrl, dummyUrl))).
        thenReturn(Future.successful(Right(Connection(dummyUrl, dummyUrl))))

      val result = Await.result(interconnectionService.updateConnections(Connection(dummyUrl, dummyUrl)), duration);

      result mustBe Right(Connection(dummyUrl, dummyUrl))
    }


    "get connections status no ok E0707" in {
      val ws = MockWS {
        case ("GET", "http://pdg-devclient/status") => Action {
          BadRequest
        }
      }
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getConnectionsStatus("test no ok"), duration);

      result.isLeft mustBe true
    }

    "get connections status no ok 2 E0707" in {

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]

      val ws = MockWS {
        case ("GET", "http://pdg-devclient/status") => Action {
          BadRequest
        }
      }
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getConnectionsStatus("pdg-devclient"), duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }
    "get connections status ok" in {
      val ws = MockWS {
        case ("GET", "http://Dummyurl.com/status") => Action {
          Ok
        }
      }

      val dummyUrl = "Dummyurl.com"

      //      val clientMock = mock[WSClient]
      //      val mockWsResponse: WSResponse = mock[WSResponse]
      //      val holder: WSRequestHolder = mock[WSRequestHolder]

      //      when(mockWsResponse.status).thenReturn(200)
      //      when(holder.get()).thenReturn(Future.successful(mockWsResponse))
      //      when(clientMock.url("http://"+dummyUrl+"/status")).thenReturn(holder)

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getConnectionsStatus(dummyUrl), duration);

      result.isRight mustBe true
    }

    "get category consumer ok" in {
      val ws = MockWS {
        case ("GET", "http://Dummyurl.com/status") => Action {
          Ok
        }
        case ("GET", "http://Dummyurl.com/superior/category-tree-combo") => Action {
          Ok("{}")
        }

      }

      val connectionRepository = mock[ConnectionRepository]

      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("Dummyurl.com", "Dummyurl.com"))))
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isRight mustBe true
    }
    "get category consumer fail to retrieve data E0707" in {
      val ws = MockWS {
        case ("GET", "http://Dummyurl.com/status") => Action {
          Ok
        }
        case ("GET", "http://Dummyurl.com/superior/category-tree-combo") => Action {
          BadRequest("{}")
        }

      }

      val connectionRepository = mock[ConnectionRepository]

      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("Dummyurl.com", "Dummyurl.com"))))
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }

    "get category consumer fail at get url superior instance E0707" in {
      val ws = MockWS {
        case ("GET", "http://Dummyurl.com/status") => Action {
          Ok
        }
        case ("GET", "http://Dummyurl.com/superior/category-tree-combo") => Action {
          Ok("{}")
        }

      }

      val connectionRepository = mock[ConnectionRepository]

      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.failed(new RuntimeException()))

      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }
    "convertToJson" in {
      val msi = MatchSuperiorInstance(Stubs.matchResult,
        Stubs.newProfile,
        SuperiorProfileData("Simples",
          None,
          "ahierro",
          None,
          "SHDG",
          "SHDG",
          "SHDG",
          None,
          "hola",
          None,
          None,
          None),
        None)

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,null, null, mock[CategoryRepository],
        mock[SuperiorInstanceProfileApprovalRepository], null, null, null, null, null, null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataServiceMock,null,null,new MongoMatchingRepository(""))
      val outputString = interconnectionService.convertToJson(msi)
      outputString.length>0 mustBe true
    }
    "get category consumer no ok 1 E0707" in {
      val dummyUrl = "Dummyurl.com"

      val clientMock = mock[WSClient]
      val mockWsResponse: WSResponse = mock[WSResponse]
      val holder: WSRequestHolder = mock[WSRequestHolder]

      when(mockWsResponse.status).thenReturn(400)
      when(holder.get()).thenReturn(Future.successful(mockWsResponse))
      when(clientMock.url(protocol + dummyUrl + categoryTreeCombo)).thenReturn(holder)

      val connectionRepository = mock[ConnectionRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("Dummyurl.com", "Dummyurl.com"))))
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, clientMock)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }

    "get category consumer no ok 2 E0707" in {
      val dummyUrl = "Dummyurl.com"

      val clientMock = mock[WSClient]
      val mockWsResponse: WSResponse = mock[WSResponse]
      val holder: WSRequestHolder = mock[WSRequestHolder]

      when(mockWsResponse.status).thenReturn(200)
      when(holder.get()).thenReturn(Future.successful(mockWsResponse))
      when(clientMock.url(protocol + dummyUrl + categoryTreeCombo)).thenReturn(holder)

      val connectionRepository = mock[ConnectionRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.failed(new RuntimeException()))
      when(connectionRepository.getConnections()).thenReturn(Future.failed(new RuntimeException()))
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, clientMock)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }

    "get category consumer no ok 3 E0707" in {
      val dummyUrl = "Dummyurl.com"

      val clientMock = mock[WSClient]
      val mockWsResponse: WSResponse = mock[WSResponse]
      val holder: WSRequestHolder = mock[WSRequestHolder]

      when(mockWsResponse.status).thenReturn(200)
      when(holder.get()).thenReturn(Future.failed(new RuntimeException()))
      when(clientMock.url(protocol + dummyUrl + categoryTreeCombo)).thenReturn(holder)

      val connectionRepository = mock[ConnectionRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("Dummyurl.com", "Dummyurl.com"))))
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, clientMock)

      val result = Await.result(interconnectionService.getCategoryConsumer, duration);

      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")
    }

    "insertInferiorInstanceConnection ok" in {

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(inferiorInstanceRepository.countByURL("a")).thenReturn(Future.successful(0L))
      when(inferiorInstanceRepository.findByLabCode(any[String])).thenReturn(Future.successful(None))

      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val permissions: Map[String, Set[Permission]] = HashMap("admin" -> Set(INF_INS_CRUD))

      when(userService.findUserAssignableByRole("admin")).thenReturn(Future.successful(Seq(Stubs.user)))
      when(roleService.getRolePermissions()).thenReturn(permissions)

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],
        mock[SuperiorInstanceProfileApprovalRepository]
        , client, userService, roleService, null, null, notificationService
        , protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,        profileDataServiceMock,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      when(inferiorInstanceRepository.insert(InferiorInstance(url = "a", laboratory = "lab"))).thenReturn(Future.successful(Right(1L)))

      val result = Await.result(interconnectionService.insertInferiorInstanceConnection("a", "lab"), duration);
      result.isRight mustBe true

    }

    "insertInferiorInstanceConnection no ok E0720" in {

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(inferiorInstanceRepository.countByURL("a")).thenReturn(Future.successful(0L))
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(inferiorInstanceRepository.insert(InferiorInstance(url = "a"))).thenReturn(Future.failed(new RuntimeException()))

      val result = Await.result(interconnectionService.insertInferiorInstanceConnection("a", "lab"), duration);
      result.isLeft mustBe true

    }
    "insertInferiorInstanceConnection no ok2 E0720" in {

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(inferiorInstanceRepository.countByURL("a")).thenReturn(Future.successful(1L))
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(inferiorInstanceRepository.insert(InferiorInstance(url = "a"))).thenReturn(Future.failed(new RuntimeException()))

      val result = Await.result(interconnectionService.insertInferiorInstanceConnection("a", "lab"), duration);
      result.isLeft mustBe true
      result mustBe Left("E0720: La instancia superior ya guardó la conexión.")

    }

    "insertInferiorInstanceConnection no ok3 E0720" in {

      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(inferiorInstanceRepository.countByURL("a")).thenReturn(Future.failed(new RuntimeException()))
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))

      when(inferiorInstanceRepository.insert(InferiorInstance(url = "a"))).thenReturn(Future.failed(new RuntimeException()))

      val result = Await.result(interconnectionService.insertInferiorInstanceConnection("a", "lab"), duration);
      result.isLeft mustBe true

    }
    "connect ok" in {

      val ws = MockWS {
        case ("POST", "http://pdg-devclient:9000/superior/connection") => Action {
          Ok
        }
      }

      val dummyUrl = localUrl
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some(dummyUrl)))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection(dummyUrl, dummyUrl))))

      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.connect(), duration);
      result.isRight mustBe true
    }

    "connect no ok E0707" in {

      val ws = MockWS {
        case ("POST", "http://pdg-devclient:9000/superior/connection") => Action {
          BadRequest("""{"message":"E0707: No se pudo conectar."}""")
        }
      }

      val dummyUrl = localUrl
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some(dummyUrl)))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection(dummyUrl, dummyUrl))))

      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, ws)

      val result = Await.result(interconnectionService.connect(), duration);
      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")

    }

    "connect no ok 2 E0707" in {

      val dummyUrl = ""
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some(dummyUrl)))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection(dummyUrl, dummyUrl))))

      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      val result = Await.result(interconnectionService.connect(), duration);
      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")

    }

    "connect no ok 3 E0707" in {

      val dummyUrl = ""
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.failed(new RuntimeException()))
      when(connectionRepository.getConnections()).thenReturn(Future.failed(new RuntimeException()))

      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      val result = Await.result(interconnectionService.connect(), duration);
      result.isLeft mustBe true
      result mustBe Left("E0707: No se pudo conectar.")

    }

    "getAllInferiorInstances" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(inferiorInstanceRepository.findAll()).thenReturn(Future.successful(Left("")))

      val result = Await.result(interconnectionService.getAllInferiorInstances(), duration);
      result.isLeft mustBe true
    }
    "getAllInferiorInstanceStatus" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val interconnectionService = interconnectionServiceFactory(connectionRepository, inferiorInstanceRepository, client)

      when(inferiorInstanceRepository.findAllInstanceStatus()).thenReturn(Future.successful(Left("")))

      val result = Await.result(interconnectionService.getAllInferiorInstanceStatus(), duration);
      result.isLeft mustBe true
    }
    "updateInferiorInstance ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val permissions: Map[String, Set[Permission]] = HashMap("admin" -> Set(INF_INS_CRUD))

      when(userService.findUserAssignableByRole("admin")).thenReturn(Future.successful(Seq(Stubs.user)))
      when(roleService.getRolePermissions()).thenReturn(permissions)

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository], mock[SuperiorInstanceProfileApprovalRepository]
        , client, userService, roleService, null, null, notificationService
        , protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,        profileDataServiceMock,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      when(inferiorInstanceRepository.update(InferiorInstanceFull(id = 1, url = "", connectivity = "", idStatus = 1))).thenReturn(Future.successful(Left("")))

      val result = Await.result(interconnectionService.updateInferiorInstance(InferiorInstanceFull(id = 1, url = "", connectivity = "", idStatus = 1)), duration);
      result.isLeft mustBe true
    }
    "uploadProfileToSuperiorInstance ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val traceService = mock[TraceService]

      val profile: Profile = Stubs.mixtureProfile
      val profileData: ProfileData = Stubs.profileData
      val categoryService = mock[CategoryService]

      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("pdg-devclient:9000")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("pdg-devclient:9000", "dummyUrl"))))

      when(categoryService.getCategoriesMappingById(profile.categoryId)).thenReturn(Future.successful(Some(profile.categoryId.text)))
      when(traceService.add(any())).thenReturn(Future.successful(Right(1l)))

      val uri = "http://pdg-devclient:9000" + uploadProfile

      val ws = MockWS {
        case ("POST", uri) => Action {
          Ok
        }
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        mock[SuperiorInstanceProfileApprovalRepository]
        , ws
        , userService
        , roleService
        , null
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,profileDataServiceMock,categoryService,
        traceService,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      interconnectionService.uploadProfileToSuperiorInstance(profile, profileData)

    }

    "uploadProfileToSuperiorInstance no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val categoryService = mock[CategoryService]
      val traceService = mock[TraceService]

      val profile: Profile = Stubs.mixtureProfile
      val profileData: ProfileData = Stubs.profileData

      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("pdg-devclient:9000")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("pdg-devclient:9000", "dummyUrl"))))

      when(categoryService.getCategoriesMappingById(profile.categoryId)).thenReturn(Future.successful(Some(profile.categoryId.text)))
      when(traceService.add(any())).thenReturn(Future.successful(Right(1l)))

      val uri = "http://pdg-devclient:9000" + uploadProfile

      val ws = MockWS {
        case ("POST", uri) => Action {
          BadRequest
        }
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        mock[SuperiorInstanceProfileApprovalRepository]
        , ws
        , userService
        , roleService
        , null
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,profileDataServiceMock,categoryService,
        traceService,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      interconnectionService.uploadProfileToSuperiorInstance(profile, profileData)

    }
    "importProfile ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]

      val profile: Profile = Stubs.mixtureProfile.copy(categoryId = AlphanumericId("INDIVIDUONN"),analyses = Some(List(analisis)))

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      val at: AnalysisType =  AnalysisType(1,"")
      val kitOption = Some(analisis.kit)
      when(profileService.getAnalysisType(kitOption,analisis.`type`)).thenReturn(Future.successful(at))
      when(profileService.validateAnalysis(analisis.genotypification,profile.categoryId,kitOption,profile.contributors.getOrElse(0),analisis.`type`,at)).thenReturn(Future.successful(Right(CategoryConfiguration())))
      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        mock[SuperiorInstanceProfileApprovalRepository]
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,        profileDataServiceMock,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)


      interconnectionService.importProfile(profile, "SHDG", "", "SHDG", "SHDG")

    }
    "importProfile with invalid category" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val profile: Profile = Stubs.mixtureProfile

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(false)

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        mock[SuperiorInstanceProfileApprovalRepository]
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataServiceMock,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      interconnectionService.importProfile(profile, "SHDG", "", "SHDG", "SHDG")

    }
    "importProfile with sample entry date" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val profile: Profile = Stubs.mixtureProfile

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(false)

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        mock[SuperiorInstanceProfileApprovalRepository]
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,        profileDataServiceMock,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      interconnectionService.importProfile(profile, "SHDG", "1", "SHDG", "SHDG")

    }
    "approveProfile no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      //      when(profileService.updateProfile(anyObject[Profile])).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.updateProfile(profile)).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.existProfile(profile.globalCode)).thenReturn(Future.successful(true))
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.approveProfile(profileApproval), Duration.Inf)
      result.isLeft mustBe true


    }
    "approveProfile non existent profile data ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(None))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      //      when(profileService.updateProfile(anyObject[Profile])).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.updateProfile(profile)).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.existProfile(profile.globalCode)).thenReturn(Future.successful(true))
      when(profileDataService.importFromAnotherInstance(any[ProfileData], anyString(), anyString())).thenReturn(Future.successful(()))
      when(profileService.addProfile(any[Profile])).thenReturn(Future.successful(profile.globalCode))

      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.approveProfile(profileApproval), Duration.Inf)
      result.isLeft mustBe true

    }
    "approveProfile invalid profile E0600" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(false)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(None))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      //      when(profileService.updateProfile(anyObject[Profile])).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.updateProfile(profile)).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.existProfile(profile.globalCode)).thenReturn(Future.successful(true))
      when(profileDataService.importFromAnotherInstance(any[ProfileData], anyString(), anyString())).thenReturn(Future.successful(()))
      when(profileService.addProfile(any[Profile])).thenReturn(Future.successful(profile.globalCode))

      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.approveProfile(profileApproval), Duration.Inf)
      result.isLeft mustBe true
      result.left.get.contains("E0600: No existe la categoría MULTIPLE.") mustBe true

    }
    "approveProfiles ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      //      when(profileService.updateProfile(anyObject[Profile])).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.updateProfile(profile)).thenReturn(Future.successful(profile.globalCode))
      //      when(profileService.existProfile(profile.globalCode)).thenReturn(Future.successful(true))
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.approveProfiles(List(profileApproval)), Duration.Inf)
      result.isLeft mustBe true
    }
    "getPendingProfiles ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(superiorInstanceProfileApprovalRepository.findAll(approvalSearch)).thenReturn(Future.successful(Nil))

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.getPendingProfiles(approvalSearch), Duration.Inf)
      result.isEmpty mustBe true
    }
    "notifyChangeStatus ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(superiorInstanceProfileApprovalRepository.findAll(approvalSearch)).thenReturn(Future.successful(Nil))

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      Await.result(interconnectionService.notifyChangeStatus(profile.globalCode.text, "SHDG", 1L), Duration.Inf)

    }
    //        "uploadProfile ok" in {
    //          val connectionRepository = mock[ConnectionRepository]
    //          val inferiorInstanceRepository = mock[InferiorInstanceRepository]
    //          val userService = mock[UserService]
    //          val roleService = mock[RoleService]
    //          val notificationService = mock[NotificationService]
    //          val categoryRepository = mock[CategoryRepository]
    //          val profileService = mock[ProfileService]
    //          val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
    //          val profile: Profile = Stubs.mixtureProfile
    //          val profileData: ProfileData = Stubs.profileData
    //          val profileApproval = ProfileApproval(profile.globalCode.text)
    //          val profileDataService = mock[ProfileDataService]
    //          val categoryService = mock[CategoryService]
    //
    //          when(profileService.get(profile.globalCode)).thenReturn(Future.successful(Some(profile)))
    //          when(profileDataService.get(profile.globalCode)).thenReturn(Future.successful(Some(profileData)))
    //          when(categoryService.getCategory(profileData.category)).thenReturn(Some(Stubs.fullCatA1))
    //          when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("pdg-devclient:9000")))
    //
    //          when(connectionRepository.getConnections()).
    //            thenReturn(Future.successful(Right(Connection("pdg-devclient:9000", "dummyUrl"))))
    //
    //          when(categoryService.getCategoriesMappingById(profile.categoryId)).thenReturn(Future.successful(Some(profile.categoryId.text)))
    //          val uri = "http://pdg-devclient:9000" + uploadProfile
    //
    //          val ws = MockWS {
    //            case ("POST", uri) => Action {
    //              Ok
    //            }
    //          }
    //
    //          val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
    //            connectionRepository,
    //            inferiorInstanceRepository,
    //            categoryRepository,
    //            superiorInstanceProfileApprovalRepository
    //            , ws
    //            , userService
    //            , roleService
    //            , profileService
    //            , notificationService
    //            , protocol
    //            , status
    //            , categoryTreeCombo
    //            , insertConnection
    //            , localUrl
    //            , uploadProfile
    //            , labCode,
    //            profileDataService,
    //            categoryService)
    //
    //          val result = Await.result(interconnectionService.uploadProfile(profile.globalCode.text), Duration.Inf)
    //          result.isRight mustBe true
    //
    //        }

    "uploadProfile no esta mapeada la categoria de la instancia superior" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileData: ProfileData = Stubs.profileData
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]
      val categoryService = mock[CategoryService]
      val traceService = mock[TraceService]

      when(profileDataService.updateUploadStatus(profile.globalCode.text,1L,Option.empty[String],Option.empty[String])).thenReturn(Future.successful(Right(())))

      when(profileService.get(profile.globalCode)).thenReturn(Future.successful(Some(profile)))
      when(profileDataService.get(profile.globalCode)).thenReturn(Future.successful(Some(profileData)))
      when(categoryService.getCategory(profileData.category)).thenReturn(Some(Stubs.fullCatA1))
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("pdg-devclient:9000")))

      when(connectionRepository.getConnections()).
        thenReturn(Future.successful(Right(Connection("pdg-devclient:9000", "dummyUrl"))))

      when(categoryService.getCategoriesMappingById(profile.categoryId)).thenReturn(Future.successful(None))
      when(traceService.add(any())).thenReturn(Future.successful(Right(1l)))

      val uri = "http://pdg-devclient:9000" + uploadProfile

      val ws = MockWS {
        case ("POST", uri) => Action {
          Ok
        }
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , ws
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        categoryService,
        traceService,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.uploadProfile(profile.globalCode.text), Duration.Inf)
      result.isLeft mustBe true

    }

    "uploadProfile no se puede conectar con la instancia superior" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileData: ProfileData = Stubs.profileData
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]
      val categoryService = mock[CategoryService]
      val traceService = mock[TraceService]

      when(profileDataService.updateUploadStatus(profile.globalCode.text,1L, Option.empty[String],Option.empty[String])).thenReturn(Future.successful(Right(())))
      when(profileService.get(profile.globalCode)).thenReturn(Future.successful(Some(profile)))
      when(profileDataService.get(profile.globalCode)).thenReturn(Future.successful(Some(profileData)))
      when(categoryService.getCategory(profileData.category)).thenReturn(Some(Stubs.fullCatA1))
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(None))

      when(categoryService.getCategoriesMappingById(profile.categoryId)).thenReturn(Future.successful(Some(profile.categoryId.text)))
      when(traceService.add(any())).thenReturn(Future.successful(Right(1l)))

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        categoryService,
        traceService,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.uploadProfile(profile.globalCode.text), Duration.Inf)
      result.isLeft mustBe true

    }

    "rejectProfile ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]
      val globalCode = Stubs.profileData.globalCode.text
      val url = "http://clientUrl/inferior/profile/status?globalCode=AR-B-LAB-1&status=3"
      when(inferiorInstanceRepository.findByLabCode("SHDG")).thenReturn(Future.successful(Some(InferiorInstance("clientUrl","SHDG"))))

      val ws = MockWS {
        case ("PUT", url) => Action {
          Ok
        }
      }

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Right(())))
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , ws
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.rejectProfile(ProfileApproval(profile.globalCode.text),"motivo",1L), Duration.Inf)
      result.isRight mustBe true
    }
    "rejectProfile falla la coneción para actualizar el estado en la inferior" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]
      val globalCode = Stubs.profileData.globalCode.text
      val url = "http://clientUrl/inferior/profile/status?globalCode=AR-B-LAB-1&status=3"
      when(inferiorInstanceRepository.findByLabCode("SHDG")).thenReturn(Future.successful(Some(InferiorInstance("clientUrl","SHDG"))))

      val ws = MockWS {
        case ("PUT", url) => Action {
          BadRequest
        }
      }

      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      when(profileDataService.findByCodeWithoutDetails(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileService.findByCode(profile.globalCode)).thenReturn(Future.successful(Some(Stubs.mixtureProfile)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Right(())))
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Right(SuperiorInstanceProfileApproval
        (id = 0L,
          globalCode = profile.globalCode.text,
          profile = Json.toJson(Stubs.mixtureProfile).toString(),
          laboratory = "SHDG",
          laboratoryInstanceOrigin = "SHDG",
          laboratoryImmediateInstance = "SHDG",
          sampleEntryDate = None))))

      when(superiorInstanceProfileApprovalRepository.upsert(anyObject[SuperiorInstanceProfileApproval])).thenReturn(Future.successful(Right(1L)))

      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
      }

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , ws
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.rejectProfile(ProfileApproval(profile.globalCode.text),"motivo",1L), Duration.Inf)
      result.isRight mustBe true
    }

    "rejectProfile with not existeng global code" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]

      when(superiorInstanceProfileApprovalRepository.delete(profileApproval.globalCode)).thenReturn(Future.successful(Right(())))
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode(profileApproval.globalCode))
        .thenReturn(Future.successful(Left("No existe")))

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.rejectProfile(ProfileApproval(profile.globalCode.text),"motivo",1L), Duration.Inf)
      result.isLeft mustBe true
    }
    "validateAnalisis ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val notificationService = mock[NotificationService]
      val categoryRepository = mock[CategoryRepository]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val profile: Profile = Stubs.mixtureProfile
      val profileApproval = ProfileApproval(profile.globalCode.text)
      val profileDataService = mock[ProfileDataService]
      when(profileService.isExistingCategory(profile.categoryId)).thenReturn(true)
      for (analysis <- profile.analyses.get) {
        when(profileService.isExistingKit(analysis.kit)).thenReturn(Future.successful(true))
        when(profileService.validateExistingLocusForKit(any[Profile.Genotypification],any[Option[String]])).thenReturn(Future.successful(Right(())))
        when(profileService.validateExistingLocusForKit(analysis.genotypification,None)).thenReturn(Future.successful(Right(())))
        when(profileService.validateExistingLocusForKit(analysis.genotypification,Some(analysis.kit))).thenReturn(Future.successful(Right(())))
      }
      //      when(profileService.validateExistingLocusForKit(any[Profile.Genotypification],any[Option[String]])).thenReturn(Future.successful(Right(())))

      val interconnectionService = new InterconnectionServiceImpl(akkaSystem,
        connectionRepository,
        inferiorInstanceRepository,
        categoryRepository,
        superiorInstanceProfileApprovalRepository
        , client
        , userService
        , roleService
        , profileService
        , null
        , notificationService
        , protocol
        , status
        , categoryTreeCombo
        , insertConnection
        , localUrl
        , uploadProfile
        , labCode,
        profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.validateAnalysis(profile), Duration.Inf)
      result.forall(x => x.isRight) mustBe true

    }

    "updateUploadStatus ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      when(profileDataService.updateUploadStatus("GLOBALCODE",1L,Option.empty[String],Option.empty[String])).thenReturn(Future.successful(Right(())))
      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository], mock[SuperiorInstanceProfileApprovalRepository], client, null, null, null, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.updateUploadStatus("GLOBALCODE",1L,None), duration)

      result.isRight mustBe true
    }

    "delete superior pendiente de aprobacion ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      when(profileService.findByCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(None))
      val approval = SuperiorInstanceProfileApproval(1L,"","","","","")
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode("AR-C-SHDG-1190")).thenReturn(Future.successful(Right(approval)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Right(())))

      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.receiveDeleteProfile("AR-C-SHDG-1190",DeletedMotive("ahierro", "motivo", 2),"","", true), duration)

      result.isRight mustBe true
    }

    "delete superior pendiente de aprobacion no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      when(profileService.findByCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(None))
      val approval = SuperiorInstanceProfileApproval(1L,"","","","","")
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode("AR-C-SHDG-1190")).thenReturn(Future.successful(Right(approval)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Left("")))

      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.receiveDeleteProfile("AR-C-SHDG-1190",DeletedMotive("ahierro", "motivo", 2),"","", true), duration)

      result.isLeft mustBe true
    }

    "delete superior aprobada ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val approval = SuperiorInstanceProfileApproval(1L,"","","","","")
      val userService = mock[UserService]
      val roleService = mock[RoleService]
      val permissions: Map[String, Set[Permission]] = HashMap("admin" -> Set(Permission.INTERCON_NOTIF))
      when(roleService.getRolePermissions()).thenReturn(permissions)
      when(userService.findUsersIdWithPermission(any[types.Permission])).thenReturn(Future.successful(Seq(Stubs.user).map(_.id)))
      when(cacheService.get(ProfileLabKey("AR-C-SHDG-1190"))).thenReturn(Some("SHDG"))

      when(userService.findUserAssignableByRole("admin")).thenReturn(Future.successful(Seq(Stubs.user)))

      when(superiorInstanceProfileApprovalRepository.findByGlobalCode("AR-C-SHDG-1190")).thenReturn(Future.successful(Right(approval)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Right(())))
      when(profileService.findByCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(Some(Stubs.mixtureP1)))
      when(profileDataService.getExternalProfileDataByGlobalCode(any[String])).thenReturn(Future.successful(Some(ExternalProfileDataRow(1l, "Otro", "Otro"))))
      when(profileDataService.get(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileDataService.deleteProfile(any[SampleCode],any[DeletedMotive],any[String],any[Boolean],any[Boolean])).thenReturn(Future.successful(Right(SampleCode("AR-C-SHDG-1190"))))

      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, userService, roleService, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.receiveDeleteProfile("AR-C-SHDG-1190",DeletedMotive("ahierro", "motivo", 2),"","", true), duration)

      result.isRight mustBe true
    }

    "delete superior aprobada no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      val approval = SuperiorInstanceProfileApproval(1L,"","","","","")
      when(superiorInstanceProfileApprovalRepository.findByGlobalCode("AR-C-SHDG-1190")).thenReturn(Future.successful(Right(approval)))
      when(superiorInstanceProfileApprovalRepository.deleteLogical(any[String],any[Option[String]],any[Option[java.sql.Timestamp]],any[Option[Long]],any[Option[String]])).thenReturn(Future.successful(Right(())))
      when(profileService.findByCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(Some(Stubs.mixtureP1)))
      when(profileDataService.getExternalProfileDataByGlobalCode(any[String])).thenReturn(Future.successful(Some(ExternalProfileDataRow(1l, "Otro", "Otro"))))
      when(profileDataService.get(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(profileDataService.deleteProfile(any[SampleCode],any[DeletedMotive],any[String],any[Boolean],any[Boolean])).thenReturn(Future.successful(Left("Error")))
      when(cacheService.get(ProfileLabKey("AR-C-SHDG-1190"))).thenReturn(Some("SHDG"))
      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.receiveDeleteProfile("AR-C-SHDG-1190",DeletedMotive("ahierro", "motivo", 2L),"","", true), duration)

      result.isLeft mustBe true
    }
    "inferior delete profile" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]

      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      interconnectionService.inferiorDeleteProfile(SampleCode("AR-C-SHDG-1190"),DeletedMotive("solicitor","motive",2L))

    }
    "do inferior delete profile profile not uploaded" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))
      when(profileDataService.getProfileUploadStatusByGlobalCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(None))
      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, client, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.doInferiorDeleteProfile(SampleCode("AR-C-SHDG-1190"),DeletedMotive("solicitor","motive"), ""), duration)
      result.mustBe(())
    }
    "do inferior delete profile profile approved" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))
      val url:String = protocol+"Dummyurl.com"+"/superior/profile/"+"AR-C-SHDG-1190"
      val ws = MockWS {
        case ("DELETE", url) => Action {
          Ok
        }
      }
      when(profileDataService.updateUploadStatus(any[String],any[Long],any[Option[String]],any[Option[String]])).thenReturn(Future.successful(Right(())))

      when(profileDataService.getProfileUploadStatusByGlobalCode(any[SampleCode])).thenReturn(Future.successful(Some(4L)))
      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, ws, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.doInferiorDeleteProfile(SampleCode("AR-C-SHDG-1190"),DeletedMotive("solicitor","motive"), url), duration)
      result.mustBe(())
    }

    "do inferior delete profile profile approved no ok" in {
      val connectionRepository = mock[ConnectionRepository]
      val inferiorInstanceRepository = mock[InferiorInstanceRepository]
      val profileDataService = mock[ProfileDataService]
      val profileService = mock[ProfileService]
      val superiorInstanceProfileApprovalRepository = mock[SuperiorInstanceProfileApprovalRepository]
      when(connectionRepository.getSupInstanceUrl()).thenReturn(Future.successful(Some("Dummyurl.com")))
      val url:String = protocol+"Dummyurl.com"+"/superior/profile/"+"AR-C-SHDG-1190"
      val ws = MockWS {
        case ("DELETE", url) => Action {
          InternalServerError
        }
      }
      when(profileDataService.updateUploadStatus(any[String],any[Long],any[Option[String]],any[Option[String]])).thenReturn(Future.successful(Right(())))

      when(profileDataService.getProfileUploadStatusByGlobalCode(SampleCode("AR-C-SHDG-1190"))).thenReturn(Future.successful(Some(4L)))
      val interconnectionService =  new InterconnectionServiceImpl(akkaSystem,connectionRepository, inferiorInstanceRepository, mock[CategoryRepository],superiorInstanceProfileApprovalRepository, ws, null, null, profileService, null
        , null, protocol, status, categoryTreeCombo, insertConnection, localUrl, uploadProfile, labCode,profileDataService,
        null,
        null,
        null,
        null,
        "tst-admin",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        "1 seconds",
        1000,
        cacheService)

      val result = Await.result(interconnectionService.doInferiorDeleteProfile(SampleCode("AR-C-SHDG-1190"),DeletedMotive("solicitor","motive"),url), duration)
      result.mustBe(())
    }
  }
}