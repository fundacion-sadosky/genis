package unit.trace

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import configdata.{CategoryAssociation, CategoryService}
import fixtures.TraceFixtures
import kits.AnalysisTypeService
import play.api.i18n.DefaultMessagesApi
import profile.ProfileRepository
import trace.*
import types.{AlphanumericId, SampleCode}

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

/**
 * Gap G3 / Q-I1 (#214 review): getFullDescription must DEGRADE (not throw) when the
 * referenced profile does not exist. `stringify(MatchActionInfo)` and
 * `stringify(AssociationInfo)` previously called `profileOpt.get`, which failed the
 * Future with NoSuchElementException → HTTP 500. After the Q-I1 fix they fall back to a
 * "(no disponible)" description so the audit screen stays usable for traces that point
 * to deleted/old profile codes.
 */
class TraceServiceErrorPathSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private val duration    = Duration(10, SECONDS)
  private val messagesApi = new DefaultMessagesApi()

  private def makeService(
    repo:        TraceRepository,
    catSvc:      CategoryService     = null,
    atSvc:       AnalysisTypeService = null,
    profileRepo: ProfileRepository   = null
  ): TraceServiceImpl =
    new TraceServiceImpl(repo, catSvc, atSvc, profileRepo, null, messagesApi)

  "TraceServiceImpl.getFullDescription (Q-I1)" must {

    "degrade to a '(no disponible)' description for HitInfo (MatchActionInfo) when the profile is missing" in {
      val atSvc = mock[AnalysisTypeService]
      when(atSvc.getById(any[Int])).thenReturn(Future.successful(Some(TraceFixtures.analysisType)))
      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCode(any[SampleCode])).thenReturn(Future.successful(None))
      val repo = mock[TraceRepository]
      when(repo.getById(any[Long])).thenReturn(Future.successful(Some(TraceFixtures.hitTrace)))

      val result = Await.result(
        makeService(repo, atSvc = atSvc, profileRepo = profileRepo).getFullDescription(1L), duration)

      result must include("no disponible")
      result must include(TraceFixtures.hitInfo.profile.text)
    }

    "degrade to a '(no disponible)' description for AssociationInfo when the profile is missing" in {
      val profileRepo = mock[ProfileRepository]
      when(profileRepo.findByCode(any[SampleCode])).thenReturn(Future.successful(None))
      val assoc = AssociationInfo(TraceFixtures.sampleCode2, "userId",
        Seq(CategoryAssociation(1, AlphanumericId("VICTIMA"), 0)))
      val catSvc = mock[CategoryService]
      when(catSvc.getCategory(any[AlphanumericId])).thenReturn(Future.successful(None))
      val repo = mock[TraceRepository]
      when(repo.getById(any[Long]))
        .thenReturn(Future.successful(Some(Trace(TraceFixtures.sampleCode, "userId", new Date(), assoc))))

      val result = Await.result(
        makeService(repo, catSvc = catSvc, profileRepo = profileRepo).getFullDescription(1L), duration)

      result must include("no disponible")
      result must include(TraceFixtures.sampleCode2.text)
    }
  }
}
