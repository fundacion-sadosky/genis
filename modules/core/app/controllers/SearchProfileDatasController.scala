package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.*
import profiledata.{ProfileData, ProfileDataSearch}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api.*
import models.Tables
import models.Tables.*
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SearchProfileDatasController @Inject()(
  cc: ControllerComponents,
  db: Database
)(using ec: ExecutionContext) extends AbstractController(cc):

  def search: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataSearch].fold(
      _ => Future.successful(BadRequest),
      s => queryProfiles(s).map(list => Ok(Json.toJson(list)))
    )
  }

  def searchTotal: Action[JsValue] = Action.async(parse.json) { request =>
    request.body.validate[ProfileDataSearch].fold(
      _ => Future.successful(BadRequest),
      s => queryCount(s).map { case (pageCount, total) =>
        Ok("").withHeaders(
          "X-PROFILES-LENGTH"       -> pageCount.toString,
          "X-PROFILES-TOTAL-LENGTH" -> total.toString
        )
      }
    )
  }

  private def baseQuery(s: ProfileDataSearch): Query[ProfileDataTable, ProfileDataRow, Seq] =
    profilesData
      .filter { pd =>
        val byDeleted =
          if s.active && s.inactive then true.bind
          else if s.active                then !pd.deleted
          else if s.inactive              then pd.deleted
          else false.bind

        val byUser =
          if s.isSuperUser then true.bind else pd.assignee === s.userId

        val byCat =
          if s.category.isEmpty then true.bind else pd.category === s.category

        byDeleted && byUser && byCat
      }

  private def queryProfiles(s: ProfileDataSearch): Future[Seq[ProfileData]] =
    db.run(
      baseQuery(s)
        .sortBy(_.globalCode)
        .drop(s.page * s.pageSize)
        .take(s.pageSize)
        .result
    ).map(_.map(rowToProfileData))

  private def queryCount(s: ProfileDataSearch): Future[(Int, Int)] =
    val q = baseQuery(s)
    for
      total <- db.run(q.length.result)
      page  <- db.run(q.drop(s.page * s.pageSize).take(s.pageSize).length.result)
    yield (page, total)

  private def rowToProfileData(r: ProfileDataRow): ProfileData =
    ProfileData(
      category              = AlphanumericId(r.category),
      globalCode            = SampleCode(r.globalCode),
      attorney              = r.attorney,
      bioMaterialType       = r.bioMaterialType,
      court                 = r.court,
      crimeInvolved         = r.crimeInvolved,
      crimeType             = r.crimeType,
      criminalCase          = r.criminalCase,
      internalSampleCode    = r.internalSampleCode,
      assignee              = r.assignee,
      laboratory            = r.laboratory,
      deleted               = r.deleted,
      deletedMotive         = None,
      responsibleGeneticist = r.responsibleGeneticist,
      profileExpirationDate = r.profileExpirationDate.map(d => new java.util.Date(d.getTime)),
      sampleDate            = r.sampleDate.map(d => new java.util.Date(d.getTime)),
      sampleEntryDate       = r.sampleEntryDate.map(d => new java.util.Date(d.getTime)),
      dataFiliation         = None,
      isExternal            = false
    )
