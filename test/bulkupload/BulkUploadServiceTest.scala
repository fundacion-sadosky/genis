package bulkupload

import configdata.{CategoryRepository, CategoryService}
import connections.InterconnectionService
import inbox.NotificationService
import kits.StrKitService
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import profile.ProfileService
import profiledata.{ImportToProfileData, ProfileDataRepository, ProfileDataService}
import services.CacheService
import specs.PdgSpec
import user.UserService

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

class BulkUploadServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)


  "bulkUpload service " must {

    "delete batch ok" in {
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.deleteBatch(1L)).thenReturn(Future.successful(Right(1L)))
      when(protoProfileRepository.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Right(0L)))

      val userService: UserService = mock[UserService]
      val kitService: StrKitService = mock[StrKitService]
      val categoryRepo: CategoryRepository = mock[CategoryRepository]
      val profileService: ProfileService = mock[ProfileService]
      val protoProfiledataService: ProfileDataService = mock[ProfileDataService]
      val profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository]
      val notificationService: NotificationService = mock[NotificationService]
      val importToProfileData: ImportToProfileData = mock[ImportToProfileData]
      val labCode: String = ""
      val country: String = ""
      val province: String = ""
      val ppGcD: String = ""
      val categoryService: CategoryService = mock[CategoryService]
      val cache: CacheService = mock[CacheService]
      val bulkUploadService = new BulkUploadServiceImpl(protoProfileRepository,
        userService, kitService, categoryRepo, profileService, protoProfiledataService, profileDataRepo,
        notificationService,importToProfileData,labCode,country,province,ppGcD,categoryService,cache
      );

      val result = Await.result(bulkUploadService.deleteBatch(1L), duration);

      result mustBe Right(1L)

    }

    "delete batch no ok with imported samples" in {
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.deleteBatch(1L)).thenReturn(Future.successful(Right(1L)))
      when(protoProfileRepository.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Right(1L)))

      val userService: UserService = mock[UserService]
      val kitService: StrKitService = mock[StrKitService]
      val categoryRepo: CategoryRepository = mock[CategoryRepository]
      val profileService: ProfileService = mock[ProfileService]
      val protoProfiledataService: ProfileDataService = mock[ProfileDataService]
      val profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository]
      val notificationService: NotificationService = mock[NotificationService]
      val importToProfileData: ImportToProfileData = mock[ImportToProfileData]
      val labCode: String = ""
      val country: String = ""
      val province: String = ""
      val ppGcD: String = ""
      val categoryService: CategoryService = mock[CategoryService]
      val cache: CacheService = mock[CacheService]
      val bulkUploadService = new BulkUploadServiceImpl(protoProfileRepository,
        userService, kitService, categoryRepo, profileService, protoProfiledataService, profileDataRepo,
        notificationService,importToProfileData,labCode,country,province,ppGcD,categoryService,cache
      );

      val result = Await.result(bulkUploadService.deleteBatch(1L), duration);

      result.isLeft mustBe true
      result mustBe Left("E0304: No se puede eliminar el lote porque contiene muestras aceptadas.")

    }

    "delete batch no ok db error" in {
      val protoProfileRepository = mock[ProtoProfileRepository]
      when(protoProfileRepository.deleteBatch(1L)).thenReturn(Future.successful(Left("DB error")))
      when(protoProfileRepository.countImportedProfilesByBatch(1L)).thenReturn(Future.successful(Right(0L)))

      val userService: UserService = mock[UserService]
      val kitService: StrKitService = mock[StrKitService]
      val categoryRepo: CategoryRepository = mock[CategoryRepository]
      val profileService: ProfileService = mock[ProfileService]
      val protoProfiledataService: ProfileDataService = mock[ProfileDataService]
      val profileDataRepo: ProfileDataRepository = mock[ProfileDataRepository]
      val notificationService: NotificationService = mock[NotificationService]
      val importToProfileData: ImportToProfileData = mock[ImportToProfileData]
      val labCode: String = ""
      val country: String = ""
      val province: String = ""
      val ppGcD: String = ""
      val categoryService: CategoryService = mock[CategoryService]
      val cache: CacheService = mock[CacheService]
      val bulkUploadService = new BulkUploadServiceImpl(protoProfileRepository,
        userService, kitService, categoryRepo, profileService, protoProfiledataService, profileDataRepo,
        notificationService,importToProfileData,labCode,country,province,ppGcD,categoryService,cache
      );

      val result = Await.result(bulkUploadService.deleteBatch(1L), duration);

      result.isLeft mustBe true

    }
  }


}
