package search

import javax.inject.{Inject, Singleton}

import models.Tables
import pdgconf.ExtendedPostgresDriver.simple._
import play.api.Application
import play.api.db.slick.{DB, DBAction}
import play.libs.Akka
import profiledata.{ProfileData, ProfileDataSearch, ProfileDataWithBatch}
import types.{AlphanumericId, SampleCode}

import scala.concurrent.{ExecutionContext, Future}
import slick.ast.TypedType
import slick.lifted.{Column, SimpleFunction}

abstract class FullTextSearch {
  def searchProfiles(search: ProfileDataSearch): Future[Seq[ProfileData]]
  def searchProfilesNodeAssociation(search: ProfileDataSearch): Future[Seq[ProfileDataWithBatch]]
  def searchTotalProfiles(search: ProfileDataSearch): Future[Int]
}

@Singleton
class FullTextSearchPg @Inject() (implicit app: Application) extends FullTextSearch {

  val dExecutionContext: ExecutionContext = DBAction.attributes(DBAction.defaultName).executionContext
  val lExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("play.akka.actor.log-context")

  val pdatas = Tables.ProfileData
  val pdu = Tables.ProfileUploaded
  val pdfs = Tables.ProfileDataFiliation
  val epd = Tables.ExternalProfileData

  val logs = Tables.OperationLogRecord
  val batchProtoProfile= Tables.BatchProtoProfile
  val protoProfile = Tables.ProtoProfile

  def replaceCharacters(inputParam: String):String = {
    Option(inputParam) match {
      case None => inputParam
      case Some(input)  => {
        input.replaceAll("\u000a","-")
          .replaceAll("\u000d","-")
          .replaceAll("\u000c","-")
          .replaceAll("\u0027","-")
          .replaceAll("\n","-")
          .replaceAll("\t","-")
          .replace(" ","-")
      }
    }
  }

  override def searchProfiles(search: ProfileDataSearch): Future[Seq[ProfileData]] = Future {

    DB.withSession { implicit session =>

      createSearchQuery(search.copy(input = replaceCharacters(search.input))).sortBy(_._1.globalCode.asc).drop(search.page * search.pageSize).take(search.pageSize).iterator.toVector map {
        case (pd, _,epd) =>
          ProfileData(AlphanumericId(pd.category),
            SampleCode(pd.globalCode), pd.attorney, pd.bioMaterialType,
            pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
            pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted, None,
            pd.responsibleGeneticist, pd.profileExpirationDate, pd.sampleDate,
            pd.sampleEntryDate, None,epd.isDefined)
      }

    }
  }(dExecutionContext)

  override def searchProfilesNodeAssociation(search: ProfileDataSearch): Future[Seq[ProfileDataWithBatch]] = Future {

    DB.withSession { implicit session =>

      val result = createSearchQueryLabel(search.copy(input = replaceCharacters(search.input))).drop(search.page * search.pageSize).take(search.pageSize).iterator.toVector

      result map {
        case ((pd, _),(_,bpp)) =>
          ProfileDataWithBatch(AlphanumericId(pd.category),
            SampleCode(pd.globalCode), pd.attorney, pd.bioMaterialType,
            pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
            pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted, None,
            pd.responsibleGeneticist, pd.profileExpirationDate, pd.sampleDate,
            pd.sampleEntryDate, None,bpp.flatMap(_.label))
      }

    }
  }(dExecutionContext)

  override def searchTotalProfiles(search: ProfileDataSearch): Future[Int] = Future {
    DB.withSession { implicit session =>
      createSearchQuery(search).list.length
    }
  }(dExecutionContext)

  //private def coalesce[T](c: Column[Option[T]], default: Column[T])(implicit arg0: TypedType[T]) = SimpleFunction[T]("coalesce").apply(Seq(c, default, ???))
  private def coalesce[T](c: Column[Option[T]], default: Column[T])(implicit arg0: TypedType[T]) = SimpleFunction.binary[Option[T], T, T]("coalesce").apply(c, default)

