package fixtures

import scala.concurrent.Future
import scala.util.{Success, Try}
import javax.inject.Singleton

import configdata.{BioMaterialType, CrimeType, CrimeTypeService, BioMaterialTypeService}
import disclaimer.{Disclaimer, DisclaimerService}
import kits.{FullStrKit, StrKit, StrKitLocus, StrKitService, NewStrKitLocus}
import services.{CountryService, GeneticistService, LaboratoryService, UserService, UserView}
import types.{Geneticist, Laboratory, User}

@Singleton
class StubLaboratoryService extends LaboratoryService:
  var listResult: Future[Seq[Laboratory]] = Future.successful(Seq.empty)
  var addResult: Future[Int] = Future.successful(1)
  var getResult: Future[Option[Laboratory]] = Future.successful(None)
  var updateResult: Future[Int] = Future.successful(1)

  override def list(): Future[Seq[Laboratory]] = listResult
  override def add(lab: Laboratory): Future[Int] = addResult
  override def get(id: String): Future[Option[Laboratory]] = getResult
  override def update(lab: Laboratory): Future[Int] = updateResult

@Singleton
class StubCountryService extends CountryService:
  var listCountriesResult: Future[Seq[String]] = Future.successful(Seq.empty)
  var listProvincesResult: Future[Seq[String]] = Future.successful(Seq.empty)

  override def listCountries: Future[Seq[String]] = listCountriesResult
  override def listProvinces(country: String): Future[Seq[String]] = listProvincesResult

@Singleton
class StubGeneticistService extends GeneticistService:
  var addResult: Future[Int] = Future.successful(0)
  var getAllResult: Future[Seq[Geneticist]] = Future.successful(Seq.empty)
  var updateResult: Future[Int] = Future.successful(0)
  var getResult: Future[Option[Geneticist]] = Future.successful(None)

  override def add(geneticist: Geneticist): Future[Int] = addResult
  override def getAll(laboratory: String): Future[Seq[Geneticist]] = getAllResult
  override def update(geneticist: Geneticist): Future[Int] = updateResult
  override def get(id: Long): Future[Option[Geneticist]] = getResult

@Singleton
class StubUserService extends UserService:
  var findUserAssignableResult: Future[Seq[User]] = Future.successful(Seq.empty)
  var getUserOrEmptyResult: Future[Option[UserView]] = Future.successful(None)
  var isSuperUserResult: Future[Boolean] = Future.successful(false)

  override def findUserAssignable: Future[Seq[User]] = findUserAssignableResult
  override def getUserOrEmpty(userId: String): Future[Option[UserView]] = getUserOrEmptyResult
  override def isSuperUser(userId: String): Future[Boolean] = isSuperUserResult

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
