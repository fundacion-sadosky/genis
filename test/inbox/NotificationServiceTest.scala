package inbox

import java.util.Date

import inbox._
import org.mockito.ArgumentCaptor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.iteratee.Iteratee
import specs.PdgSpec
import play.api.libs.concurrent.Akka

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import scenarios.CalculationScenario
import types.SampleCode


class NotificationServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)
  
  val sc = stubs.Stubs.sampleCode
  val user = "userId"
  val notiInfo: NotificationInfo = ProfileDataInfo("", sc)
  val noti: Notification = Notification(0, user, new Date(), None, false, false, notiInfo)
  val notiSearch: NotificationSearch = NotificationSearch(0,30,user,Some(false),Some(true))

  "A NotificationService" must {
    "push a notification" in {
      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.add(any[Notification])).thenReturn(Future.successful(Right(1234l)))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)
      service.push(user, notiInfo)
      Thread.sleep(1000)

      val captor = ArgumentCaptor.forClass(classOf[Notification])
      verify(notificationRepository).add(captor.capture())
      val pushed = captor.getValue

      pushed.id mustBe 0
      pushed.info mustBe notiInfo
      pushed.user mustBe user
      pushed.pending mustBe true
      pushed.flagged mustBe false
    }

    "solve a notification" in {
      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.get(any[String], any[String], any[String])).thenReturn(Future.successful(Seq(noti)))
      when(notificationRepository.update(any[Notification])).thenReturn(Future.successful(Right(1234l)))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)
      service.solve(user, notiInfo)
      Thread.sleep(1000)

      val captor = ArgumentCaptor.forClass(classOf[Notification])
      verify(notificationRepository).update(captor.capture())
      val solved = captor.getValue

      solved.id mustBe noti.id
      solved.info mustBe noti.info
      solved.user mustBe noti.user
      solved.pending mustBe false
      solved.flagged mustBe noti.flagged
    }

    "solve an unexisting notification" in {
      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.get(any[String], any[String], any[String])).thenReturn(Future.successful(Seq()))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)

      service.solve(user, MatchingInfo(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-2"), "12"))
      Thread.sleep(1000)

      verify(notificationRepository, times(0)).update(any[Notification])
    }

    /* no funciona
    "feed notifications for a user" in {
      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.add(any[Notification])).thenReturn(Future.successful(Right(1234l)))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)
      Await.result(service.push(user, notiInfo), duration)

      val ret = service.getNotifications(user)

      val acc = ret |>>> Iteratee.head[Notification]

      val pdn = Await.result(acc, duration)

      val ff = pdn.get

      ff.kind mustBe NotificationType.profileData
      ff.user mustBe user
      ff.url mustBe "/profile/AR-B-LAB-1"
    }
    */

    "search notifications" in {
      val mockResult = IndexedSeq(noti)

      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.search(any[NotificationSearch])).thenReturn(Future.successful(mockResult))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)

      val result = Await.result(service.search(notiSearch), duration)

      result mustBe mockResult
    }

    "count notifications" in {
      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.count(any[NotificationSearch])).thenReturn(Future.successful(5))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)

      val result = Await.result(service.count(notiSearch), duration)

      result mustBe 5
    }

    "delete a notification" in {
      val mockResult = Right(noti.id)

      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.delete(any[Long])).thenReturn(Future.successful(mockResult))
      when(notificationRepository.getById(any[Long])).thenReturn(Future.successful(
        Some(Notification(1L,"user",new Date(),None,false,false,DeleteProfileInfo(SampleCode("AR-C-SHDG-1148"))))))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)

      val result = Await.result(service.delete(noti.id), duration)

      result mustBe mockResult
    }

    "change a notification flag" in {
      val mockResult = Right(noti.id)

      val notificationRepository = mock[NotificationRepository]
      when(notificationRepository.changeFlag(any[Long], any[Boolean])).thenReturn(Future.successful(mockResult))

      val service = new NotificationServiceImpl(Akka.system, notificationRepository)

      val result = Await.result(service.changeFlag(noti.id, true), duration)

      result mustBe mockResult
    }

  }



}