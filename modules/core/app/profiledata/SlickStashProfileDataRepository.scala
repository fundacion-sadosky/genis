package profiledata

import jakarta.inject.{Inject, Singleton}
import models.Tables
import models.Tables.ProfileDataFiliationRow
import play.api.i18n.MessagesApi
import slick.jdbc.PostgresProfile.api.*
import types.SampleCode

import scala.concurrent.{ExecutionContext, Future}

// Uses STASH schema tables (equivalent to ProtoProfileDataRepository in legacy).
// Overrides only the tables that differ (PROFILE_DATA, PROFILE_DATA_FILIATION,
// PROFILE_DATA_FILIATION_RESOURCES); all other tables remain APP schema.
@Singleton
class SlickStashProfileDataRepository @Inject()(
  db: slick.jdbc.JdbcBackend.Database,
  messagesApi: MessagesApi
)(implicit ec: ExecutionContext) extends SlickProfileDataRepository(db, messagesApi):

  override protected val pdTable     = Tables.stashProfileData
  override protected val pdfTable    = Tables.stashProfileDataFiliation
  override protected val pdfResTable = Tables.stashProfileDataFiliationResources

  // Legacy ProtoProfileDataRepository.updateProfileData deliberately excludes
  // the `category` column — proto-profile category is set at creation and updated
  // separately via modifyCategory/updateProtoProfileData. Including it here would
  // let a caller silently overwrite the category on every field edit.
  override def updateProfileData(
    globalCode: SampleCode,
    newProfile: ProfileData,
    imageList: Option[List[java.io.File]] = None,
    picturesList: Option[List[java.io.File]] = None,
    signaturesList: Option[List[java.io.File]] = None
  ): Future[Boolean] =
    val action = for
      updated <- pdTable
                   .filter(_.globalCode === globalCode.text)
                   .map(pd => (pd.attorney, pd.bioMaterialType, pd.court, pd.crimeInvolved,
                               pd.crimeType, pd.criminalCase, pd.responsibleGeneticist,
                               pd.profileExpirationDate, pd.sampleDate, pd.sampleEntryDate, pd.laboratory))
                   .update((
                     newProfile.attorney, newProfile.bioMaterialType, newProfile.court,
                     newProfile.crimeInvolved, newProfile.crimeType, newProfile.criminalCase,
                     newProfile.responsibleGeneticist,
                     newProfile.profileExpirationDate.map(d => new java.sql.Date(d.getTime)),
                     newProfile.sampleDate.map(d => new java.sql.Date(d.getTime)),
                     newProfile.sampleEntryDate.map(d => new java.sql.Date(d.getTime)),
                     newProfile.laboratory
                   ))
      _ <- newProfile.dataFiliation.fold(DBIO.successful(()))(df => upsertStashFiliation(globalCode.text, df, imageList, picturesList, signaturesList))
    yield updated >= 1
    db.run(action.transactionally)

  private def upsertStashFiliation(
    gc: String, df: DataFiliation,
    imageList: Option[List[java.io.File]],
    picturesList: Option[List[java.io.File]],
    signaturesList: Option[List[java.io.File]]
  ): DBIO[Unit] =
    val fData = (df.fullName, df.nickname, df.birthday.map(d => new java.sql.Date(d.getTime)),
                 df.birthPlace, df.nationality, df.identification, df.identificationIssuingAuthority, df.address)
    for
      updated <- pdfTable
                   .filter(_.profileData === gc)
                   .map(pdf => (pdf.fullName, pdf.nickname, pdf.birthday, pdf.birthPlace, pdf.nationality,
                                pdf.identification, pdf.identificationIssuingAuthority, pdf.address))
                   .update(fData)
      _ <- (if updated == 0 then
               pdfTable += ProfileDataFiliationRow(0L, gc, df.fullName, df.nickname,
                 df.birthday.map(d => new java.sql.Date(d.getTime)), df.birthPlace, df.nationality,
                 df.identification, df.identificationIssuingAuthority, df.address)
             else DBIO.successful(0))
      _ <- insertResources(gc, "I", imageList)
      _ <- insertResources(gc, "P", picturesList)
      _ <- insertResources(gc, "S", signaturesList)
    yield ()
