package unit.profile

import fixtures.StubProfileService
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import profile.*
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.{Duration, SECONDS}

class ProfileServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  val timeout = Duration(10, SECONDS)
  val sc = SampleCode("AR-B-IMBICE-1")
  val catId = AlphanumericId("SOSPECHOSO")
  val genot: Profile.Genotypification = Map("CSF1PO" -> List(AlleleValue("10"), AlleleValue("12")))
  val genotByType: GenotypificationByType.GenotypificationByType = Map(1 -> genot)

  val sampleProfile = Profile(
    _id = sc, globalCode = sc, internalSampleCode = "ISC-001",
    assignee = "user1", categoryId = catId, genotypification = genotByType,
    analyses = None, labeledGenotypification = None, contributors = Some(1),
    mismatches = None, deleted = false, matcheable = true, isReference = false, processed = false
  )

  "ProfileService (stub)" must {

    "findByCode returns profile when exists" in {
      val service = new StubProfileService
      val result = Await.result(service.findByCode(sc), timeout)
      result mustBe defined
      result.get.globalCode mustBe sc
    }

    "findByCode returns None when not found" in {
      val service = new StubProfileService
      service.findByCodeResult = Future.successful(None)
      val result = Await.result(service.findByCode(sc), timeout)
      result mustBe empty
    }

    "findByCodes returns profiles" in {
      val service = new StubProfileService
      val result = Await.result(service.findByCodes(List(sc)), timeout)
      result must not be empty
      result.head.globalCode mustBe sc
    }

    "create returns Right with profile on success" in {
      val service = new StubProfileService
      val na = NewAnalysis(sc, "user1", "token", genotypification = genot,
        labeledGenotypification = None, contributors = Some(1), mismatches = None)
      val result = Await.result(service.create(na), timeout)
      result mustBe a[Right[?, ?]]
      result.toOption.get.globalCode mustBe sc
    }

    "create returns Left with errors on failure" in {
      val service = new StubProfileService
      service.createResult = Future.successful(Left(List("validation error")))
      val na = NewAnalysis(sc, "user1", "token", genotypification = genot,
        labeledGenotypification = None, contributors = Some(1), mismatches = None)
      val result = Await.result(service.create(na), timeout)
      result mustBe a[Left[?, ?]]
      result.left.toOption.get must contain("validation error")
    }

    "getProfileModelView returns view" in {
      val service = new StubProfileService
      val result = Await.result(service.getProfileModelView(sc), timeout)
      result.globalCode mustBe Some(sc)
      result.associable mustBe true
    }

    "getElectropherogramsByCode returns list" in {
      val service = new StubProfileService
      val result = Await.result(service.getElectropherogramsByCode(sc), timeout)
      result must have length 1
      result.head.fileId mustBe "epg1"
    }

    "getElectropherogramImage returns bytes when found" in {
      val service = new StubProfileService
      val result = Await.result(service.getElectropherogramImage(sc, "epg1"), timeout)
      result mustBe defined
      result.get must have length 3
    }

    "getElectropherogramImage returns None when not found" in {
      val service = new StubProfileService
      service.getEpgImageResult = Future.successful(None)
      val result = Await.result(service.getElectropherogramImage(sc, "missing"), timeout)
      result mustBe empty
    }

    "saveLabels returns Right on success" in {
      val service = new StubProfileService
      val labels: Profile.LabeledGenotypification = Map("V" -> Map("CSF1PO" -> List(AlleleValue("10"))))
      val result = Await.result(service.saveLabels(sc, labels, "user1"), timeout)
      result mustBe Right(sc)
    }

    "saveLabels returns Left on failure" in {
      val service = new StubProfileService
      service.saveLabelsResult = Future.successful(Left(List("error")))
      val labels: Profile.LabeledGenotypification = Map("V" -> Map("CSF1PO" -> List(AlleleValue("10"))))
      val result = Await.result(service.saveLabels(sc, labels, "user1"), timeout)
      result mustBe a[Left[?, ?]]
    }

    "getLabelsSets returns configured label sets" in {
      val service = new StubProfileService
      val sets = service.getLabelsSets()
      sets must contain key "set1"
    }

    "getLabels returns None when no labels" in {
      val service = new StubProfileService
      val result = Await.result(service.getLabels(sc), timeout)
      result mustBe empty
    }

    "isReadOnlySampleCode returns tuple" in {
      val service = new StubProfileService
      val result = Await.result(service.isReadOnlySampleCode(sc), timeout)
      result._1 mustBe false
    }

    "getFilesByCode returns file list" in {
      val service = new StubProfileService
      val result = Await.result(service.getFilesByCode(sc), timeout)
      result mustBe a[List[?]]
    }

    "getFile returns bytes when found" in {
      val service = new StubProfileService
      val result = Await.result(service.getFile(sc, "f1"), timeout)
      result mustBe defined
    }

    "removeFile returns Right on success" in {
      val service = new StubProfileService
      val result = Await.result(service.removeFile("f1", "user1"), timeout)
      result mustBe Right("ok")
    }

    "removeEpg returns Right on success" in {
      val service = new StubProfileService
      val result = Await.result(service.removeEpg("e1", "user1"), timeout)
      result mustBe Right("ok")
    }

    "removeProfile returns Right on success" in {
      val service = new StubProfileService
      val result = Await.result(service.removeProfile(sc), timeout)
      result mustBe Right("ok")
    }

    "removeProfile returns Left on failure" in {
      val service = new StubProfileService
      service.removeProfileResult = Future.successful(Left("cannot delete"))
      val result = Await.result(service.removeProfile(sc), timeout)
      result mustBe Left("cannot delete")
    }

    "saveElectropherograms returns list of results" in {
      val service = new StubProfileService
      val result = Await.result(service.saveElectropherograms("t", sc, "a1", "test"), timeout)
      result must have length 1
      result.head mustBe Right(sc)
    }

    "saveFile returns list of results" in {
      val service = new StubProfileService
      val result = Await.result(service.saveFile("t", sc, "a1", "test"), timeout)
      result must have length 1
      result.head mustBe Right(sc)
    }

    "verifyMixtureAssociation returns Right on match" in {
      val service = new StubProfileService
      val result = Await.result(
        service.verifyMixtureAssociation(Map(1 -> genot), sc, AlphanumericId("SOSPECHOSO")),
        timeout
      )
      result mustBe a[Right[?, ?]]
    }
  }
}
