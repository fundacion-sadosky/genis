package profiledata

import java.io.File
import java.util.Date

import scala.Left
import scala.Right
import scala.concurrent.{Await, Future}
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

import com.github.tototoshi.csv.{CSVWriter, DefaultCSVFormat}
import models.Tables
import models.Tables.ProfileUploadedRow
import models.Tables.ProfileSentRow
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profile.{MtRCRS, Profile, ProfileRepository, ProfileService}
import services.CacheService
import services.TemporaryAssetKey
import types.AlphanumericId
import types.SampleCode
import configdata.BioMaterialTypeService
import configdata.CrimeTypeService
import configdata.CategoryService
import configdata.Category
import configdata.CrimeType
import laboratories.Laboratory
import laboratories.LaboratoryService
import matching.{MatchGlobalStatus, MatchingService}
import configdata.Group
import configdata.FullCategory
import connections.InterconnectionService
import inbox._
import scenarios.{ScenarioRepository, ScenarioService}
import trace.{DeleteInfo, Trace, TraceService}
import play.api.i18n.Messages
import models.Tables.ExternalProfileDataRow
import pedigree.PedigreeService
import matching.CollapseRequest
import user.UserService

import scala.concurrent.duration._
trait ProfileDataService {
  def get(id: Long): Future[(ProfileData, Group, Category)]
  def findByCode(globalCode: SampleCode): Future[Option[ProfileData]]
  def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]]
  def findByCodeWithAssociations(globalCode: SampleCode): Future[Option[(ProfileData, Group, FullCategory)]]
  def updateProfileData(globalCode: SampleCode, profileData: ProfileDataAttempt): Future[Boolean]
  def get(sampleCode: SampleCode): Future[Option[ProfileData]]
  def isEditable(sampleCode: SampleCode): Future[Option[Boolean]]
  def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]]
  def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]]
  def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String,shouldUpdateSuperiorInstance:Boolean = true,validateMPI:Boolean = true): Future[Either[String, SampleCode]]
  def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]]
  def delete(globalCode: SampleCode): Future[Either[String, SampleCode]]
  def importFromAnotherInstance(profileData: ProfileData,labOrigin:String,labImmediate:String):Future[Unit]
  def updateUploadStatus(globalCode: String,status:Long,motive:Option[String]= None): Future[Either[String,Unit]]
  def getProfileUploadStatusByGlobalCode(globalCode:SampleCode):Future[Option[Long]]
  def getExternalProfileDataByGlobalCode(globalCode:String):Future[Option[ExternalProfileDataRow]]
  def findProfileDataLocalOrSuperior(globalCode:SampleCode):Future[Option[ProfileData]]
  def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]]
  def gefFailedProfilesUploaded():Future[Seq[ProfileUploadedRow]]
  def gefFailedProfilesUploadedDeleted():Future[Seq[ProfileUploadedRow]]
  def gefFailedProfilesSentDeleted(labCode:String):Future[Seq[ProfileSentRow]]
  def updateProfileSentStatus(globalCode: String,status:Long,motive:Option[String]= None,labCode:String): Future[Either[String,Unit]]
  def getMtRcrs():Future[MtRCRS]
}

