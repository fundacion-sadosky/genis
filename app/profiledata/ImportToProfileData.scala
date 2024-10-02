package profiledata

import java.sql.SQLException

import scala.concurrent.Future
import scala.language.postfixOps
import slick.jdbc.{StaticQuery => Q}
import javax.inject._

import models.Tables
import play.api.{Application, Logger}
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.db.slick.DBAction
import types.SampleCode
import util.Transaction
import util.DefaultDb

abstract class ImportToProfileData extends DefaultDb with Transaction {
  def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String) : (SampleCode,String)
  def deleteProfileData(sampleCode: String) : Int
}

@Singleton
class SlickImportToProfileData @Inject() (
    @Named("protoProfileGcDummy") val ppGcD: String, 
    implicit val app: Application) extends ImportToProfileData {

  val logger = Logger(this.getClass())

  val protoProfileData: TableQuery[Tables.ProfileData] = new TableQuery(tag => new Tables.ProfileData(tag, Some("STASH"), "PROFILE_DATA"))
  val protoProfileDataFil: TableQuery[Tables.ProfileDataFiliation] = new TableQuery(tag => new Tables.ProfileDataFiliation(tag, Some("STASH"), "PROFILE_DATA_FILIATION"))
  val protoProfileDataRes: TableQuery[Tables.ProfileDataFiliationResources] = new TableQuery(tag => new Tables.ProfileDataFiliationResources(tag, Some("STASH"), "PROFILE_DATA_FILIATION_RESOURCES"))

  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData // Tables.ProtoProfileData
  val profileMetaDataFiliations: TableQuery[Tables.ProfileDataFiliation] = Tables.ProfileDataFiliation // Tables.ProtoProfileDataFiliation
  val profileMetaDataFiliationResources: TableQuery[Tables.ProfileDataFiliationResources] = Tables.ProfileDataFiliationResources // Tables.ProtoProfileDataFiliationResources

  val laboratories: TableQuery[Tables.Laboratory] = Tables.Laboratory

  private def queryDefineGetResources(id: Column[String]) = for {
    resource <- protoProfileDataRes if (resource.profileDataFiliation === id)
  } yield (resource)

  val queryGetResources = Compiled(queryDefineGetResources _)

  private def queryDefineGetLab(id: Column[Long]) = for {
    ppd <- protoProfileData if (ppd.id === id)
  } yield (ppd.laboratory)

  val queryGetLab = Compiled(queryDefineGetLab _)

  private def queryDefineGetLabByCode(code: Column[String]) = for (
    lab <- laboratories if lab.codeName === code
  ) yield lab

  val queryGetLabByCode = Compiled(queryDefineGetLabByCode _)

  private def queryDefineGetProfileData(sampleCode: Column[String]) = for (
    profileData <- profilesData if (profileData.globalCode === sampleCode)
  ) yield (profileData)

  val queryGetProfileData = Compiled(queryDefineGetProfileData _)

  private def queryDefineGetFiliationResource(sampleCode: Column[String]) = for (
    filiationResource <- profileMetaDataFiliationResources if (filiationResource.profileDataFiliation === sampleCode)
  ) yield (filiationResource)

  val queryGetFiliationResource = Compiled(queryDefineGetFiliationResource _)

  private def queryDefineGetFiliation(sampleCode: Column[String]) = for (
    filiation <- profileMetaDataFiliations if (filiation.profileData === sampleCode)
  ) yield (filiation)

  val queryGetFiliation = Compiled(queryDefineGetFiliation _)

  override def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String) : (SampleCode,String) = DB.withTransaction (implicit s => {

    val nextVal: Long = Q.queryNA[Long]("select nextval('\"APP\".\"PROFILE_DATA_GLOBAL_CODE_seq\"')").first

    val labo = queryGetLab(id).first

    val preFicGc = if (labo == labCode)
      s"$country-$prov-$labCode-"
    else {
      val lab = queryGetLabByCode(labo).firstOption.get
      lab.country + "-" + lab.province + "-" + lab.codeName + "-"
    }

    val gc = preFicGc + nextVal
    val ppGc = ppGcD + id

    val queryPd = protoProfileData.filter(_.id === id).map { x =>
        (x.category,gc, x.internalCode, x.description,
        x.attorney, x.bioMaterialType,
        x.court, x.crimeInvolved, x.crimeType,
        x.criminalCase, x.internalSampleCode,
        assignee, x.laboratory, x.profileExpirationDate,
        x.responsibleGeneticist, x.sampleDate, x.sampleEntryDate)
    }

    profilesData.map { x =>
        (x.category,x.globalCode, x.internalCode,
        x.description, x.attorney, x.bioMaterialType,
        x.court, x.crimeInvolved, x.crimeType,
        x.criminalCase, x.internalSampleCode,
        x.assignee, x.laboratory, x.profileExpirationDate,
        x.responsibleGeneticist, x.sampleDate, x.sampleEntryDate)
    }.insert(queryPd)

    val queryPdf = protoProfileDataFil.filter(_.profileData === ppGc).map { x =>
      (gc, x.fullName, x.nickname, x.birthday, x.birthPlace, x.nationality,
        x.identification, x.identificationIssuingAuthority,
        x.address)
    }

    profileMetaDataFiliations.map { x =>
      (x.profileData, x.fullName, x.nickname, x.birthday, x.birthPlace, x.nationality,
        x.identification, x.identificationIssuingAuthority,
        x.address)
    }.insert(queryPdf)

    queryGetResources(ppGc).list.map { x =>
      profileMetaDataFiliationResources.map { y =>
        (y.profileDataFiliation, y.resource, y.resourceType)
      }.insert((gc, x.resource, x.resourceType))
    }

    (SampleCode(gc),labo)
  })

  override def deleteProfileData(sampleCode: String) : Int = {
    DB.withTransaction(implicit s => {
      queryGetFiliationResource(sampleCode).delete
      queryGetFiliation(sampleCode).delete
      queryGetProfileData(sampleCode).delete
    })
  }
}