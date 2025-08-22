package controllers

import connections._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import profiledata.DeletedMotive
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Future

class InterconnectionsTest extends PdgSpec with MockitoSugar with Results {

  val instanceStub = InferiorInstanceFull(id=1,url="",connectivity="", idStatus = 1)
  val instances = List(instanceStub)
  val instancesStatus = List(InferiorInstanceStatus(id=1,description=""))
  val approvalSearch = ProfileApprovalSearch(1,5)
  "Interconnection controller" must {

    "get connections no ok" in {
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.getConnections()).thenReturn(Future.successful(Left("DB Error")))

      val target = new Interconnections(interconnectionService, null)
      val result: Future[Result] = target.getConnections().apply(FakeRequest())
      status(result) mustBe INTERNAL_SERVER_ERROR

    }

    "get connections ok" in {
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.getConnections()).thenReturn(Future.successful(Right(Connection("192.168.0.1:9000", "192.168.0.2:9000"))))

      val target = new Interconnections(interconnectionService, null)
      val result: Future[Result] = target.getConnections().apply(FakeRequest())
      status(result) mustBe OK

    }

    "update connections ok" in {
      val conn = Connection("192.168.0.1:9000", "192.168.0.2:9000")
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.updateConnections(conn)).
        thenReturn(Future.successful(Right(Connection("192.168.0.1:9000", "192.168.0.2:9000"))))

      val target = new Interconnections(interconnectionService, null)
      val jsRequest = Json.obj("superiorInstance" -> "192.168.0.1:9000", "pki" -> "192.168.0.2:9000")

      val request = FakeRequest().withBody(jsRequest)

      val result: Future[Result] = target.updateConnections().apply(request)
      status(result) mustBe OK

    }
    "update connections bad request" in {
      val conn = Connection("192.168.0.1:9000", "192.168.0.2:9000")
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.updateConnections(conn)).
        thenReturn(Future.successful(Right(Connection("192.168.0.1:9000", "192.168.0.2:9000"))))

      val target = new Interconnections(interconnectionService, null)
      val jsRequest = Json.obj("malrequest" -> "192.168.0.1:9000", "pki" -> "192.168.0.2:9000")

      val request = FakeRequest().withBody(jsRequest)

      val result: Future[Result] = target.updateConnections().apply(request)
      status(result) mustBe BAD_REQUEST

    }
    "update connections database error" in {
      val conn = Connection("192.168.0.1:9000", "192.168.0.2:9000")
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.updateConnections(conn)).
        thenReturn(Future.successful(Left("Db Error")))

      val target = new Interconnections(interconnectionService, null)
      val jsRequest = Json.obj("superiorInstance" -> "192.168.0.1:9000", "pki" -> "192.168.0.2:9000")

      val request = FakeRequest().withBody(jsRequest)

      val result: Future[Result] = target.updateConnections().apply(request)
      status(result) mustBe BAD_REQUEST

    }
    "get connection status" in {
      val interconnectionService = mock[InterconnectionService]
      when(interconnectionService.getConnectionsStatus("testok")).
        thenReturn(Future.successful(Right(())))
      when(interconnectionService.getConnectionsStatus("testnook")).
        thenReturn(Future.successful(Left("no ok")))
      val target = new Interconnections(interconnectionService, null)

      val resultOk: Future[Result] = target.getConnectionStatus("testok").apply(FakeRequest())
      status(resultOk) mustBe OK

      val resultNoOk: Future[Result] = target.getConnectionStatus("testnook").apply(FakeRequest())
      status(resultNoOk) mustBe NOT_FOUND
    }
    "get category tree combo ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      val jsValue: JsValue = Json.parse(
        """
          {
          } """)
      when(interconnectionService.getCategoryConsumer).thenReturn(Future.successful(Right(jsValue)))
      val resultOk: Future[Result] = target.getCategoryTreeComboConsumer().apply(FakeRequest())
      status(resultOk) mustBe OK
    }
    "get insertConnection ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.connect).thenReturn(Future.successful(Right()))
      val resultOk: Future[Result] = target.insertConnection().apply(FakeRequest())
      status(resultOk) mustBe OK
    }

    "get insertInferiorInstanceConnection ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.insertInferiorInstanceConnection("a", "SHDG")).thenReturn(Future.successful(Right()))

      val request = FakeRequest().withHeaders("X-URL-INSTANCIA-INFERIOR" -> "a").withHeaders(HeaderInsterconnections.laboratoryImmediateInstance -> "SHDG")

      val resultOk: Future[Result] = target.insertInferiorInstanceConnection().apply(request)
      status(resultOk) mustBe OK
    }
    "get insertInferiorInstanceConnection no ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.insertInferiorInstanceConnection("a", "lab")).thenReturn(Future.successful(Right()))

      val resultOk: Future[Result] = target.insertInferiorInstanceConnection().apply(FakeRequest())
      status(resultOk) mustBe BAD_REQUEST
    }

    "get getInferiorInstancesok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.getAllInferiorInstances()).thenReturn(Future.successful(Right(instances)))

      val resultOk: Future[Result] = target.getInferiorInstances().apply(FakeRequest())
      status(resultOk) mustBe OK
    }

    "get getInferiorInstancesStatus ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.getAllInferiorInstanceStatus()).thenReturn(Future.successful(Right(instancesStatus)))

      val resultOk: Future[Result] = target.getInferiorInstancesStatus().apply(FakeRequest())
      status(resultOk) mustBe OK
    }

    "get updateInferiorInstance ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.updateInferiorInstance(instanceStub)).thenReturn(Future.successful(Right()))
      val jsValue: JsValue = Json.parse(
        """
          {"id":1,"url":"","connectivity":"","idStatus":1,"laboratory":""} """)
      val request = FakeRequest().withBody(jsValue)

      val resultOk: Future[Result] = target.updateInferiorInstance.apply(request)
      status(resultOk) mustBe OK
    }
    "get updateInferiorInstance no ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.updateInferiorInstance(instanceStub)).thenReturn(Future.successful(Right()))
      val jsValue: JsValue = Json.parse(
        """
          {} """)
      val request = FakeRequest().withBody(jsValue)

      val resultOk: Future[Result] = target.updateInferiorInstance.apply(request)
      status(resultOk) mustBe BAD_REQUEST
    }


    "get getPendingProfiles ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      when(interconnectionService.getPendingProfiles(approvalSearch)).thenReturn(Future.successful(Nil))
      val resultOk: Future[Result] = target.getPendingProfiles(1,5).apply(FakeRequest())
      status(resultOk) mustBe OK
    }

    "post importProfile ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)


      val request = FakeRequest().withBody(Json.toJson(ProfileTransfer(Stubs.mixtureProfile,Some(Stubs.mixtureProfile)))).withHeaders(HeaderInsterconnections.labCode -> "")
        .withHeaders(HeaderInsterconnections.sampleEntryDate -> "")
        .withHeaders(HeaderInsterconnections.laboratoryOrigin -> "")
        .withHeaders(HeaderInsterconnections.laboratoryImmediateInstance -> "")
      val resultOk: Future[Result] = target.importProfile().apply(request)
      status(resultOk) mustBe OK
    }

    "post importProfile no ok 1" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)

      val request = FakeRequest().withBody(Json.toJson(Stubs.mixtureProfile)).withHeaders(HeaderInsterconnections.labCode -> "")
        .withHeaders(HeaderInsterconnections.sampleEntryDate -> "")
      val resultOk: Future[Result] = target.importProfile().apply(request)
      status(resultOk) mustBe BAD_REQUEST
    }
    "post importProfile no ok 2" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)

      val jsValue: JsValue = Json.parse(
        """
          {"badrequest":"400"} """)
      val request = FakeRequest().withBody(jsValue).withHeaders(HeaderInsterconnections.labCode -> "")
        .withHeaders(HeaderInsterconnections.sampleEntryDate -> "")
        .withHeaders(HeaderInsterconnections.laboratoryOrigin -> "")
        .withHeaders(HeaderInsterconnections.laboratoryImmediateInstance -> "")
      val resultOk: Future[Result] = target.importProfile().apply(request)
      status(resultOk) mustBe BAD_REQUEST
    }
    "post approveProfiles ok" in {

      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      val requestObj: List[ProfileApproval] = Nil
      when(interconnectionService.approveProfiles(requestObj, "test-user")).thenReturn(Future.successful(Right(())))

      val request = FakeRequest().withBody(Json.toJson(requestObj))

      val resultOk: Future[Result] = target.approveProfiles("test-user").apply(request)
      status(resultOk) mustBe OK

    }

    "post approveProfiles no ok" in {

      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      val requestObj: List[ProfileApproval] = Nil
      when(interconnectionService.approveProfiles(requestObj, "test-user")).thenReturn(Future.successful(Left("error")))

      val request = FakeRequest().withBody(Json.toJson(requestObj))

      val resultOk: Future[Result] = target.approveProfiles("test-user").apply(request)
      status(resultOk) mustBe BAD_REQUEST

    }

    "post approveProfiles no ok2" in {

      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)

      val requestObj: JsValue = Json.parse(
        """
          {"badrequest":"400"} """)

      val request = FakeRequest().withBody(Json.toJson(requestObj))

      val resultOk: Future[Result] = target.approveProfiles("test-user").apply(request)
      status(resultOk) mustBe BAD_REQUEST

    }
    "post uploadProfile ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      val globalCode = ""
      when(interconnectionService.uploadProfile(globalCode)).thenReturn(Future.successful(Right(())))

      val request = FakeRequest()

      val resultOk: Future[Result] = target.uploadProfile(globalCode).apply(request)
      status(resultOk) mustBe OK
    }

    "delete rejectPendingProfile ok" in {
      val interconnectionService = mock[InterconnectionService]
      val target = new Interconnections(interconnectionService, null)
      val globalCode = ""
      when(interconnectionService.rejectProfile(ProfileApproval(globalCode), "motive", 1L, "usuario")).thenReturn(Future.successful(Right(())))

      val request = FakeRequest()

      val resultOk: Future[Result] = target.rejectPendingProfile(globalCode, "motive", 1L, "usuario", false).apply(request)
      status(resultOk) mustBe OK

    }
  }
}
