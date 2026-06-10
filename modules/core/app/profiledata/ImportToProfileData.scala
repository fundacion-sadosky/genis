package profiledata

import jakarta.inject.{Inject, Named, Singleton}
import models.Tables
import play.api.Logging
import slick.jdbc.PostgresProfile.api.*
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

trait ImportToProfileData {
  def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String, desktopSearch: Boolean): Future[(SampleCode, String)]
  def deleteProfileData(sampleCode: String): Future[Int]
}

@Singleton
class SlickImportToProfileData @Inject()(
    db: Database,
    @Named("protoProfileGcDummy") ppGcD: String
)(implicit ec: ExecutionContext) extends ImportToProfileData with Logging:

  private val stashProfileData              = Tables.stashProfileData
  private val stashProfileDataFiliation     = Tables.stashProfileDataFiliation
  private val stashProfileDataFiliationRes  = Tables.stashProfileDataFiliationResources
  private val profilesData                  = Tables.profilesData
  private val profileDataFiliations         = Tables.profileDataFiliations
  private val profileDataFiliationResources = Tables.profileDataFiliationResources
  private val laboratories                  = Tables.laboratories

  private def labPrefixAction(labo: String, labCode: String, country: String, prov: String): DBIO[String] =
    if labo == labCode then
      DBIO.successful(s"$country-$prov-$labCode-")
    else
      laboratories.filter(_.codeName === labo).result.headOption.map { labOpt =>
        val lab = labOpt.get
        s"${lab.country}-${lab.province}-${lab.codeName}-"
      }

  override def fromProtoProfileData(id: Long, labCode: String, country: String, prov: String, assignee: String, desktopSearch: Boolean = false): Future[(SampleCode, String)] =
    val action = for
      nextVal  <- sql"""select nextval('"APP"."PROFILE_DATA_GLOBAL_CODE_seq"')""".as[Long].head
      labo     <- stashProfileData.filter(_.id === id).map(_.laboratory).result.head
      preFicGc <- labPrefixAction(labo, labCode, country, prov)
      gc        = preFicGc + nextVal
      ppGc      = ppGcD + id
      pdRow    <- stashProfileData.filter(_.id === id).result.head
      _        <- profilesData += pdRow.copy(id = 0L, globalCode = gc, assignee = assignee,
                    deleted = false, deletedSolicitor = None, deletedMotive = None, fromDesktopSearch = desktopSearch)
      pdfRows  <- stashProfileDataFiliation.filter(_.profileData === ppGc).result
      _        <- profileDataFiliations ++= pdfRows.map(_.copy(id = 0L, profileData = gc))
      resRows  <- stashProfileDataFiliationRes.filter(_.profileDataFiliation === ppGc).result
      _        <- profileDataFiliationResources ++= resRows.map(_.copy(id = 0L, profileDataFiliation = gc))
    yield (SampleCode(gc), labo)
    db.run(action.transactionally)

  override def deleteProfileData(sampleCode: String): Future[Int] =
    val action = for
      _ <- profileDataFiliationResources.filter(_.profileDataFiliation === sampleCode).delete
      _ <- profileDataFiliations.filter(_.profileData === sampleCode).delete
      n <- profilesData.filter(_.globalCode === sampleCode).delete
    yield n
    db.run(action.transactionally)
