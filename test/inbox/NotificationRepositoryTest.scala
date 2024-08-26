package inbox

import java.util.Date

import org.apache.commons.lang.time.DateUtils
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration._


class NotificationRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val sc = stubs.Stubs.sampleCode

  val notiInfo: NotificationInfo = ProfileDataInfo("", sc)
  val noti: Notification = Notification(0, "userId", new Date(), None, true, true, notiInfo)

  "A NotificationRepository" must {
    "add a notification" in {
      val repository = new SlickNotificationRepository()

      val result = Await.result(repository.add(noti), duration)

      result.isRight mustBe true
      result.right.get must not be 0

      Await.result(repository.delete(result.right.get), duration)
    }

    "delete a notification" in {
      val repository = new SlickNotificationRepository()

      val id = Await.result(repository.add(noti), duration).right.get
      val result = Await.result(repository.delete(id), duration)

      result.isRight mustBe true
      result.right.get mustBe id
    }

    "get a pending notification" in {
      val repository = new SlickNotificationRepository()

      val id = Await.result(repository.add(noti), duration).right.get

      val result = Await.result(repository.get(noti.user, Json.toJson(noti.info).toString, noti.kind.toString), duration)

      result.nonEmpty mustBe true
      result.head.id mustBe id

      Await.result(repository.delete(id), duration)
    }

    "get a non existing notification" in {
      val repository = new SlickNotificationRepository()

      val result = Await.result(repository.get(noti.user, Json.toJson(noti.info).toString, noti.kind.toString), duration)

      result mustBe List()
    }

    "update a notification" in {
      val repository = new SlickNotificationRepository()

      val id = Await.result(repository.add(noti), duration).right.get

      val updated = Notification(id, noti.user, noti.creationDate, Some(new Date()), noti.flagged, noti.pending, noti.info)
      val resultUpdate = Await.result(repository.update(updated), duration)

      val resultGet = Await.result(repository.get(noti.user, Json.toJson(noti.info).toString, noti.kind.toString), duration)

      resultUpdate mustBe Right(id)
      resultGet.nonEmpty mustBe true
      resultGet.head.updateDate mustBe updated.updateDate

      Await.result(repository.delete(id), duration)
    }

    "change a notification flag" in {
      val repository = new SlickNotificationRepository()

      val id = Await.result(repository.add(noti), duration).right.get

      val resultUpdate = Await.result(repository.changeFlag(id, false), duration)
      val resultGet = Await.result(repository.get(noti.user, Json.toJson(noti.info).toString, noti.kind.toString), duration)

      resultUpdate mustBe Right(id)
      resultGet.nonEmpty mustBe true
      resultGet.head.flagged mustBe false

      Await.result(repository.delete(id), duration)
    }

  }

  def previousSettings(notiRepo: NotificationRepository) = {
    val noti1: Notification = Notification(0, "pdg", DateUtils.addDays(new Date(), -1), None, false, false, notiInfo)
    val noti2: Notification = Notification(0, "pdg", new Date(), None, false, true, UserPendingInfo("userPending"))
    val noti3: Notification = Notification(0, "pdg", new Date(), None, true, true, UserPendingInfo("userPending"))
    val noti4: Notification = Notification(0, "userId", DateUtils.addDays(new Date(), 1), None, true, true, notiInfo)

    val id1 = Await.result(notiRepo.add(noti1), duration).right.get
    val id2 = Await.result(notiRepo.add(noti2), duration).right.get
    val id3 = Await.result(notiRepo.add(noti3), duration).right.get
    val id4 = Await.result(notiRepo.add(noti4), duration).right.get

    List(id1,id2,id3,id4)
  }

  def afterSettings(ids: List[Long], notiRepo: NotificationRepository) = {
    ids.foreach(n => Await.result(notiRepo.delete(n),duration))
  }

  "A NotificationRepository" should {
    "count notifications by filters" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val result = Await.result(repository.count(NotificationSearch(0,30, "pdg")), duration)
      result mustBe 3

      afterSettings(ids, repository)
    }
    "filter by user" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(NotificationSearch(0,30, "pdg")), duration)
      val noResults = Await.result(repository.search(NotificationSearch(0,30, "fakeUser")), duration)

      results.size mustBe 3
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }
    "filter by flagged" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val flagged = Await.result(repository.search(NotificationSearch(0,30, "pdg", Some(true))), duration)
      val unflagged = Await.result(repository.search(NotificationSearch(0,30, "pdg", Some(false))), duration)

      flagged.size mustBe 1
      unflagged.size mustBe 2

      afterSettings(ids, repository)
    }
    "filter by pending" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val pending = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, Some(true))), duration)
      val solved = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, Some(false))), duration)

      pending.size mustBe 2
      solved.size mustBe 1

      afterSettings(ids, repository)
    }
    "filter by hour from" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, Some(DateUtils.addDays(new Date(), -2)))), duration)
      val noResults = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, Some(DateUtils.addDays(new Date(), 2)))), duration)

      results.size mustBe 3
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }
    "filter by hour until" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, None, Some(DateUtils.addDays(new Date(), 2)))), duration)
      val noResults = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, None, Some(DateUtils.addDays(new Date(), -2)))), duration)

      results.size mustBe 3
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }
    "filter by kind" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val results = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, None, None, Some(NotificationType.userNotification))), duration)
      val noResults = Await.result(repository.search(NotificationSearch(0,30, "pdg", None, None, None, None, Some(NotificationType.bulkImport))), duration)

      results.size mustBe 2
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }
    "use pagination to return notifications" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val result = Await.result(repository.search(NotificationSearch(0,2, "pdg")), duration)
      result.size mustBe 2

      afterSettings(ids, repository)
    }
    "sort results by sort field and ascending value" in {
      val repository = new SlickNotificationRepository()
      val ids = previousSettings(repository)

      val filters = NotificationSearch(0, 30, "pdg", None, None, None, None, None, Some(true), Some("date"))
      val result = Await.result(repository.search(filters), duration)

      result(0).creationDate mustBe <= (result(1).creationDate)

      afterSettings(ids, repository)
    }

  }



}
