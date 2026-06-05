package fixtures

import scala.collection.immutable.IndexedSeq
import scala.concurrent.Future
import scala.util.{Success, Try}
import java.io.File
import javax.inject.Singleton

import audit.{Key, OperationLogEntry, OperationLogEntryAttemp, OperationLogLotView,
  OperationLogSearch, OperationLogService, SignedOperationLogEntry}
import configdata.{BioMaterialType, CrimeType, CrimeTypeService, BioMaterialTypeService}
import disclaimer.{Disclaimer, DisclaimerService}
import kits.{FullStrKit, StrKit, StrKitLocus, StrKitService, NewStrKitLocus}
import inbox.NotificationInfo
import probability.ProbabilityService
import profile.Analysis
import services.{Country, CountryService, GeneticistService, LaboratoryService, Province, UserService}
import stats.{Fmins, PopBaseFreqResult, PopulationBaseFrequency, PopulationBaseFrequencyGrouppedByLocus, PopulationBaseFrequencyNameView, PopulationBaseFrequencyService, PopulationBaseFrequencyView, ProbabilityModel}
import types.{AlphanumericId, Geneticist, Laboratory, Permission, StatOption}
import security.User
import user.{ClearPassChallenge, ClearPassResponse, ClearPassSolicitud,
  SignupChallenge, SignupResponse, SignupSolicitude, UserStatus, UserView}

/** No-op audit service for tests: avoids the PEOSignerActor connecting to genislogdb
 *  during GuiceApplicationBuilder startup. */
@Singleton
class StubOperationLogService extends OperationLogService:
  override def add(entry: OperationLogEntryAttemp): Future[Unit] = Future.successful(())
  override def listLotsView(page: Int, pageSize: Int): Future[Seq[OperationLogLotView]] =
    Future.successful(Seq.empty)
  override def checkLot(id: Long): Future[Option[Either[(SignedOperationLogEntry, Key), Unit]]] =
    Future.successful(Some(Right(())))
  override def getLotsLength(): Future[Int] = Future.successful(0)
  override def searchLogs(search: OperationLogSearch): Future[Seq[OperationLogEntry]] =
    Future.successful(Seq.empty)
  override def getLogsLength(search: OperationLogSearch): Future[Int] = Future.successful(0)

@Singleton
class StubLaboratoryService extends LaboratoryService:
  var listResult: Future[Seq[Laboratory]] = Future.successful(Seq.empty)
  var listDescriptiveResult: Future[Seq[Laboratory]] = Future.successful(Seq.empty)
  var addResult: Future[Int] = Future.successful(1)
  var getResult: Future[Option[Laboratory]] = Future.successful(None)
  var updateResult: Future[Int] = Future.successful(1)

  override def list(): Future[Seq[Laboratory]] = listResult
  override def listDescriptive(): Future[Seq[Laboratory]] = listDescriptiveResult
  override def add(lab: Laboratory): Future[Int] = addResult
  override def get(id: String): Future[Option[Laboratory]] = getResult
  override def update(lab: Laboratory): Future[Int] = updateResult

@Singleton
class StubCountryService extends CountryService:
  var listCountriesResult: Future[Seq[Country]] = Future.successful(Seq.empty)
  var listProvincesResult: Future[Seq[Province]] = Future.successful(Seq.empty)

  override def listCountries: Future[Seq[Country]] = listCountriesResult
  override def listProvinces(country: String): Future[Seq[Province]] = listProvincesResult

@Singleton
class StubGeneticistService extends GeneticistService:
  var addResult: Future[Either[String, Int]] = Future.successful(Right(0))
  var getAllResult: Future[Seq[Geneticist]] = Future.successful(Seq.empty)
  var updateResult: Future[Int] = Future.successful(0)
  var getResult: Future[Option[Geneticist]] = Future.successful(None)

  override def add(geneticist: Geneticist): Future[Either[String, Int]] = addResult
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = getAllResult
  override def update(geneticist: Geneticist): Future[Int] = updateResult
  override def get(id: Long): Future[Option[Geneticist]] = getResult

