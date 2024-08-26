package kits

import bulkupload.ProtoProfileRepository
import org.mockito.Matchers.any
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.Play.current
import play.api.db.slick._
import profile.ProfileRepository
import services.{CacheService, Keys}
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class StrKitServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val kitList = List(StrKit("Identifiler", "Identifiler", 1, 16, 15), StrKit("Powerplex16", "Powerplex16", 1, 16, 16))

  val lociList = Seq("AMEL", "CSF1PO", "D13S317")

  val aliasLoci = Map("amel" -> "AMEL", "Csf1po" -> "CSF1PO", "d13s317" -> "D13S317")

  val aliasList = Map("identifiler" -> "Identifiler", "PowerPlex16" -> "Powerplex16", "Power_Plex_16" -> "Powerplex16")

  val kit = Stubs.fullKits.head
  val id = kit.id
  val error = Left("Error message")
  val strKits = Stubs.fullKits.map(k => StrKit(k.id, k.name, k.`type`, k.locy_quantity, k.representative_parameter))

  "A StrKitService" must {
    "list all the alias for kits" in {

      val mockedStrKitRepository = mock[StrKitRepository]
      when(mockedStrKitRepository.list).thenReturn(Future.successful(kitList))
      when(mockedStrKitRepository.getKitsAlias).thenReturn(Future.successful(aliasList))

      val service: StrKitService = new StrKitServiceImpl(Stubs.cacheServiceMock, mockedStrKitRepository, null, null)

      val result = Await.result(service.getKitAlias, duration)

      result.size mustBe kitList.length + aliasList.size
      result("identifiler") mustBe "Identifiler"
      result("Powerplex16") mustBe "Powerplex16"
    }

    "list all the alias for locus" in {

      val mockedStrKitRepository = mock[StrKitRepository]
      when(mockedStrKitRepository.getAllLoci).thenReturn(Future.successful(lociList))
      when(mockedStrKitRepository.getLociAlias).thenReturn(Future.successful(aliasLoci))

      val service: StrKitService = new StrKitServiceImpl(Stubs.cacheServiceMock, mockedStrKitRepository, null, null)

      val result = Await.result(service.getLocusAlias, duration)

      result.size mustBe lociList.length + aliasLoci.size
      result("amel") mustBe "AMEL"
      result("D13S317") mustBe "D13S317"
    }

    "list full kits" in {
      val kitRepository = mock[StrKitRepository]
      when(kitRepository.listFull()).thenReturn(Future.successful(Stubs.fullKits))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, null, null)

      val result = Await.result(service.listFull(), duration)

      result mustBe Stubs.fullKits
    }

    "list kits" in {
      val kitRepository = mock[StrKitRepository]
      when(kitRepository.listFull()).thenReturn(Future.successful(Stubs.fullKits))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, null, null)

      val result = Await.result(service.list(), duration)

      result mustBe strKits
    }

    "add kit - ok" in {
      val kitRepository = new MockKitRepository(Right(id), Right(id))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, null, null)

      val result = Await.result(service.add(kit), duration)

      result mustBe Right(id)
    }

    "add kit - error" in {
      val kitRepository = new MockKitRepository(error, Right(id))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, null, null)

      val result = Await.result(service.add(kit), duration)

      result mustBe error
    }

    "delete kit - ok" in {
      val kitRepository = new MockKitRepository(Right(id), Right(id))
      val profileRepository = mock[ProfileRepository]
      when(profileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, profileRepository, protoProfileRepository)

      val result = Await.result(service.delete(id), duration)

      result mustBe Right(id)
    }

    "delete kit - error" in {
      val kitRepository = new MockKitRepository(Right(id), error)
      val profileRepository = mock[ProfileRepository]
      when(profileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, profileRepository, protoProfileRepository)

      val result = Await.result(service.delete(id), duration)

      result mustBe error
    }

    "not delete kit because of profile E0695" in {
      val kitRepository = new MockKitRepository(Right(id), Right(id))
      val profileRepository = mock[ProfileRepository]
      when(profileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(false))
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, profileRepository, protoProfileRepository)

      val result = Await.result(service.delete(id), duration)

      result.isLeft mustBe true
      result mustBe Left("E0695: No se puede eliminar el kit KIT 1 ya que se está utilizando en un perfil.")

    }

    "not delete kit because of proto profile E0696" in {
      val kitRepository = new MockKitRepository(Right(id), Right(id))
      val profileRepository = mock[ProfileRepository]
      when(profileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(false))

      val service = new StrKitServiceImpl(Stubs.cacheServiceMock, kitRepository, profileRepository, protoProfileRepository)

      val result = Await.result(service.delete(id), duration)

      result.isLeft mustBe true
      result mustBe Left("E0696: No se puede eliminar el kit KIT 1 ya que se está utilizando en un lote de carga masiva.")

    }

    "get kit by id" in {
      val mockResult = Stubs.strKits.headOption

      val mockedStrKitRepository = mock[StrKitRepository]
      when(mockedStrKitRepository.get(any[String])).thenReturn(Future.successful(mockResult))

      val service: StrKitService = new StrKitServiceImpl(Stubs.cacheServiceMock, mockedStrKitRepository, null, null)

      val result = Await.result(service.get("Identifiler"), duration)

      result mustBe mockResult
    }

    "clean cache after add kit" in {
      val cache = mock[CacheService]
      val kitRepository = new MockKitRepository(Right(id), Right(id))

      val service = new StrKitServiceImpl(cache, kitRepository, null, null)

      Await.result(service.add(kit), duration)

      verify(cache).pop(Keys.strKits)
    }

    "clean cache after delete kit" in {
      val cache = mock[CacheService]

      val kitRepository = new MockKitRepository(Right(id), Right(id))

      val profileRepository = mock[ProfileRepository]
      when(profileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))

      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.canDeleteKit(any[String])).thenReturn(Future.successful(true))

      val service = new StrKitServiceImpl(cache, kitRepository, profileRepository, protoProfileRepository)

      Await.result(service.delete(id), duration)

      verify(cache).pop(Keys.strKits)
    }
  }
}
