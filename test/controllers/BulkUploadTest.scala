package controllers

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import specs.PdgSpec
import bulkupload.{BulkUploadService, GenotypificationItem, ProtoProfile, ProtoProfileStatus}
import user.UserService
import org.mockito.Matchers.any
import stubs.Stubs
import types.AlphanumericId

import scala.concurrent.Future

class BulkUploadTest extends PdgSpec with MockitoSugar with Results {

  "BulkUpload controller" must {

    "delete batch ok" in {
      val bulkUploadService = mock[BulkUploadService]
      when(bulkUploadService.deleteBatch(1L)).thenReturn(Future.successful(Right(1L)))
      val userService = mock[UserService]
      val target = new BulkUpload(bulkUploadService,userService)
      val result: Future[Result] = target.deleteBatch(1L).apply(FakeRequest())
      status(result) mustBe OK

    }

    "delete batch no ok" in {
      val bulkUploadService = mock[BulkUploadService]
      when(bulkUploadService.deleteBatch(1L)).thenReturn(Future.successful(Left("Not Exist")))

      val userService = mock[UserService]
      val target = new BulkUpload(bulkUploadService,userService)
      val result: Future[Result] = target.deleteBatch(1L).apply(FakeRequest())
      status(result) mustBe BAD_REQUEST

    }

    "update protoprofile ok" in {
      val bulkUploadService = mock[BulkUploadService]
      val geno = Stubs.newProfile.genotypification.flatMap {
        case (at, locusMap) => locusMap.map(x => GenotypificationItem(x._1, x._2)).toList
      }.toList
      val matchingRules = Stubs.listOfMinimumStringencies
      val mismatches = Stubs.mismatches
      val protoProfile = ProtoProfile(0, "sample1", "user1", "CATT", ProtoProfileStatus.ReadyForApproval, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None)
      when(bulkUploadService.updateProtoProfileData(any[Long],any[AlphanumericId],any[String])).thenReturn(Future.successful(Right(protoProfile)))
      val userService = mock[UserService]
      val target = new BulkUpload(bulkUploadService,userService)
      val id:AlphanumericId = AlphanumericId("SOSPECHOSO")
      val result: Future[Result] = target.updateProtoProfileData(1L, id).apply(FakeRequest().withHeaders("X-USER" -> "ahierro"))
      status(result) mustBe OK

    }

    "update protoprofile no ok" in {
      val bulkUploadService = mock[BulkUploadService]
      when(bulkUploadService.updateProtoProfileData(any[Long],any[AlphanumericId],any[String])).thenReturn(Future.successful(Left(Seq("No existe la categoria"))))
      val userService = mock[UserService]
      val target = new BulkUpload(bulkUploadService,userService)
      val id:AlphanumericId = AlphanumericId("SOSPECHOSO")
      val result: Future[Result] = target.updateProtoProfileData(1L, id).apply(FakeRequest().withHeaders("X-USER" -> "ahierro"))
      status(result) mustBe BAD_REQUEST

    }
  }

}
