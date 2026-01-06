package controllers

import java.util.Date

import inbox.{Notification, NotificationSearch, NotificationService, UserPendingInfo}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import specs.PdgSpec
import play.api.test.Helpers._

import scala.concurrent.Future

class NotificationsTest extends PdgSpec with MockitoSugar with Results {

  val id = 1234l
  val sc = stubs.Stubs.sampleCode

  val noti: Notification = Notification(id, "userId", new Date(), None, true, true, UserPendingInfo("userId"))

  "Notifications controller" must {
    "delete a notification - ok" in {

      val notificationService = mock[NotificationService]
      when(notificationService.delete(any[Long])).thenReturn(Future.successful(Right(id)))

      val target = new Notifications(notificationService, null)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe OK
    }
    "delete a notification - bad request" in {

      val notificationService = mock[NotificationService]
      when(notificationService.delete(any[Long])).thenReturn(Future.successful(Left("Error")))

      val target = new Notifications(notificationService, null)
      val result: Future[Result] = target.delete(id).apply(FakeRequest())

      status(result) mustBe BAD_REQUEST
    }
    "change flag - ok" in {

      val notificationService = mock[NotificationService]
      when(notificationService.changeFlag(any[Long], any[Boolean])).thenReturn(Future.successful(Right(id)))

      val target = new Notifications(notificationService, null)
      val result: Future[Result] = target.changeFlag(id, true).apply(FakeRequest())

      status(result) mustBe OK
    }
    "change flag - bad request" in {

      val notificationService = mock[NotificationService]
      when(notificationService.changeFlag(any[Long], any[Boolean])).thenReturn(Future.successful(Left("Error")))

      val target = new Notifications(notificationService, null)
      val result: Future[Result] = target.changeFlag(id, true).apply(FakeRequest())

      status(result) mustBe BAD_REQUEST
    }
    "search - ok" in {
      val notiSearch = NotificationSearch(0, 30, "pdg")

      val notificationService = mock[NotificationService]
      when(notificationService.search(any[NotificationSearch])).thenReturn(Future.successful(Seq(noti)))

      val target = new Notifications(notificationService, null)

      val jsRequest = Json.toJson(notiSearch)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe OK
    }
    "search - bad request" in {
      val notificationService = mock[NotificationService]
      when(notificationService.search(any[NotificationSearch])).thenReturn(Future.successful(Seq(noti)))

      val target = new Notifications(notificationService, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.search().apply(request)

      status(result) mustBe BAD_REQUEST
    }
    "count - ok" in {
      val notiSearch = NotificationSearch(0, 30, "pdg")

      val notificationService = mock[NotificationService]
      when(notificationService.count(any[NotificationSearch])).thenReturn(Future.successful(4))

      val target = new Notifications(notificationService, null)

      val jsRequest = Json.toJson(notiSearch)
      val request = FakeRequest().withBody(jsRequest)
      val result: Future[Result] = target.count().apply(request)

      status(result) mustBe OK
      header("X-NOTIF-LENGTH", result).get mustBe "4"
    }
    "count - bad request" in {
      val notificationService = mock[NotificationService]
      when(notificationService.count(any[NotificationSearch])).thenReturn(Future.successful(4))

      val target = new Notifications(notificationService, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = target.count().apply(request)

      status(result) mustBe BAD_REQUEST
    }

  }

}
