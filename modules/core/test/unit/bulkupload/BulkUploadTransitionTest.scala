package unit.bulkupload

import bulkupload.*
import configdata.{CategoryRepository, CategoryService}
import connections.InterconnectionService
import inbox.NotificationService
import kits.StrKitService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.Helpers.stubMessagesApi
import profile.ProfileService
import profiledata.{ImportToProfileData, ProfileDataRepository, ProfileDataService}
import services.{CacheService, UserService}
import user.{UserStatus, UserView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

// #218 — matriz de transiciones de estado de proto-perfiles (allowTransition es private; se ejercita
// via updateProtoProfileStatus). Propiedad de seguridad forense: toda transicion NO permitida -> E0104.
class BulkUploadTransitionTest extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures:

  given PatienceConfig = PatienceConfig(timeout = 10.seconds)

  import ProtoProfileStatus.*

  // Debe espejar BulkUploadServiceImpl.allowTransition (13 pares permitidos).
  private val allowed: Set[(ProtoProfileStatus.Value, ProtoProfileStatus.Value)] = Set(
    (Incomplete, ReadyForApproval), (ReadyForApproval, ReadyForApproval),
    (Incomplete, Disapproved), (ReadyForApproval, Approved), (ReadyForApproval, Disapproved),
    (Approved, Imported), (Approved, DesktopSearch), (Approved, Rejected),
    (Imported, Uploaded), (Uploaded, Imported), (Imported, ReplicatedMatchingProfile),
    (Uploaded, ReplicatedMatchingProfile), (ReplicatedMatchingProfile, Uploaded)
  )

  // Permitidas cuyo unico side-effect es updateStatus (testeables sin mockear import/replicacion).
  private val simpleAllowed: Set[(ProtoProfileStatus.Value, ProtoProfileStatus.Value)] = Set(
    (Incomplete, ReadyForApproval), (ReadyForApproval, ReadyForApproval),
    (Incomplete, Disapproved), (ReadyForApproval, Approved), (ReadyForApproval, Disapproved),
    (Approved, Rejected), (Imported, ReplicatedMatchingProfile), (Uploaded, ReplicatedMatchingProfile)
  )

  private val geneticist =
    UserView("geneticist1", "Gene", "Ticist", "g@test.com", Seq.empty, UserStatus.active, "geneticist1", "")

  private def aProtoProfile(status: ProtoProfileStatus.Value): ProtoProfile =
    ProtoProfile(1L, "sample1", "geneticist1", "CAT", status, "kit", Nil, Map.empty, Nil, Nil, "", None)

  private def serviceFor(from: ProtoProfileStatus.Value, updateOk: Boolean): BulkUploadServiceImpl =
    val repo = mock[ProtoProfileRepository]
    when(repo.getProtoProfile(1L)).thenReturn(Future.successful(Some(aProtoProfile(from))))
    if updateOk then
      when(repo.updateProtoProfileStatus(any[Long](), any[ProtoProfileStatus.Value]())).thenReturn(Future.successful(1))
    val us = mock[UserService]
    when(us.findByGeneMapper("geneticist1")).thenReturn(Future.successful(Some(geneticist)))
    BulkUploadServiceImpl(
      protoRepo               = repo,
      userService             = us,
      kitService              = mock[StrKitService],
      categoryRepo            = mock[CategoryRepository],
      profileService          = mock[ProfileService],
      protoProfiledataService = mock[ProfileDataService],
      profileDataRepo         = mock[ProfileDataRepository],
      notificationService     = mock[NotificationService],
      importToProfileData     = mock[ImportToProfileData],
      labCode                 = "SHDG",
      country                 = "AR",
      province                = "C",
      ppGcD                   = "XX-X-XXXX-",
      categoryService         = mock[CategoryService],
      cache                   = mock[CacheService],
      interconnectionService  = mock[InterconnectionService],
      messagesApi             = stubMessagesApi()
    )

  "allowTransition — transiciones NO permitidas (propiedad de seguridad)" must {
    for
      from <- ProtoProfileStatus.values.toSeq
      to   <- ProtoProfileStatus.values.toSeq
      if !allowed((from, to))
    do
      s"rechazar $from -> $to con E0104" in {
        val result = serviceFor(from, updateOk = false)
          .updateProtoProfileStatus(1L, to, "user1").futureValue
        result.exists(_.contains("E0104")) mustBe true
      }
  }

  "allowTransition — transiciones permitidas simples" must {
    for (from, to) <- simpleAllowed.toSeq do
      s"permitir $from -> $to (sin E0104)" in {
        val result = serviceFor(from, updateOk = true)
          .updateProtoProfileStatus(1L, to, "user1").futureValue
        result.exists(_.contains("E0104")) mustBe false
      }
  }
