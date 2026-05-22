package search

import javax.inject.{Inject, Singleton}
import profiledata.{ProfileData, ProfileDataSearch, ProfileDataWithBatch}
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{ExecutionContext, Future}
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.GetResult

abstract class FullTextSearch {
  def searchProfiles(search: ProfileDataSearch): Future[Seq[ProfileData]]
  def searchProfilesNodeAssociation(search: ProfileDataSearch): Future[Seq[ProfileDataWithBatch]]
  def searchTotalProfiles(search: ProfileDataSearch): Future[Int]
}

@Singleton
class FullTextSearchPg @Inject()(db: Database)(implicit ec: ExecutionContext)
    extends FullTextSearch {

  // Mirrors the legacy replaceCharacters() exactly — uses Char literals to avoid encoding issues
  private[search] def replaceCharacters(input: String): String =
    Option(input).fold(input) { s =>
      s.replace('\n', '-')  // LF
       .replace('\r', '-')  // CR
       .replace('\f', '-')  // FF
       .replace('\'', '-') // single quote
       .replace('\t', '-')
       .replace(' ', '-')
    }

  private[search] def toTsQueryInput(raw: String): String = {
    val sanitized = replaceCharacters(raw)
    if (sanitized != null && sanitized.trim.nonEmpty) sanitized + ":*" else sanitized
  }

  // Derived from Scala Booleans — safe for literal SQL injection via #$
  private[search] def statusFragment(active: Boolean, inactive: Boolean): String =
    (active, inactive) match {
      case (true, true)  => "(pd.\"DELETED\" = true OR pd.\"DELETED\" = false)"
      case (true, false) => "pd.\"DELETED\" = false"
      case (false, true) => "pd.\"DELETED\" = true"
      case _             => "1=0"
    }

  // Derived from Scala Boolean — safe for literal injection
  private[search] def notUploadedFragment(notUploaded: Option[Boolean]): String =
    if (notUploaded.contains(true)) """AND pu."ID" IS NULL""" else ""

  private implicit val getProfileDataResult: GetResult[ProfileData] = GetResult { r =>
    ProfileData(
      AlphanumericId(r.<<[String]),   // category
      SampleCode(r.<<[String]),       // globalCode
      r.<<?[String],                  // attorney
      r.<<?[String],                  // bioMaterialType
      r.<<?[String],                  // court
      r.<<?[String],                  // crimeInvolved
      r.<<?[String],                  // crimeType
      r.<<?[String],                  // criminalCase
      r.<<[String],                   // internalSampleCode
      r.<<[String],                   // assignee
      r.<<[String],                   // laboratory
      r.<<[Boolean],                  // deleted
      r.<<?[String],                  // responsibleGeneticist
      r.<<?[java.sql.Date],           // profileExpirationDate
      r.<<?[java.sql.Date],           // sampleDate
      r.<<?[java.sql.Date],           // sampleEntryDate
      r.<<[Boolean]                   // isExternal
    )
  }

  private implicit val getProfileDataWithBatchResult: GetResult[ProfileDataWithBatch] = GetResult { r =>
    ProfileDataWithBatch(
      AlphanumericId(r.<<[String]),   // category
      SampleCode(r.<<[String]),       // globalCode
      r.<<?[String],                  // attorney
      r.<<?[String],                  // bioMaterialType
      r.<<?[String],                  // court
      r.<<?[String],                  // crimeInvolved
      r.<<?[String],                  // crimeType
      r.<<?[String],                  // criminalCase
      r.<<[String],                   // internalSampleCode
      r.<<[String],                   // assignee
      r.<<[String],                   // laboratory
      r.<<[Boolean],                  // deleted
      None,                           // deletedMotive (always None in search results)
      r.<<?[String],                  // responsibleGeneticist
      r.<<?[java.sql.Date],           // profileExpirationDate
      r.<<?[java.sql.Date],           // sampleDate
      r.<<?[java.sql.Date],           // sampleEntryDate
      None,                           // dataFiliation (not loaded in search)
      r.<<?[String]                   // label
    )
  }

  // Pure SQL constant fragments — safe for #$ literal injection
  private val tsvectorFields: String =
    """COALESCE(pd."CATEGORY",'') || ' ' ||
      COALESCE(pd."GLOBAL_CODE",'') || ' ' ||
      COALESCE(pd."INTERNAL_CODE",'') || ' ' ||
      COALESCE(pd."DESCRIPTION",'') || ' ' ||
      COALESCE(pd."ATTORNEY",'') || ' ' ||
      COALESCE(pd."BIO_MATERIAL_TYPE",'') || ' ' ||
      COALESCE(pd."COURT",'') || ' ' ||
      COALESCE(pd."CRIME_INVOLVED",'') || ' ' ||
      COALESCE(pd."CRIME_TYPE",'') || ' ' ||
      COALESCE(pd."CRIMINAL_CASE",'') || ' ' ||
      COALESCE(pd."INTERNAL_SAMPLE_CODE",'') || ' ' ||
      COALESCE(pd."ASSIGNEE",'') || ' ' ||
      COALESCE(pd."LABORATORY",'') || ' ' ||
      COALESCE(pd."RESPONSIBLE_GENETICIST",'') || ' ' ||
      COALESCE(pdf."FULL_NAME",'') || ' ' ||
      COALESCE(pdf."IDENTIFICATION",'') || ' ' ||
      COALESCE(pdf."NATIONALITY",'') || ' ' ||
      COALESCE(pdf."ADDRESS",'')"""

  private val tsvectorFieldsWithBatch: String =
    """COALESCE(pd."CATEGORY",'') || ' ' ||
      COALESCE(pd."GLOBAL_CODE",'') || ' ' ||
      COALESCE(pd."INTERNAL_CODE",'') || ' ' ||
      COALESCE(pd."DESCRIPTION",'') || ' ' ||
      COALESCE(pd."ATTORNEY",'') || ' ' ||
      COALESCE(pd."BIO_MATERIAL_TYPE",'') || ' ' ||
      COALESCE(pd."COURT",'') || ' ' ||
      COALESCE(pd."CRIME_INVOLVED",'') || ' ' ||
      COALESCE(pd."CRIME_TYPE",'') || ' ' ||
      COALESCE(pd."CRIMINAL_CASE",'') || ' ' ||
      COALESCE(pd."INTERNAL_SAMPLE_CODE",'') || ' ' ||
      COALESCE(pd."ASSIGNEE",'') || ' ' ||
      COALESCE(pd."LABORATORY",'') || ' ' ||
      COALESCE(pd."RESPONSIBLE_GENETICIST",'') || ' ' ||
      COALESCE(pdf."FULL_NAME",'') || ' ' ||
      COALESCE(pdf."IDENTIFICATION",'') || ' ' ||
      COALESCE(bpp."LABEL",'') || ' ' ||
      COALESCE(pdf."NATIONALITY",'') || ' ' ||
      COALESCE(pdf."ADDRESS",'')"""

  override def searchProfiles(search: ProfileDataSearch): Future[Seq[ProfileData]] = {
    val inputLike     = toTsQueryInput(search.input)
    val offset        = search.page * search.pageSize
    val limit         = search.pageSize
    val statusFrag    = statusFragment(search.active, search.inactive)
    val notUploadFrag = notUploadedFragment(search.notUploaded)
    // user-supplied values — always parameterized with $
    val isSuperUser   = search.isSuperUser
    val userId        = search.userId
    val category      = search.category

    val query = sql"""
      SELECT
        pd."CATEGORY",
        pd."GLOBAL_CODE",
        pd."ATTORNEY",
        pd."BIO_MATERIAL_TYPE",
        pd."COURT",
        pd."CRIME_INVOLVED",
        pd."CRIME_TYPE",
        pd."CRIMINAL_CASE",
        pd."INTERNAL_SAMPLE_CODE",
        pd."ASSIGNEE",
        pd."LABORATORY",
        pd."DELETED",
        pd."RESPONSIBLE_GENETICIST",
        pd."PROFILE_EXPIRATION_DATE",
        pd."SAMPLE_DATE",
        pd."SAMPLE_ENTRY_DATE",
        (epd."ID" IS NOT NULL) AS "IS_EXTERNAL"
      FROM "APP"."PROFILE_DATA" pd
      LEFT JOIN "APP"."PROFILE_DATA_FILIATION" pdf ON pd."ID" = pdf."ID"
      LEFT JOIN "APP"."PROFILE_UPLOADED" pu ON pd."ID" = pu."ID"
      LEFT JOIN "APP"."EXTERNAL_PROFILE_DATA" epd ON pd."ID" = epd."ID"
      WHERE #$statusFrag
        #$notUploadFrag
        AND ($category = '' OR pd."CATEGORY" = $category)
        AND ($isSuperUser OR pd."ASSIGNEE" = $userId)
        AND to_tsvector('simple', #$tsvectorFields)
            @@ to_tsquery('simple', $inputLike)
      ORDER BY pd."GLOBAL_CODE" ASC
      LIMIT $limit OFFSET $offset
    """.as[ProfileData]

    db.run(query)
  }

  override def searchTotalProfiles(search: ProfileDataSearch): Future[Int] = {
    val inputLike     = toTsQueryInput(search.input)
    val statusFrag    = statusFragment(search.active, search.inactive)
    val notUploadFrag = notUploadedFragment(search.notUploaded)
    val isSuperUser   = search.isSuperUser
    val userId        = search.userId
    val category      = search.category

    val query = sql"""
      SELECT COUNT(*)
      FROM "APP"."PROFILE_DATA" pd
      LEFT JOIN "APP"."PROFILE_DATA_FILIATION" pdf ON pd."ID" = pdf."ID"
      LEFT JOIN "APP"."PROFILE_UPLOADED" pu ON pd."ID" = pu."ID"
      WHERE #$statusFrag
        #$notUploadFrag
        AND ($category = '' OR pd."CATEGORY" = $category)
        AND ($isSuperUser OR pd."ASSIGNEE" = $userId)
        AND to_tsvector('simple', #$tsvectorFields)
            @@ to_tsquery('simple', $inputLike)
    """.as[Int]

    db.run(query).map(_.head)
  }

  override def searchProfilesNodeAssociation(search: ProfileDataSearch): Future[Seq[ProfileDataWithBatch]] = {
    val inputLike   = toTsQueryInput(search.input)
    val offset      = search.page * search.pageSize
    val limit       = search.pageSize
    val statusFrag  = statusFragment(search.active, search.inactive)
    val isSuperUser = search.isSuperUser
    val userId      = search.userId

    val query = sql"""
      SELECT
        pd."CATEGORY",
        pd."GLOBAL_CODE",
        pd."ATTORNEY",
        pd."BIO_MATERIAL_TYPE",
        pd."COURT",
        pd."CRIME_INVOLVED",
        pd."CRIME_TYPE",
        pd."CRIMINAL_CASE",
        pd."INTERNAL_SAMPLE_CODE",
        pd."ASSIGNEE",
        pd."LABORATORY",
        pd."DELETED",
        pd."RESPONSIBLE_GENETICIST",
        pd."PROFILE_EXPIRATION_DATE",
        pd."SAMPLE_DATE",
        pd."SAMPLE_ENTRY_DATE",
        bpp."LABEL"
      FROM "APP"."PROFILE_DATA" pd
      LEFT JOIN "APP"."PROFILE_DATA_FILIATION" pdf ON pd."ID" = pdf."ID"
      INNER JOIN "APP"."PROTO_PROFILE" pp ON pd."INTERNAL_SAMPLE_CODE" = pp."SAMPLE_NAME"
      INNER JOIN "APP"."BATCH_PROTO_PROFILE" bpp ON pp."ID_BATCH" = bpp."ID"
      WHERE #$statusFrag
        AND ($isSuperUser OR pd."ASSIGNEE" = $userId)
        AND to_tsvector('simple', #$tsvectorFieldsWithBatch)
            @@ to_tsquery('simple', $inputLike)
      ORDER BY ts_rank(
        to_tsvector('simple', #$tsvectorFieldsWithBatch),
        to_tsquery('simple', $inputLike)
      ) DESC
      LIMIT $limit OFFSET $offset
    """.as[ProfileDataWithBatch]

    db.run(query)
  }
}
