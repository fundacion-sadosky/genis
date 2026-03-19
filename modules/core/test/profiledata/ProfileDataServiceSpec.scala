package profiledata

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import types.{AlphanumericId, SampleCode}
import models.{ProfileReceivedRow, ProfileUploadedRow}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class ProfileDataServiceSpec extends PlaySpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val sampleCode = SampleCode("AR-B-SIGE-1")

  val testProfile = ProfileData(
    category             = AlphanumericId("CATX"),
    globalCode           = sampleCode,
    attorney             = None,
    bioMaterialType      = None,
    court                = None,
    crimeInvolved        = None,
    crimeType            = None,
    criminalCase         = None,
    internalSampleCode   = "INT-001",
    assignee             = "gen1",
    laboratory           = "SIGE",
    deleted              = false,
    deletedMotive        = None,
    responsibleGeneticist = None,
    profileExpirationDate = None,
    sampleDate           = None,
    sampleEntryDate      = None,
    dataFiliation        = None,
    isExternal           = false
  )

  "ProfileDataServiceImpl.get(id)" must {

    "return Some when the repository finds a profile by numeric id" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.get(1L)).thenReturn(Future.successful(Some(testProfile)))

      val result = Await.result(service.get(1L), duration)
      result mustBe Some(testProfile)
    }

    "return None when the repository finds no profile by numeric id" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.get(99L)).thenReturn(Future.successful(None))

      val result = Await.result(service.get(99L), duration)
      result mustBe None
    }
  }

  "ProfileDataServiceImpl.get(SampleCode)" must {

    "return Some when the repository finds a profile by sample code" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.get(sampleCode)).thenReturn(Future.successful(Some(testProfile)))

      val result = Await.result(service.get(sampleCode), duration)
      result mustBe Some(testProfile)
    }

    "return None when the repository finds no profile" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.get(sampleCode)).thenReturn(Future.successful(None))

      Await.result(service.get(sampleCode), duration) mustBe None
    }
  }

  "ProfileDataServiceImpl.findByCode" must {

    "return Some when found" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.findByCode(sampleCode)).thenReturn(Future.successful(Some(testProfile)))

      Await.result(service.findByCode(sampleCode), duration) mustBe Some(testProfile)
    }

    "return None when not found" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.findByCode(sampleCode)).thenReturn(Future.successful(None))

      Await.result(service.findByCode(sampleCode), duration) mustBe None
    }
  }

  "ProfileDataServiceImpl.findByCodes" must {

    "return a sequence of profiles from the repository" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      val codes   = List(sampleCode)
      when(repo.findByCodes(codes)).thenReturn(Future.successful(Seq(testProfile)))

      val result = Await.result(service.findByCodes(codes), duration)
      result must have length 1
      result.head.globalCode mustBe sampleCode
    }

    "return an empty sequence when no profiles match" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.findByCodes(Nil)).thenReturn(Future.successful(Seq.empty))

      Await.result(service.findByCodes(Nil), duration) mustBe empty
    }
  }

  "ProfileDataServiceImpl.isDeleted" must {

    "return Some(true) for a deleted profile" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.isDeleted(sampleCode)).thenReturn(Future.successful(Some(true)))

      Await.result(service.isDeleted(sampleCode), duration) mustBe Some(true)
    }

    "return Some(false) for an existing non-deleted profile" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.isDeleted(sampleCode)).thenReturn(Future.successful(Some(false)))

      Await.result(service.isDeleted(sampleCode), duration) mustBe Some(false)
    }

    "return None when the profile does not exist" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.isDeleted(sampleCode)).thenReturn(Future.successful(None))

      Await.result(service.isDeleted(sampleCode), duration) mustBe None
    }
  }

  "ProfileDataServiceImpl.getDeleteMotive" must {

    "return the motive when present" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      val motive  = DeletedMotive("sol", "mot", 1L)
      when(repo.getDeletedMotive(sampleCode)).thenReturn(Future.successful(Some(motive)))

      Await.result(service.getDeleteMotive(sampleCode), duration) mustBe Some(motive)
    }

    "return None when the profile has no delete motive" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.getDeletedMotive(sampleCode)).thenReturn(Future.successful(None))

      Await.result(service.getDeleteMotive(sampleCode), duration) mustBe None
    }
  }

  "ProfileDataServiceImpl.getDesktopProfiles" must {

    "return a sequence of sample codes" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.getDesktopProfiles()).thenReturn(Future.successful(Seq(sampleCode)))

      val result = Await.result(service.getDesktopProfiles(), duration)
      result mustBe Seq(sampleCode)
    }
  }

  "ProfileDataServiceImpl.isDesktopProfile" must {

    "return Some(true) for a desktop profile" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.isDesktopProfile(sampleCode)).thenReturn(Future.successful(Some(true)))

      Await.result(service.isDesktopProfile(sampleCode), duration) mustBe Some(true)
    }
  }

  "ProfileDataServiceImpl.removeProfile" must {

    "return Right(sampleCode) on success" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.removeProfile(sampleCode)).thenReturn(Future.successful(Right(sampleCode)))

      Await.result(service.removeProfile(sampleCode), duration) mustBe Right(sampleCode)
    }

    "return Left with error message when profile does not exist" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.removeProfile(sampleCode)).thenReturn(Future.successful(Left("Profile not found: AR-B-SIGE-1")))

      val result = Await.result(service.removeProfile(sampleCode), duration)
      result mustBe a[Left[?, ?]]
    }
  }

  "ProfileDataServiceImpl.getLabFromGlobalCode" must {

    "extract the lab segment (index 2) from a valid global code" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      service.getLabFromGlobalCode(SampleCode("AR-B-SIGE-1")) mustBe Some("SIGE")
    }

    "extract a multi-letter lab code correctly" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      service.getLabFromGlobalCode(SampleCode("ARG-B-LABGENET-42")) mustBe Some("LABGENET")
    }
  }

  "ProfileDataServiceImpl.shouldSendDeleteToSuperiorInstance" must {

    "return true when profile is replicated" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.getIsProfileReplicated(sampleCode)).thenReturn(true)

      service.shouldSendDeleteToSuperiorInstance(sampleCode) mustBe true
    }

    "return false when profile is not replicated" in {
      val repo    = mock[ProfileDataRepository]
      val service = new ProfileDataServiceImpl(repo)
      when(repo.getIsProfileReplicated(sampleCode)).thenReturn(false)

      service.shouldSendDeleteToSuperiorInstance(sampleCode) mustBe false
    }
  }
}