  def createSearchQuery(search: ProfileDataSearch) = {

    //def coalesce2[T](c: Column[T], default: Column[T])(implicit arg0: TypedType[T]) = SimpleFunction.binary[T, T, T]("coalesce").apply(c, default)

    val query = pdatas.leftJoin(pdfs).on(_.id === _.id).leftJoin(pdu).on(_._1.id === _.id).leftJoin(epd).on(_._1._1.id === _.id)

    val searchFields = (tup: (Tables.ProfileData, Tables.ProfileDataFiliation)) => {
      val (pd, pdf) = tup

      pd.category ++ " " ++
        pd.globalCode ++ " " ++
        pd.internalCode ++ " " ++
        coalesce(pd.description, "") ++ " " ++
        coalesce(pd.attorney, "") ++ " " ++
        coalesce(pd.bioMaterialType, "") ++ " " ++
        coalesce(pd.court, "") ++ " " ++
        coalesce(pd.crimeInvolved, "") ++ " " ++
        coalesce(pd.crimeType, "") ++ " " ++
        coalesce(pd.criminalCase, "") ++ " " ++
        pd.internalSampleCode ++ " " ++
        pd.assignee ++ " " ++
        pd.laboratory ++ " " ++
        coalesce(pd.responsibleGeneticist, "") ++ " " ++
        pdf.fullName.getOrElse("")  ++ " " ++
        pdf.identification.getOrElse("")  ++ " " ++
        pdf.nationality.getOrElse("") ++ " " ++
        pdf.address.getOrElse("")
    }

    val inputLike = if (search.input != null && search.input.trim.nonEmpty) search.input + ":*" else search.input
    var profiles = query
      .filter {
        record => {
          if(search.notUploaded.contains(true)){
            if(search.category.isEmpty) {
              record._2.id.isNull && ((record._1._1._1.deleted && search.inactive) || (!record._1._1._1.deleted && search.active)) &&
                toTsVector(searchFields(record._1._1)).setWeight('A') @@ toTsQuery(inputLike)
            }else{
              record._2.id.isNull && ((record._1._1._1.deleted && search.inactive) || (!record._1._1._1.deleted && search.active)) &&
                record._1._1._1.category===search.category &&
                toTsVector(searchFields(record._1._1)).setWeight('A') @@ toTsQuery(inputLike)
            }
          }else{
            if(search.category.isEmpty) {
              ((record._1._1._1.deleted && search.inactive) || (!record._1._1._1.deleted && search.active)) &&
                toTsVector(searchFields(record._1._1)).setWeight('A') @@ toTsQuery(inputLike)
            }else{
              ((record._1._1._1.deleted && search.inactive) || (!record._1._1._1.deleted && search.active)) &&
                record._1._1._1.category ===search.category &&
                toTsVector(searchFields(record._1._1)).setWeight('A') @@ toTsQuery(inputLike)
            }
          }
        }
      }
      .map {
        case (((pd, pdf), pu), epd)=> (pd, tsRank(tsVector(searchFields((pd, pdf))).setWeight('A'), toTsQuery(inputLike)),epd.?)
      }

    if (!search.isSuperUser) {
      profiles = profiles.filter {
        pd => pd._1.assignee === search.userId
      }
    }
profiles
    //profiles.sortBy(_._2)
  }

  def createSearchQueryLabel(search: ProfileDataSearch) = {

    //def coalesce2[T](c: Column[T], default: Column[T])(implicit arg0: TypedType[T]) = SimpleFunction.binary[T, T, T]("coalesce").apply(c, default)

    val query = (pdatas.leftJoin (pdfs).on(_.id === _.id)) innerJoin  (protoProfile innerJoin batchProtoProfile on (_.idBatch === _.id)) on (_._1.internalSampleCode === _._1.sampleName )


    val searchFields = (tup: ((Tables.ProfileData, Tables.ProfileDataFiliation),(Tables.ProtoProfile, Tables.BatchProtoProfile))) => {
      val ((pd, pdf),(_,bpp)) = tup

      pd.category ++ " " ++
        pd.globalCode ++ " " ++
        pd.internalCode ++ " " ++
        coalesce(pd.description, "") ++ " " ++
        coalesce(pd.attorney, "") ++ " " ++
        coalesce(pd.bioMaterialType, "") ++ " " ++
        coalesce(pd.court, "") ++ " " ++
        coalesce(pd.crimeInvolved, "") ++ " " ++
        coalesce(pd.crimeType, "") ++ " " ++
        coalesce(pd.criminalCase, "") ++ " " ++
        pd.internalSampleCode ++ " " ++
        pd.assignee ++ " " ++
        pd.laboratory ++ " " ++
        coalesce(pd.responsibleGeneticist, "") ++ " " ++
        pdf.fullName.getOrElse("")  ++ " " ++
        pdf.identification.getOrElse("")  ++ " " ++
        coalesce(bpp.label,"") ++ " " ++
        pdf.nationality.getOrElse("")  ++ " " ++
        pdf.address.getOrElse("")

    }

    val inputLike = if (search.input != null && search.input.trim.nonEmpty) search.input + ":*" else search.input
    var profiles = query
      .filter {
        record => ((record._1._1.deleted && search.inactive) || (!record._1._1.deleted && search.active)) &&
          toTsVector(searchFields(record)).setWeight('A') @@ toTsQuery(inputLike)
      }
      .map {
        case ((pd, pdf),(pp,bpp)) => ((pd, tsRank(tsVector(searchFields((pd,pdf),(pp,bpp))).setWeight('A'), toTsQuery(inputLike))),(pp,bpp.?))
      }


    if (!search.isSuperUser) {
      profiles = profiles.filter {
        pd => pd._1._1.assignee === search.userId
      }
    }

    profiles.sortBy(_._1._2)
  }
}