@Singleton
class ProfileDataServiceImpl @Inject() (
    cache: CacheService,
    @Named("special") profileDataRepository: ProfileDataRepository,
    categoryService: CategoryService,
    notificationService: NotificationService,
    bioMatService: BioMaterialTypeService,
    crimeType: CrimeTypeService,
    laboratories: LaboratoryService,
    matchingService: MatchingService,
    scenarioRepository: ScenarioRepository,
    profileRepository: ProfileRepository,
    traceService: TraceService,
    @Named("labCode") val labCode: String,
    @Named("country") val country: String,
    @Named("province") val province: String,
    interconnectionService : InterconnectionService = null,
    profileService:ProfileService=null,
    pedigreeService: PedigreeService = null,
    userService : UserService = null) extends ProfileDataService {

  val logger = Logger(this.getClass())

  override def getResource(resourceType: String, id: Long): Future[Option[Array[Byte]]] = {
    profileDataRepository.getResource(resourceType, id)
  }

  override def get(sampleCode: SampleCode): Future[Option[ProfileData]] = {
    profileDataRepository.get(sampleCode)
  }

  private def canDeleteProfile(sampleCode: SampleCode,validateMPI:Boolean) : Future[(Boolean,Option[String])] = {

    Future.sequence(
        if(validateMPI){
          Seq(
            doesntHaveScenarios(sampleCode)
            ,isNotAssociatedToCourtCase(sampleCode)
            ,isNotAssociatedToActivePedigree(sampleCode)
            ,doesNotHavePedigreeMatches(sampleCode)
            ,doesNotHavePedigreeMatchesBeingUnknown(sampleCode)
          )
        }else{
          Seq(doesntHaveScenarios(sampleCode)
            ,isNotAssociatedToActivePedigree(sampleCode)
            ,doesNotHavePedigreeMatches(sampleCode)
            ,doesNotHavePedigreeMatchesBeingUnknown(sampleCode)
          )
        }
      )
      .map(listBooleans => (listBooleans.forall(x=>x._1),listBooleans.find(x => !x._1).getOrElse((true,None))._2))
  }
  private def doesNotHavePedigreeMatches(sampleCode: SampleCode) : Future[(Boolean,Option[String])]= {
    pedigreeService.getTotalProfilesPedigreeMatches(sampleCode).map(quantity => (quantity == 0,Some("error.E0130")))
  }
  private def doesNotHavePedigreeMatchesBeingUnknown(sampleCode: SampleCode) : Future[(Boolean,Option[String])]= {
    pedigreeService.getTotalProfileNumberOfMatches(sampleCode).map(quantity => (quantity == 0,Some("error.E0129")))
  }
  private def isNotAssociatedToCourtCase(sampleCode: SampleCode) : Future[(Boolean,Option[String])]= {
    pedigreeService.getTotalProfilesOccurenceInCase(sampleCode).map(quantity => (quantity == 0,Some("error.E0125")))
  }
  private def isNotAssociatedToPendingPedigreeScenario(sampleCode: SampleCode) : Future[(Boolean,Option[String])]= {
    pedigreeService.countPendingScenariosByProfile(sampleCode.text).map(quantity => (quantity == 0,Some("error.E0128")))
  }
  private def isNotAssociatedToActivePedigree(sampleCode: SampleCode) : Future[(Boolean,Option[String])]= {
    pedigreeService.countActivePedigreesByProfile(sampleCode.text).map(quantity => (quantity == 0,Some("error.E0126")))
  }
  private def doesntHaveScenarios(sampleCode: SampleCode) : Future[(Boolean,Option[String])] = {
    scenarioRepository.getByProfile(sampleCode) map { scenarios => (scenarios.isEmpty,Some("error.E0118")) }
  }

  /*
    implicit object ProfileArchiveFormat extends DefaultCSVFormat {
      override val delimiter = '\t'
    }

    def createBajaLimsArchive(globalCode: SampleCode, motive: DeletedMotive) = {
      val folder = s"$exportProfilesPath${File.separator}"
      val folderFile = new File(folder)

      folderFile.mkdirs

      generateBajaFile(folder, globalCode, motive)

    }
    def generateBajaFile(folder: String, globalCode: SampleCode, motive: DeletedMotive): File = {
      val file = new File(s"${folder}BajaPerfil${globalCode.text}.txt")

      val writer = CSVWriter.open(file)
      writer.writeAll(List(List("GENis Code",
        "Sample name",
        "Motive",
        "Status",
        "DateTime")))
      val format = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss a")
      val profileFuture = get(globalCode)
      val profile = Await.result(profileFuture, Duration(100, SECONDS))

      writer.writeAll(List(List(globalCode.text, profile.get.internalSampleCode , motive.motive, "BAJABD" , format.format(new java.util.Date()))))


      writer.close()
      file
    }
  */


  override def deleteProfile(globalCode: SampleCode, motive: DeletedMotive, userId: String,shouldUpdateSuperiorInstance:Boolean = true,validateMPI:Boolean = true): Future[Either[String, SampleCode]] = {
    canDeleteProfile(globalCode,validateMPI) flatMap { case (allowed,msg) =>
      if (allowed) {
        delete(globalCode) flatMap { response =>
          if (response.isRight) {
            profileDataRepository.delete(globalCode, motive) map { resp =>
              if (resp == 1) {
                traceService.add(Trace(globalCode, userId, new Date(), DeleteInfo(motive.solicitor, motive.motive)))
                if(shouldUpdateSuperiorInstance){
                  interconnectionService.inferiorDeleteProfile(globalCode,motive)
                }
/*
                if (exportaALims) {
                  createBajaLimsArchive(globalCode, motive)
                }
*/
                response
              } else Left(Messages("error.E0117"))
            }
          } else {
            Future.successful(response)
          }
        }
      } else {
        Future.successful(Left(Messages(msg.getOrElse("error.E0127"), globalCode.text)))
      }
    }

  }

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = {
    profileRepository.delete(globalCode) flatMap { response =>
      response.fold(fa => Future.successful(Left(fa)), fb => {
            Future.successful(Right(globalCode))
      })
    }
  }

  override def get(id: Long): Future[(ProfileData, Group, Category)] = {
    for {
      profile <- profileDataRepository.get(id)
    } yield {

      val catId = profile.category
      val grpId = categoryService.listCategories(catId).group
      val (group, category) = categoryService.categoryTree.find { case (group, _) => group.id == grpId }.map {
        case (group, cats) => (group, cats.find { _.id == catId }.get)
      }.get

      (profile, group, category)
    }
  }

  def isEditable(sampleCode: SampleCode): Future[Option[Boolean]] = {

    val d = for {
      matches <- matchingService.findMatchingResults(sampleCode)
      isReadOnly <-profileService.isReadOnlySampleCode(sampleCode)
    } yield (matches.isDefined,isReadOnly._1)

    d map {
      case (hasMatches,isReadOnly) => {
        Some(!(hasMatches || isReadOnly))
      }
    }

  }

  private def getDetails(pd: Option[ProfileData]) = {
    pd.fold[Future[Option[ProfileData]]](Future.successful(None))({ pd =>
      val joins = for {
        cat <- Future.successful(categoryService.listCategories(pd.category))
        bmt <- bioMatService.list map { seq => pd.bioMaterialType.fold[Option[String]](None)(f => seq.find { _.id.text == f } map (_.name)) }
        crt <- crimeType.list map { map => pd.crimeType.fold[Option[CrimeType]](None)(map.get(_)) }
        lab <- laboratories.list map { _.find { _.code == pd.laboratory } }
      } yield (cat, bmt, crt, lab)

      joins map {
        case (cat, bmt, ct, lab) => {

          val category = cat.id
          val crimetype = ct map { _.name }
          val crimeInv = pd.crimeInvolved.fold[Option[String]](None)(f => ct flatMap (_.crimes.find(_.id == f) map (_.name)))

          val labName = lab.getOrElse(Laboratory(labCode, labCode, country, province, "", "", null, 0 , 0))

          val some = ProfileData(category, pd.globalCode, pd.attorney,
            bmt, pd.court, crimeInv, crimetype, pd.criminalCase,
            pd.internalSampleCode, pd.assignee, labName.name, pd.deleted, None, pd.responsibleGeneticist,
            pd.profileExpirationDate, pd.sampleDate, pd.sampleEntryDate, pd.dataFiliation,pd.isExternal)

          Option(some)
        }
      }
    })
  }
  override def findByCode(globalCode: SampleCode): Future[Option[ProfileData]] = {
    profileDataRepository.findByCode(globalCode) flatMap {
      getDetails(_)
    }
  }
  override def findByCodeWithoutDetails(globalCode: SampleCode): Future[Option[ProfileData]] = {
    profileDataRepository.findByCode(globalCode)
  }
  override def findByCodes(globalCodes: List[SampleCode]): Future[Seq[ProfileData]] = {
    val futSeqFut = profileDataRepository.findByCodes(globalCodes) map { listProfiles =>
      listProfiles.map(p => getDetails(Some(p)) map (_.get))
    }
    futSeqFut.flatMap(Future.sequence(_))
  }

  private def searchImagesInCache(uuid: String): Either[String, Option[List[File]]] =
    cache.get(TemporaryAssetKey(uuid)).map {
      tempFiles =>
        val files = tempFiles map { _.file }
        Right(Some(files))
    }.getOrElse(Left(Messages("error.E0951", uuid )))

  override def updateProfileData(globalCode: SampleCode, profileData: ProfileDataAttempt): Future[Boolean] = {

    this.isEditable(globalCode).flatMap { result =>
      if (result.get) {
        val filiationDataOpt = profileData.dataFiliation

        val images = filiationDataOpt map { filiationData =>
          val images = for {
            inprints <- searchImagesInCache(filiationData.inprint).right
            pictures <- searchImagesInCache(filiationData.picture).right
            signatures <- searchImagesInCache(filiationData.signature).right
          } yield {
            (inprints, pictures, signatures)
          }
          images
        } getOrElse {
          Right((None, None, None))
        }

        images match {
          case Right((inprints, pictures, signatures)) => {
            val pd = profileData.pdAttempToPd(labCode) //pdAttempToPd(profileData)
            val updatePromise = profileDataRepository.updateProfileData(globalCode, pd, inprints, pictures, signatures)
            updatePromise.onComplete { updated =>
               traceService.add(Trace(globalCode, profileData.assignee, new Date(), trace.ProfileDataInfo))
              filiationDataOpt map { filiationData =>
                cache.pop(TemporaryAssetKey(filiationData.inprint))
                cache.pop(TemporaryAssetKey(filiationData.picture))
                cache.pop(TemporaryAssetKey(filiationData.signature))
              }
            }
            updatePromise
          }
          case Left(_) => Future.successful(false)
        }
      } else {
        Future.successful(false)
      }
    }
  }

  override def create(profileData: ProfileDataAttempt): Future[Either[String, SampleCode]] = {

    val filiationDataOpt = profileData.dataFiliation

    val images = filiationDataOpt map { filiationData =>
      val images = for {
        inprints <- searchImagesInCache(filiationData.inprint).right
        pictures <- searchImagesInCache(filiationData.picture).right
        signatures <- searchImagesInCache(filiationData.signature).right
      } yield {
        (inprints, pictures, signatures)
      }
      images
    } getOrElse {
      Right((None, None, None))
    }

    images match {
      case Right((inprints, pictures, signatures)) => {
        val pd = profileData.pdAttempToPd(labCode)
        val gsc = profileData.laboratory.fold(Future.successful(country + "-" + province + "-" + labCode))(f => profileDataRepository.giveGlobalCode(f))
        val addPromise = gsc flatMap { g => profileDataRepository.add(pd, g, inprints, pictures, signatures) map { result => Right(result) } }
        addPromise.onComplete { sampleCode =>
          filiationDataOpt map { filiationData =>
            cache.pop(TemporaryAssetKey(filiationData.inprint))
            cache.pop(TemporaryAssetKey(filiationData.picture))
            cache.pop(TemporaryAssetKey(filiationData.signature))
          }
        }
        addPromise.onSuccess {
          case Right(globalCode) => {
            traceService.add(Trace(globalCode, profileData.assignee, new Date(), trace.ProfileDataInfo))
            notificationService.push(profileData.assignee,
              ProfileDataInfo(profileData.internalSampleCode, globalCode))
            userService.sendNotifToAllSuperUsers(ProfileDataInfo(profileData.internalSampleCode, globalCode), Seq(profileData.assignee))

            if (categoryService.listCategories(profileData.category).associations.nonEmpty) {
              notificationService.push(profileData.assignee,
                ProfileDataAssociationInfo(profileData.internalSampleCode, globalCode))
              userService.sendNotifToAllSuperUsers(ProfileDataAssociationInfo(profileData.internalSampleCode, globalCode), Seq(profileData.assignee))
            }
          }
        }
        addPromise.recover {
          case error => Left(Messages("error.E0119"))
        }
      }
    }
  }

  override def findByCodeWithAssociations(globalCode: SampleCode): Future[Option[(ProfileData, Group, FullCategory)]] = {
    for {
      profileOpt <- profileService.findProfileDataLocalOrSuperior(globalCode)
    } yield {

      profileOpt map { profile =>
        val catId = profile.category
        val grpId = categoryService.listCategories(catId).group
        val group = categoryService.categoryTree.find { case (g, _) => g.id == grpId }.map(_._1).get
        val category = categoryService.listCategories.find { case (g, _) => g == catId }.map(_._2).get
        (profile, group, category)
      }
    }
  }

  override def getDeleteMotive(sampleCode: SampleCode): Future[Option[DeletedMotive]] = profileDataRepository.getDeletedMotive(sampleCode)

  def importFromAnotherInstance(profileData: ProfileData,labOrigin:String,labImmediate:String):Future[Unit] = {
    profileDataRepository.addExternalProfile(profileData,labOrigin,labImmediate).map( _ => ())
  }

  override def updateUploadStatus(globalCode: String,status:Long,motive:Option[String]= None): Future[Either[String,Unit]] = {
    profileDataRepository.updateUploadStatus(globalCode,status,motive)
  }

  override def getProfileUploadStatusByGlobalCode(gc:SampleCode):Future[Option[Long]] = {
    this.profileDataRepository.getProfileUploadStatusByGlobalCode(gc)
  }

  override def getExternalProfileDataByGlobalCode(globalCode:String):Future[Option[ExternalProfileDataRow]] = {
    profileDataRepository.getExternalProfileDataByGlobalCode(globalCode)
  }

  override def findProfileDataLocalOrSuperior(globalCode:SampleCode):Future[Option[ProfileData]] = {
    profileService.findProfileDataLocalOrSuperior(globalCode)
  }

  override def gefFailedProfilesUploaded():Future[Seq[ProfileUploadedRow]] = {
    this.profileDataRepository.gefFailedProfilesUploaded().map(list => list.seq)
  }
  override def gefFailedProfilesUploadedDeleted():Future[Seq[ProfileUploadedRow]] = {
    this.profileDataRepository.gefFailedProfilesUploadedDeleted().map(list => list.seq)
  }
  override def gefFailedProfilesSentDeleted(labCode:String):Future[Seq[ProfileSentRow]] = {
    this.profileDataRepository.gefFailedProfilesSentDeleted(labCode).map(list => list.seq)
  }
  override def updateProfileSentStatus(globalCode: String,status:Long,motive:Option[String]= None,labCode:String): Future[Either[String,Unit]] = {
    profileDataRepository.updateProfileSentStatus(globalCode,status,motive,labCode)
  }
  override def getMtRcrs() = {
    this.profileDataRepository.getMtRcrs()
  }
}