@Singleton
class StubUserService extends UserService:
  var findUserAssignableResult: Future[Seq[User]] = Future.successful(Seq.empty)
  var signupRequestResult: Future[Either[String, SignupResponse]] = Future.successful(Left("stub"))
  var clearPassRequestResult: Future[Either[String, ClearPassResponse]] = Future.successful(Left("stub"))
  var signupConfirmationResult: Future[Either[String, Int]] = Future.successful(Left("stub"))
  var clearPassConfirmationResult: Future[Either[String, Int]] = Future.successful(Left("stub"))
  var listAllUsersResult: Future[Seq[UserView]] = Future.successful(Seq.empty)
  var setStatusResult: Future[Either[String, Int]] = Future.successful(Left("stub"))
  var updateUserResult: Future[Boolean] = Future.successful(true)

  override def findUserAssignable: Future[Seq[User]] = findUserAssignableResult
  override def signupRequest(solicitude: SignupSolicitude): Future[Either[String, SignupResponse]] = signupRequestResult
  override def clearPassRequest(solicitude: ClearPassSolicitud): Future[Either[String, ClearPassResponse]] = clearPassRequestResult
  override def signupConfirmation(confirmation: SignupChallenge): Future[Either[String, Int]] = signupConfirmationResult
  override def clearPassConfirmation(confirmation: ClearPassChallenge): Future[Either[String, Int]] = clearPassConfirmationResult
  override def listAllUsers(): Future[Seq[UserView]] = listAllUsersResult
  override def setStatus(userId: String, newStatus: UserStatus): Future[Either[String, Int]] = setStatusResult
  override def updateUser(user: UserView): Future[Boolean] = updateUserResult
  override def getUser(userId: String): Future[UserView] = Future.failed(new UnsupportedOperationException("stub"))
  override def getUserOrEmpty(userId: String): Future[Option[UserView]] = Future.successful(None)
  override def findByStatus(status: UserStatus): Future[Seq[UserView]] = Future.successful(Seq.empty)
  override def findByGeneMapper(geneMapper: String): Future[Option[UserView]] = Future.successful(None)
  override def findUserAssignableByRole(roleId: String): Future[Seq[User]] = Future.successful(Seq.empty)
  override def findUsersIdWithPermission(permission: Permission): Future[Seq[String]] = Future.successful(Seq.empty)
  override def findUsersIdWithPermissions(permissions: Seq[Permission]): Future[Seq[String]] = Future.successful(Seq.empty)
  override def isSuperUser(userId: String): Future[Boolean] = Future.successful(false)
  override def isSuperUserByGeneMapper(geneMapperId: String): Future[Boolean] = Future.successful(false)
  override def findSuperUsers(): Future[Seq[String]] = Future.successful(Seq.empty)
  override def sendNotifToAllSuperUsers(info: NotificationInfo, excepThis: Seq[String]): Unit = ()

@Singleton
class StubDisclaimerService extends DisclaimerService:
  var getResult: Future[Disclaimer] = Future.successful(Disclaimer(None))

  override def get(): Future[Disclaimer] = getResult

@Singleton
class StubBioMaterialTypeService extends BioMaterialTypeService:
  var listResult: Future[Seq[BioMaterialType]] = Future.successful(Seq.empty)
  var insertResult: Future[Int] = Future.successful(1)
  var updateResult: Future[Int] = Future.successful(1)
  var deleteResult: Future[Int] = Future.successful(1)

  override def list(): Future[Seq[BioMaterialType]] = listResult
  override def insert(bmt: BioMaterialType): Future[Int] = insertResult
  override def update(bmt: BioMaterialType): Future[Int] = updateResult
  override def delete(bmtId: String): Future[Int] = deleteResult

@Singleton
class StubCrimeTypeService extends CrimeTypeService:
  var listResult: Future[Map[String, CrimeType]] = Future.successful(Map.empty)

  override def list(): Future[Map[String, CrimeType]] = listResult

