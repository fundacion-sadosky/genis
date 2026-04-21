package unit.bulkupload

import bulkupload.*
import configdata.{CategoryRepository, CategoryService, MatchingRule}
import connections.InterconnectionService
import inbox.NotificationService
import kits.StrKitService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.stubMessagesApi
import profile.{Profile, ProfileService}
import profiledata.{ImportToProfileData, ProfileDataRepository, ProfileDataService}
import services.{CacheService, UserService}
import types.AlphanumericId
import user.UserView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

class BulkUploadServiceTest extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures:

  given PatienceConfig = PatienceConfig(timeout = 10.seconds)

  private def makeService(
    protoRepo: ProtoProfileRepository = mock[ProtoProfileRepository],
    userService: UserService          = mock[UserService],
    categoryService: CategoryService  = mock[CategoryService]
  ): BulkUploadServiceImpl =
    BulkUploadServiceImpl(
      protoRepo             = protoRepo,
      userService           = userService,
      kitService            = mock[StrKitService],
      categoryRepo          = mock[CategoryRepository],
      profileService        = mock[ProfileService],
      protoProfiledataService = mock[ProfileDataService],
      profileDataRepo       = mock[ProfileDataRepository],
      notificationService   = mock[NotificationService],
      importToProfileData   = mock[ImportToProfileData],
      labCode               = "SHDG",
      country               = "AR",
      province              = "C",
      ppGcD                 = "XX-X-XXXX-",
      categoryService       = categoryService,
      cache                 = mock[CacheService],
      interconnectionService = mock[InterconnectionService],
      messagesApi           = stubMessagesApi()
    )

  private def aProtoProfile(
    id: Long                       = 1L,
    assignee: String               = "geneticist1",
    status: ProtoProfileStatus.Value = ProtoProfileStatus.ReadyForApproval
  ): ProtoProfile =
    ProtoProfile(id, "sample1", assignee, "CAT", status, "kit", Nil, Map.empty, Nil, Nil, "", None)

  "BulkUploadService.deleteBatch" must {

    "return Right when batch has no imported samples and delete succeeds" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Right(0L)))
      when(repo.deleteBatch(1L)).thenReturn(Future.successful(Right(1L)))

      makeService(protoRepo = repo).deleteBatch(1L).futureValue mustBe Right(1L)
    }

    "return Left when batch has imported samples (cannot delete)" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Right(1L)))

      val result = makeService(protoRepo = repo).deleteBatch(1L).futureValue
      result.isLeft mustBe true
    }

    "return Left when countImportedProfilesByBatch returns Left (DB error)" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Left("DB error")))

      makeService(protoRepo = repo).deleteBatch(1L).futureValue.isLeft mustBe true
    }
  }

  "BulkUploadService.getBatchSearchModalViewByIdOrLabel" must {

    "return Nil immediately for empty input without calling repository" in {
      val repo = mock[ProtoProfileRepository]
      makeService(protoRepo = repo).getBatchSearchModalViewByIdOrLabel("", 1L).futureValue mustBe Nil
      verifyNoInteractions(repo)
    }

    "delegate to repository for non-empty input" in {
      val expected = List(BatchModelView(42L, Some("Lote 2"), "01/01/2025"))
      val repo = mock[ProtoProfileRepository]
      when(repo.getBatchSearchModalViewByIdOrLabel("term", 5L)).thenReturn(Future.successful(expected))

      makeService(protoRepo = repo).getBatchSearchModalViewByIdOrLabel("term", 5L).futureValue mustBe expected
    }
  }

  "BulkUploadService.updateProtoProfileRulesMismatch" must {

    "return true when repository updates exactly one row" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.updateProtoProfileMatchingRulesMismatch(any(), any(), any())).thenReturn(Future.successful(1))

      makeService(protoRepo = repo)
        .updateProtoProfileRulesMismatch(1L, Seq.empty, Map.empty)
        .futureValue mustBe true
    }

    "return false when repository updates zero rows" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.updateProtoProfileMatchingRulesMismatch(any(), any(), any())).thenReturn(Future.successful(0))

      makeService(protoRepo = repo)
        .updateProtoProfileRulesMismatch(1L, Seq.empty, Map.empty)
        .futureValue mustBe false
    }
  }

  "BulkUploadService.updateProtoProfileStatus" must {

    "return an error when the proto profile does not exist" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.getProtoProfile(99L)).thenReturn(Future.successful(None))

      makeService(protoRepo = repo)
        .updateProtoProfileStatus(99L, ProtoProfileStatus.Approved, "user1", replicate = false, desktopSearch = false)
        .futureValue must not be empty
    }

    "return an error when the assignee is not found by geneMapperId" in {
      val repo = mock[ProtoProfileRepository]
      when(repo.getProtoProfile(1L)).thenReturn(Future.successful(Some(aProtoProfile())))

      val us = mock[UserService]
      when(us.findByGeneMapper("geneticist1")).thenReturn(Future.successful(None))

      makeService(protoRepo = repo, userService = us)
        .updateProtoProfileStatus(1L, ProtoProfileStatus.Approved, "user1", replicate = false, desktopSearch = false)
        .futureValue must not be empty
    }
  }

  "BulkUploadService.rejectProtoProfile" must {

    "propagate transition errors when updateProtoProfileStatus fails (profile not found)" in {
      val repo = mock[ProtoProfileRepository]
      // profile not found → updateProtoProfileStatus returns error E0105
      when(repo.getProtoProfile(99L)).thenReturn(Future.successful(None))

      val result = makeService(protoRepo = repo)
        .rejectProtoProfile(99L, "motive", "user1", 1L)
        .futureValue

      result must not be empty
    }

    "invoke setRejectMotive and return Nil when rejection succeeds" in {
      val repo = mock[ProtoProfileRepository]
      // Approved → Rejected is an allowed transition
      val pp   = aProtoProfile(status = ProtoProfileStatus.Approved)
      when(repo.getProtoProfile(1L)).thenReturn(Future.successful(Some(pp)))

      val us = mock[UserService]
      val geneticist = UserView("geneticist1", "Gene", "Ticist", "g@test.com", Seq.empty,
        user.UserStatus.active, "geneticist1", "")
      when(us.findByGeneMapper("geneticist1")).thenReturn(Future.successful(Some(geneticist)))

      // updateProtoProfileStatus → updateStatus → updateProtoProfileStatus(id, Rejected)
      when(repo.updateProtoProfileStatus(1L, ProtoProfileStatus.Rejected)).thenReturn(Future.successful(1))
      when(repo.setRejectMotive(any(), any(), any(), any(), any())).thenReturn(Future.successful(1))

      makeService(protoRepo = repo, userService = us)
        .rejectProtoProfile(1L, "motive", "user1", 1L)
        .futureValue mustBe Nil
    }
  }