@Singleton
class StubStrKitService extends StrKitService:
  var getResult: Future[Option[StrKit]] = Future.successful(None)
  var getFullResult: Future[Option[FullStrKit]] = Future.successful(None)
  var listResult: Future[Seq[StrKit]] = Future.successful(Seq.empty)
  var listFullResult: Future[Seq[FullStrKit]] = Future.successful(Seq.empty)
  var findLociByKitResult: Future[List[StrKitLocus]] = Future.successful(List.empty)
  var findLociByKitsResult: Future[Map[String, List[StrKitLocus]]] = Future.successful(Map.empty)
  var kitAliasResult: Future[Map[String, String]] = Future.successful(Map.empty)
  var locusAliasResult: Future[Map[String, String]] = Future.successful(Map.empty)
  var addResult: Future[Either[String, String]] = Future.successful(Right(""))
  var addAliasResult: Future[Either[String, String]] = Future.successful(Right(""))
  var addLocusResult: Future[Either[String, String]] = Future.successful(Right(""))
  var updateResult: Future[Either[String, String]] = Future.successful(Right(""))
  var deleteResult: Future[Either[String, String]] = Future.successful(Right(""))
  var deleteAliasResult: Future[Either[String, String]] = Future.successful(Right(""))
  var deleteLocusResult: Future[Either[String, String]] = Future.successful(Right(""))

  override def get(id: String) = getResult
  override def getFull(id: String) = getFullResult
  override def list() = listResult
  override def listFull() = listFullResult
  override def findLociByKit(kitId: String) = findLociByKitResult
  override def findLociByKits(kitIds: Seq[String]) = findLociByKitsResult
  override def getKitAlias = kitAliasResult
  override def getLocusAlias = locusAliasResult
  override def add(kit: StrKit) = addResult
  override def addAlias(id: String, alias: String) = addAliasResult
  override def addLocus(id: String, locus: NewStrKitLocus) = addLocusResult
  override def update(kit: StrKit) = updateResult
  override def delete(id: String) = deleteResult
  override def deleteAlias(id: String) = deleteAliasResult
  override def deleteLocus(id: String) = deleteLocusResult

@Singleton
class StubLdapHealthService extends user.LdapHealthService:
  var result: Try[(String, String)] = Success(("UP", "StubVendor"))
  override def checkStatus(): Try[(String, String)] = result

@Singleton
class StubProbabilityService extends ProbabilityService:
  var getStatsResult: Future[Option[StatOption]] = Future.successful(None)
  var calculateContributorsResult: Future[Int] = Future.successful(1)

  override def getStats(laboratory: String): Future[Option[StatOption]] = getStatsResult
  override def calculateContributors(analysis: Analysis, category: AlphanumericId, stats: StatOption): Future[Int] = calculateContributorsResult

@Singleton
class StubPopulationBaseFrequencyService extends PopulationBaseFrequencyService:
  var getByNameResult: Future[Option[PopulationBaseFrequency]] = Future.successful(None)
  var getDefaultResult: Future[Option[PopulationBaseFrequencyNameView]] = Future.successful(None)
  var getAllNamesResult: Future[Seq[PopulationBaseFrequencyNameView]] = Future.successful(Seq.empty)

  override def save(popBaseFreq: PopulationBaseFrequency): Future[Int] = Future.successful(1)
  override def getByName(name: String): Future[Option[PopulationBaseFrequency]] = getByNameResult
  override def getByNamePV(name: String): Future[PopulationBaseFrequencyView] = Future.failed(new UnsupportedOperationException("stub"))
  override def getAllNames(): Future[Seq[PopulationBaseFrequencyNameView]] = getAllNamesResult
  override def toggleStateBase(name: String): Future[Option[Int]] = Future.successful(Some(1))
  override def setAsDefault(name: String): Future[Int] = Future.successful(1)
  override def parseFile(name: String, theta: Double, model: ProbabilityModel, csvFile: File): Future[PopBaseFreqResult] = Future.failed(new UnsupportedOperationException("stub"))
  override def insertFmin(id: String, fmins: Fmins): Future[PopBaseFreqResult] = Future.failed(new UnsupportedOperationException("stub"))
  override def getDefault(): Future[Option[PopulationBaseFrequencyNameView]] = getDefaultResult
  override def getAllPossibleAllelesByLocus(): Future[PopulationBaseFrequencyGrouppedByLocus] = Future.successful(PopulationBaseFrequencyGrouppedByLocus(Map.empty))
class StubMongoHealthService extends profile.MongoHealthService:
  var result: Try[(String, String)] = Success(("UP", "StubDB"))
  override def checkStatus(): Try[(String, String)] = result

@Singleton
class StubPostgresHealthService extends configdata.PostgresHealthService:
  var result: Try[(String, String)] = Success(("UP", "PostgreSQL 15.0"))
  override def checkStatus(): Try[(String, String)] = result
