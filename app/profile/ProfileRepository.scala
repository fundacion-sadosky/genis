package profile

import java.util.Date
import configdata.MatchingRule
import connections.FileInterconnection
import org.apache.commons.codec.binary.Base64
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import util.FutureUtils
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import profile.GenotypificationByType.{GenotypificationByType, _}
import profile.Profile._ //{LabeledGenotypification, Mismatch}
import reactivemongo.api.Cursor
import reactivemongo.bson.{BSONObjectID, _}
import reactivemongo.core.commands.{FindAndModify, Update}
import types._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import javax.inject.{Inject, Named, Singleton}

import sttp.client3._
import sttp.client3.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.Decoder
import io.circe.generic.semiauto._


abstract class ProfileRepository {

  def get(id: SampleCode): Future[Option[Profile]]

  def getBy(user: String,isSuperUser: Boolean,internalSampleCode: Option[String] = None,categoryId: Option[String] = None,
            laboratory: Option[String] = None,
            hourFrom: Option[Date] = None,
            hourUntil: Option[Date] = None): Future[List[Profile]]

  def getBetweenDates(hourFrom: Option[Date] = None, hourUntil: Option[Date] = None): Future[List[Profile]]

  def findByCode(globalCode: SampleCode): Future[Option[Profile]]

  def add(profile: Profile): Future[SampleCode]

  def addElectropherogram(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String): Future[Either[String, SampleCode]]

  def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]]

  def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]

  def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]

  def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]]

  def getFullFilesByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]]

  def addElectropherogramWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte],name:String,id:String): Future[Either[String, SampleCode]]

  def addFileWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte],name:String,id:String): Future[Either[String, SampleCode]]

  def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]]

  def findByCodes(globalCodes: Seq[SampleCode]): Future[Seq[Profile]]

  def addAnalysis(_id: SampleCode, analysis: Analysis, genotypification: GenotypificationByType, labeledGenotypification: Option[Profile.LabeledGenotypification], matchingRules: Option[Seq[MatchingRule]], mismatches: Option[Profile.Mismatch]): Future[SampleCode]

  def saveLabels(globalCode: SampleCode, labels: Profile.LabeledGenotypification): Future[SampleCode]

  def existProfile(globalCode: SampleCode): Future[Boolean]

  def delete(globalCode: SampleCode): Future[Either[String, SampleCode]]

  def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]]

  def updateAssocTo(globalCode: SampleCode, to: SampleCode): Future[(String, String, SampleCode)]

  def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]]

  def getUnprocessed(): Future[Seq[SampleCode]]

  def canDeleteKit(id: String): Future[Boolean]

  def updateProfile(profile: Profile): Future[SampleCode]

  def findByCodeWithoutAceptedLocus(globalCode: SampleCode, aceptedLocus: Seq[String]): Future[Option[Profile]]

  def addFile(globalCode: SampleCode, analysisId: String, image: Array[Byte],name:String): Future[Either[String, SampleCode]]

  def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]]

  def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]]

  def getFileByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]]

  def getFullElectropherogramsById(id: String): Future[List[connections.FileInterconnection]]

  def getFullFilesById(id: String): Future[List[connections.FileInterconnection]]

  def getProfilesMarkers(profiles: Array[Profile]) :List[String]

  def removeFile(id: String):Future[Either[String,String]]

  def removeEpg(id: String):Future[Either[String,String]]

  def getProfileOwnerByFileId(id: String):Future[(String,SampleCode)]

  def getProfileOwnerByEpgId(id: String): Future[(String,SampleCode)]

  def getAllProfiles(): Future[List[(SampleCode, String)]]

}

class MongoProfileRepository extends ProfileRepository {
  private def profiles = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("profiles")), Duration(10, SECONDS))

  private def electropherograms = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("electropherograms")), Duration(10, SECONDS))

  private def files = Await.result(play.modules.reactivemongo.ReactiveMongoPlugin.database.map(_.collection[JSONCollection]("files")), Duration(10, SECONDS))

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = {

    val set: JsObject = Json.obj("$set" -> Json.obj("deleted" -> true))

    profiles.update(Json.obj("_id" -> globalCode), set).map { lastError =>
      if (lastError.ok)
        Right(globalCode)
      else
        Left(lastError.errmsg.getOrElse("Error"))
    }
  }

  def get(id: SampleCode): Future[Option[Profile]] = {
    profiles.find(Json.obj("_id" -> id))
      .sort(Json.obj("_id" -> -1))
      .one[Profile]
  }

  def findAll(): Future[List[Profile]] = ???
// no usages
//  {
//    profiles
//      .find(Json.obj())
//      .cursor[Profile]()
//      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Profile]]())
//  }

  def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {
    //println("Mongo busca: " + globalCode.text)
    profiles
      .find(Json.obj("globalCode" -> globalCode))
      .sort(Json.obj("globalCode" -> -1))
      .one[Profile]
  }

  override def findByCodeWithoutAceptedLocus(globalCode: SampleCode, aceptedLocus: Seq[String]): Future[Option[Profile]] = {
    profiles
      .find(Json.obj("globalCode" -> globalCode))
      .projection(
        Json.toJson(aceptedLocus.map(x => "genotypification.1." + x).map(x => (x, false)).toMap).as[JsObject]
      )
      .sort(Json.obj("globalCode" -> -1))
      .one[Profile]
  }

  def add(profile: Profile): Future[SampleCode] = {
    profiles.insert(profile).map { result => profile._id }
  }

  override def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]] = {
    val set: JsObject = Json.obj("$set" -> Json.obj("matcheable" -> true, "processed" -> true))

    profiles.update(Json.obj("_id" -> globalCode), set).map { lastError =>
      if (lastError.ok)
        Right(globalCode)
      else
        Left(lastError.errmsg.getOrElse("Error"))
    }
  }

  override def getUnprocessed(): Future[Seq[SampleCode]] = {

    val query = Json.obj("processed" -> false)
    val projection = Json.obj("_id" -> 0, "globalCode" -> 1)

    val cursor = profiles
      .find(query, projection)
      .cursor[BSONDocument]()
    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map {
        element => SampleCode(element.getAs[String]("globalCode").get)
      }
    }
  }

  override def addAnalysis(_id: SampleCode, analysis: Analysis, genotypification: GenotypificationByType, labeledGenotypification: Option[Profile.LabeledGenotypification], matchingRules: Option[Seq[MatchingRule]], mismatches: Option[Profile.Mismatch]): Future[SampleCode] = {
    profiles.update(Json.obj("_id" -> _id),
      Json.obj("$set" ->
        Json.obj(
          "genotypification" -> genotypification,
          "labeledGenotypification" -> labeledGenotypification,
          "matchingRules" -> matchingRules,
          "mismatches" -> mismatches,
          "matcheable" -> false,
          "processed" -> false),
        "$push" -> Json.obj("analyses" -> analysis))).map {
      case result if result.ok => _id
      case error => throw new RuntimeException(error.errmsg.getOrElse("Error"))
    }
  }

  def updateProfile(profile: Profile): Future[SampleCode] = {
    profiles.update(Json.obj("_id" -> profile._id),
      Json.obj("$set" ->
        Json.obj(
          "internalSampleCode" -> profile.internalSampleCode,
          "assignee" -> profile.assignee,
          "categoryId" -> profile.categoryId,
          "genotypification" -> profile.genotypification,
          "analyses" -> profile.analyses,
          "labeledGenotypification" -> profile.labeledGenotypification,
          "contributors" -> profile.contributors,
          "matchingRules" -> profile.matchingRules,
          "associatedTo" -> profile.associatedTo,
          "deleted" -> profile.deleted,
          "matcheable" -> false,
          "isReference" -> profile.isReference,
          "processed" -> false)
      )).map {
      case result if result.ok => profile._id
      case error => throw new RuntimeException(error.errmsg.getOrElse("Error"))
    }
  }

  def addElectropherogram(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String = null): Future[Either[String, SampleCode]] = {
    val array = BSONBinary(image, Subtype.GenericBinarySubtype)
    val imageToStore = if (name!=null)
      BSONDocument("profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> array,
        "name" -> name) else
      BSONDocument("profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> array)

    val result = electropherograms.insert(imageToStore)
    result.map { result => Right(globalCode) }
      .recover { case error => Left(error.getMessage) }
  }
  def addElectropherogramWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte],name: String, id:String): Future[Either[String, SampleCode]] = {
    val array = BSONBinary(image, Subtype.GenericBinarySubtype)
    val imageToStore = if (name!=null)
      BSONDocument("_id" -> BSONObjectID(id),
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> array,
        "name" -> name) else
      BSONDocument("_id" -> BSONObjectID(id),
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> array)

    val result = electropherograms.insert(imageToStore)
    result.map { result => Right(globalCode) }
      .recover { case error => Left(error.getMessage) }
  }

  override def existProfile(globalCode: SampleCode): Future[Boolean] = {
    profiles.count(Some(Json.obj("_id" -> globalCode))).map { x => x > 0 }
  }

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {

    val cursor = electropherograms
      .find(Json.obj("profileId" -> globalCode))
      .projection(Json.obj("_id" -> true, "analysisId" -> true, "name" -> true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        (x.getAs[BSONObjectID]("_id").get.stringify,
          x.getAs[String]("analysisId").get,
          x.getAs[String]("name").getOrElse(this.getDateFromTime(x.getAs[BSONObjectID]("_id").get.time)).toString
        )
      })
    }
  }
  private def getDateFromTime(time: Long):String = {
    var date = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime
    return date.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
  }

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]] = {

    val cursor = electropherograms
      .find(Json.obj("profileId" -> globalCode))
      .projection(Json.obj("_id" -> true, "analysisId" -> true, "name" -> true,  "electropherogram" -> true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        connections.FileInterconnection(
          x.getAs[BSONObjectID]("_id").get.stringify,
          globalCode.text,
          x.getAs[String]("analysisId").get,
          x.getAs[String]("name"),
          "ELECTROPHEROGRAM",
          Base64.encodeBase64String(x.getAs[BSONBinary]("electropherogram").get.byteArray)
        )
      })
    }
  }

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]] = {
    val cursor = files
      .find(Json.obj("profileId" -> globalCode))
      .projection(Json.obj("_id" -> true, "analysisId" -> true,"name"->true,"content"->true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        connections.FileInterconnection(
          x.getAs[BSONObjectID]("_id").get.stringify,
          globalCode.text,
          x.getAs[String]("analysisId").get,
          x.getAs[String]("name"),
          "FILE",
          Base64.encodeBase64String(x.getAs[BSONBinary]("content").get.byteArray)
        )
      })
    }
  }
  override def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = {
    val opt = electropherograms
      .find(Json.obj("_id" -> BSONObjectID(electropherogramId), "profileId" -> profileId))
      .one[BSONDocument]

    val future = opt map (option =>
      option.map(doc => {
        val bytes = doc.getAs[BSONBinary]("electropherogram").get
        bytes.value.readArray(bytes.value.readable)
      }))
    future
  }

  override def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = {
    val cursor = electropherograms
      .find(Json.obj("analysisId" -> analysisId, "profileId" -> profileId))
      .projection(Json.obj("_id" -> true,"name" -> true))
      .cursor[BSONDocument]()
    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => FileUploadedType(x.getAs[BSONObjectID]("_id").get.stringify,
        x.getAs[String]("name").getOrElse(this.getDateFromTime(x.getAs[BSONObjectID]("_id").get.time))
      ))
    }
  }

override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = ???
//
//    val cursor = profiles
//      .find(Json.obj("globalCode" -> globalCode))
//      .cursor[Profile]()
//
//    cursor.headOption.map { opt => opt.map(p => p.genotypification) }
//  }

  def addFile(globalCode: SampleCode, analysisId: String, image: Array[Byte],name:String): Future[Either[String, SampleCode]] = {
    val array = BSONBinary(image, Subtype.GenericBinarySubtype)
    val imageToStore =
      BSONDocument("profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "content" -> array,
        "name"->name)

    val result = files.insert(imageToStore)
    result.map { result => Right(globalCode) }
      .recover { case error => Left(error.getMessage) }
  }

  def addFileWithId(globalCode: SampleCode, analysisId: String, image: Array[Byte],name:String,id:String): Future[Either[String, SampleCode]] = {
    val array = BSONBinary(image, Subtype.GenericBinarySubtype)
    val imageToStore =
      BSONDocument("_id" -> BSONObjectID(id),
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "content" -> array,
        "name"->name)

    val result = files.insert(imageToStore)
    result.map { result => Right(globalCode) }
      .recover { case error => Left(error.getMessage) }
  }

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {

    val cursor = files
      .find(Json.obj("profileId" -> globalCode))
      .projection(Json.obj("_id" -> true, "analysisId" -> true,"name"->true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        (x.getAs[BSONObjectID]("_id").get.stringify, x.getAs[String]("analysisId").get,x.getAs[String]("name").get)
      })
    }
  }

  override def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = {
    val opt = files
      .find(Json.obj("_id" -> BSONObjectID(electropherogramId), "profileId" -> profileId))
      .one[BSONDocument]

    val future = opt map (option =>
      option.map(doc => {
        val bytes = doc.getAs[BSONBinary]("content").get
        bytes.value.readArray(bytes.value.readable)
      }))
    future
  }

  override def getFileByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = {
    val cursor = files
      .find(Json.obj("analysisId" -> analysisId, "profileId" -> profileId))
      .projection(Json.obj("_id" -> true,"name"->true))
      .cursor[BSONDocument]()
    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => FileUploadedType(x.getAs[BSONObjectID]("_id").get.stringify,x.getAs[String]("name").get))
    }
  }

  override def findByCodes(globalCodes: Seq[SampleCode]): Future[Seq[Profile]] = {
    profiles
      .find(Json.obj("_id" -> Json.obj("$in" -> globalCodes)))
      .cursor[Profile]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Profile]]())
  }

  override def saveLabels(globalCode: SampleCode, labeledGenotypification: Profile.LabeledGenotypification): Future[SampleCode] = {
    profiles.update(Json.obj("_id" -> globalCode),
      Json.obj("$set" -> Json.obj("labeledGenotypification" -> labeledGenotypification, "matcheable" -> false, "processed" -> false))).map {
      case result if result.ok => globalCode
      case error => throw new RuntimeException(error.errmsg.get)
    }
  }

  override def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]] = {
    val opt = profiles
      .find(Json.obj("_id" -> globalCode.text))
      .projection(Json.obj("_id" -> false, "labeledGenotypification" -> true))
      .one[BSONDocument]

    opt.map(docOpt =>
      docOpt.flatMap(doc => {
        val js = Json.toJson(doc)
        val jsonValue = js.\("labeledGenotypification")
        Json.fromJson[Map[Profile.MixLabel, Profile.Genotypification]](jsonValue).asOpt
      }))
  }

  override def updateAssocTo(globalCode: SampleCode, to: SampleCode): Future[(String, String, SampleCode)] = {
    val query = BSONDocument("_id" -> globalCode.text)
    val update = Update(BSONDocument("$push" -> BSONDocument("associatedTo" -> to.text)), false)
    val command = FindAndModify(profiles.name, query, update)

    ReactiveMongoPlugin.db.command(command).map { posibleDoc =>
      val doc = posibleDoc.get
      val assignee = doc.getAs[String]("assignee").get
      val internalSampleCode = doc.getAs[String]("internalSampleCode").get
      val globalCode = SampleCode(doc.getAs[String]("globalCode").get)
      (assignee, internalSampleCode, globalCode)
    }

  }

  override def canDeleteKit(id: String): Future[Boolean] = {
    profiles
      .find(Json.obj("analyses.kit" -> id))
      .one[Profile]
      .map(_.isEmpty)
  }
  override def getFullElectropherogramsById(id: String): Future[List[connections.FileInterconnection]] = {
    val cursor = electropherograms
      .find(Json.obj("_id" -> Json.obj("$oid" -> id)))
      .projection(Json.obj("profileId" -> true, "analysisId" -> true, "electropherogram" -> true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        connections.FileInterconnection(
          id,
          x.getAs[String]("profileId").get,
          x.getAs[String]("analysisId").get,
          None,
          "ELECTROPHEROGRAM",
          Base64.encodeBase64String(x.getAs[BSONBinary]("electropherogram").get.byteArray)
        )
      })
    }
  }
  override def getFullFilesById(id: String): Future[List[connections.FileInterconnection]] = {
    val cursor = files
      .find(Json.obj("_id" -> Json.obj("$oid" -> id)))
      .projection(Json.obj("profileId" -> true, "analysisId" -> true,"name"->true,"content"->true))
      .cursor[BSONDocument]()

    cursor.collect[List](Int.MaxValue, Cursor.FailOnError[List[BSONDocument]]()) map { list =>
      list map (x => {
        connections.FileInterconnection(
          id,
          x.getAs[String]("profileId").get,
          x.getAs[String]("analysisId").get,
          x.getAs[String]("name"),
          "FILE",
          Base64.encodeBase64String(x.getAs[BSONBinary]("content").get.byteArray)
        )
      })
    }
  }

  override def getProfilesMarkers(profiles: Array[Profile]) :List[String] = {
    profiles.flatMap(x => {
      x.genotypification.get(1).map(result => {
        result.keySet.map(_.toString)
      }).getOrElse(Nil).toList
    }).toSet.toList
  }

  override def getBy(user: String,isSuperUser: Boolean,internalSampleCode: Option[String] = None,categoryId: Option[String] = None,
                     laboratory: Option[String] = None,
                     hourFrom: Option[Date] = None,
                     hourUntil: Option[Date] = None): Future[List[Profile]] = {
    var query = Json.obj()
    if(internalSampleCode.isDefined && internalSampleCode.get!=""){
      query = query.+("internalSampleCode", JsString(internalSampleCode.get))
    }
    if(categoryId.isDefined && categoryId.get!=""){
      query = query.+("categoryId", JsString(categoryId.get))
    }
    val dateFilter = (hourFrom,hourUntil) match {
      case (Some(hf),Some(hu)) => Some(Json.obj("$gte" ->MongoDate(hf),"$lte" ->MongoDate(hu)))
      case (Some(hf),None) => Some(Json.obj("$gte" ->MongoDate(hf)))
      case (None,Some(hu)) => Some(Json.obj("$lte" ->MongoDate(hu)))
      case (None,None) => None
    }
    if(dateFilter.isDefined){
      query = query.+("analyses", Json.obj("$elemMatch"->Json.obj("date"->dateFilter.get)))
    }
    if(!isSuperUser){
      query = query.+("assignee", JsString(user))
    }
    query = query.+("deleted", JsBoolean(false))

    val result = profiles
      .find(query)
      .cursor[Profile]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Profile]]())
    result.map(listProfiles => {
      if(laboratory.isDefined && laboratory.get!=""){
        listProfiles.filter(_.globalCode.text.contains(s"-${laboratory.get}-"))
      }else{
        listProfiles
      }
    })
  }

  override def getBetweenDates(hourFrom: Option[Date] = None, hourUntil: Option[Date] = None): Future[List[Profile]] = {
    var query = Json.obj()
    //ver que la fecha menor de los analisis este entre las fechas que le paso
    val dateFilter = (hourFrom,hourUntil) match {
      case (Some(hf),Some(hu)) => Some(Json.obj("$gte" ->MongoDate(hf),"$lte" ->MongoDate(hu)))
      case (Some(hf),None) => Some(Json.obj("$gte" ->MongoDate(hf)))
      case (None,Some(hu)) => Some(Json.obj("$lte" ->MongoDate(hu)))
      case (None,None) => None
    }
    if(dateFilter.isDefined){
      query = query.+("analyses", Json.obj("$elemMatch"->Json.obj("date"->dateFilter.get)))
    }

    query = query.+("deleted", JsBoolean(false))

    val result = profiles
      .find(query)
      .cursor[Profile]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Profile]]())
    /*
        result.map(listProfiles => {
          if(laboratory.isDefined && laboratory.get!=""){
            listProfiles.filter(_.globalCode.text.contains(s"-${laboratory.get}-"))
          }else{
            listProfiles
          }
        })
    */
    result
  }

  def removeFile(id: String):Future[Either[String,String]] = Future{
    val query = Json.obj("_id" -> BSONObjectID(id))
    files.findAndRemove(query)
    Right(id)
  }

  def removeEpg(id: String):Future[Either[String,String]] = Future{
    val query = Json.obj("_id" -> BSONObjectID(id))
    electropherograms.findAndRemove(query)
    Right(id)
  }

  def getProfileOwnerByFileId(id: String): Future[(String,SampleCode)] = {
    this.getProfileOwnerByEpgOrFileId(id,files);
  }
  def getProfileOwnerByEpgId(id: String): Future[(String,SampleCode)] = {
    this.getProfileOwnerByEpgOrFileId(id,electropherograms);
  }
  def getProfileOwnerByEpgOrFileId(id: String,collection: JSONCollection): Future[(String,SampleCode)] = {
    ((collection.find( Json.obj("_id" -> BSONObjectID(id))).one[BSONDocument]) flatMap (optDoc =>{
      FutureUtils.swap(optDoc.map(doc => this.get(SampleCode(doc.getAs[String]("profileId").get))
        .map(res => res.map( x => (x.assignee, x.globalCode) )))).map(_.flatten)
    })).map( x => x.getOrElse(("",SampleCode(""))))
  }

  override def getAllProfiles() : Future[List[(SampleCode, String)]]= {
    profiles
      .find(Json.obj())
      .cursor[Profile]()
      .collect[List](-1, Cursor.FailOnError[List[Profile]]())
      .map(
        ps => ps.map(
          p => (p.globalCode, p.categoryId.text)
        )
      )
  }
}


@Singleton
class MiddleProfileRepository @Inject () (
                                           mongoRepo: MongoProfileRepository,
                                           couchRepo: CouchProfileRepository
                                         ) extends ProfileRepository {

  override def get(id: SampleCode): Future[Option[Profile]] = {
    {
      for {
        r1 <- mongoRepo.get(id)
        r2 <- couchRepo.get(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("get - Mongo encuentra: " + r1)
          println("get - Couch encuentra: " + r2)
        } else {
          println("get - Iguales")
        }
        r1
      }
    }
  }
  override def getBy(
                      user: String,
                      isSuperUser: Boolean,
                      internalSampleCode: Option[String],
                      categoryId: Option[String],
                      laboratory: Option[String],
                      hourFrom: Option[Date],
                      hourUntil: Option[Date]
                    ): Future[List[Profile]] = {
      {
        for {
          r1 <- mongoRepo.getBy(
            user,
            isSuperUser,
            internalSampleCode,
            categoryId,
            laboratory,
            hourFrom,
            hourUntil
          )
          r2 <- couchRepo.getBy(
            user,
            isSuperUser,
            internalSampleCode,
            categoryId,
            laboratory,
            hourFrom,
            hourUntil
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("getBy - Mongo encuentra: " + r1)
            println("getBy - Couch encuentra: " + r2)
          } else {
            println("getBy - Iguales")
          }
          r1
        }
      }
    }

  override def getBetweenDates (
                                 hourFrom: Option[Date],
                                 hourUntil: Option[Date]
                               ): Future[List[Profile]] = {
      {
        for {
          r1 <- mongoRepo.getBetweenDates(
              hourFrom,
              hourUntil
            )
          r2 <- couchRepo.getBetweenDates(
            hourFrom,
            hourUntil
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("getBetweenDates - Mongo encuentra: " + r1)
            println("getBetweenDates - Couch encuentra: " + r2)
          } else {
            println("getBetweenDates - Iguales")
          }
          r1
        }
      }
    }
  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {
    {
      for {
        r1 <- mongoRepo.findByCode(globalCode)
        r2 <- couchRepo.findByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("findByCode - Mongo encuentra: " + r1)
          println("findByCode - Couch encuentra: " + r2)
        } else {
          println("findByCode - Iguales")
        }
        r1
      }
    }
  }

  override def add(profile: Profile): Future[SampleCode] = {
      {
        for {
          r1 <- mongoRepo.add(profile)
          r2 <- couchRepo.add(profile)
        }
        yield {
          if (!r1.equals(r2)) {
            println("add - Mongo encuentra: " + r1)
            println("add - Couch encuentra: " + r2)
          } else {
            println("add - Iguales")
          }
          r1
        }
      }
    }

  override def addElectropherogram(
                                    globalCode: SampleCode,
                                    analysisId: String,
                                    image: Array[Byte],
                                    name: String
                                  ): Future[Either[String, SampleCode]] = {
      {
        for {
          r1 <- mongoRepo.addElectropherogram(
            globalCode,
            analysisId,
            image,
            name
          )
          r2 <- couchRepo.addElectropherogram(
            globalCode,
            analysisId,
            image,
            name
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("addElectropherogram - Mongo encuentra: " + r1)
            println("addElectropherogram - Couch encuentra: " + r2)
          } else {
            println("addElectropherogram - Iguales")
          }
          r1
        }
      }
    }

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {
    {
      for {
        r1 <- mongoRepo.getElectropherogramsByCode(globalCode)
        r2 <- couchRepo.getElectropherogramsByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getElectropherogramsByCode - Mongo encuentra: " + r1)
          println("getElectropherogramsByCode - Couch encuentra: " + r2)
        } else {
          println("getElectropherogramsByCode - Iguales")
        }
        r1
      }
    }
  }


  override def getElectropherogramImage(
                                         profileId: SampleCode,
                                         electropherogramId: String
                                       ): Future[Option[Array[Byte]]] = {
    {
      for {
        r1 <- mongoRepo.getElectropherogramImage(profileId, electropherogramId)
        r2 <- couchRepo.getElectropherogramImage(profileId, electropherogramId)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getElectropherogramImage - Mongo encuentra: " + r1)
          println("getElectropherogramImage - Couch encuentra: " + r2)
        } else {
          println("getElectropherogramImage - Iguales")
        }
        r1
      }
    }
  }

  override def getElectropherogramsByAnalysisId(
                                                 profileId: SampleCode,
                                                 analysisId: String
                                               ): Future[List[FileUploadedType]] = {
    {
      for {
        r1 <- mongoRepo.getElectropherogramsByAnalysisId(profileId, analysisId)
        r2 <- couchRepo.getElectropherogramsByAnalysisId(profileId, analysisId)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getElectropherogramsByAnalysisId - Mongo encuentra: " + r1)
          println("getElectropherogramsByAnalysisId - Couch encuentra: " + r2)
        } else {
          println("getElectropherogramsByAnalysisId - Iguales")
        }
        r1
      }
    }
  }

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = {
    {
      for {
        r1 <- mongoRepo.getFullElectropherogramsByCode(globalCode)
        r2 <- couchRepo.getFullElectropherogramsByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFullElectropherogramsByCode - Mongo encuentra: " + r1)
          println("getFullElectropherogramsByCode - Couch encuentra: " + r2)
        } else {
          println("getFullElectropherogramsByCode - Iguales")
        }
        r1
      }
    }
  }

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = {
    {
      for {
        r1 <- mongoRepo.getFullFilesByCode(globalCode)
        r2 <- couchRepo.getFullFilesByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFullFilesByCode - Mongo encuentra: " + r1)
          println("getFullFilesByCode - Couch encuentra: " + r2)
        } else {
          println("getFullFilesByCode - Iguales")
        }
        r1
      }
    }
  }

  override def addElectropherogramWithId(
                                          globalCode: SampleCode,
                                          analysisId: String,
                                          image: Array[Byte],
                                          name: String,
                                          id: String
                                        ): Future[Either[String, SampleCode]] = {
      {
        for {
          r1 <- mongoRepo.addElectropherogramWithId(
            globalCode,
            analysisId,
            image,
            name,
            id
          )
          r2 <- couchRepo.addElectropherogramWithId(
            globalCode,
            analysisId,
            image,
            name,
            id
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("addElectropherogramWithId - Mongo encuentra: " + r1)
            println("addElectropherogramWithId - Couch encuentra: " + r2)
          } else {
            println("addElectropherogramWithId - Iguales")
          }
          r1
        }
      }
    }
  override def addFileWithId(
                              globalCode: SampleCode,
                              analysisId: String,
                              image: Array[Byte],
                              name: String,
                              id: String
                            ): Future[Either[String, SampleCode]] = {
      {
        for {
          r1 <- mongoRepo.addFileWithId(
            globalCode,
            analysisId,
            image,
            name,
            id
          )
          r2 <- couchRepo.addFileWithId(
            globalCode,
            analysisId,
            image,
            name,
            id
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("addFileWithId - Mongo encuentra: " + r1)
            println("addFileWithId - Couch encuentra: " + r2)
          } else {
            println("addFileWithId - Iguales")
          }
          r1
        }
      }
    }

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = ???
//  Out of use
  //  {
//    for {
//      r1 <- mongoRepo.getGenotyficationByCode(globalCode)
//      r2 <- couchRepo.getGenotyficationByCode(globalCode)
//    }
//    yield {
//      if (!r1.equals(r2)) {
//        println("Mongo: " + r1)
//        println("Couch: " + r2)
//      } else {
//        println("***Iguales***")
//      }
//      r1
//    }
//  }


  override def findByCodes(
                            globalCodes: Seq[SampleCode]
                          ): Future[Seq[Profile]] = {
    {
      for {
        r1 <- mongoRepo.findByCodes(globalCodes)
        r2 <- couchRepo.findByCodes(globalCodes)
      }
      yield {
        if (!r1.equals(r2)) {
          println("findByCodes - Mongo: " + r1)
          println("findByCodes - Couch: " + r2)
        } else {
          println("findByCodes - Iguales")
        }
        r1
      }
    }
  }

  override def addAnalysis(
                            _id: SampleCode,
                            analysis: Analysis,
                            genotypification: GenotypificationByType,
                            labeledGenotypification: Option[LabeledGenotypification],
                            matchingRules: Option[Seq[MatchingRule]],
                            mismatches: Option[Mismatch]
                          ): Future[SampleCode] = {
      {
        for {
          r1 <- mongoRepo.addAnalysis(
            _id,
            analysis,
            genotypification,
            labeledGenotypification,
            matchingRules,
            mismatches
          )
          r2 <- couchRepo.addAnalysis(
            _id,
            analysis,
            genotypification,
            labeledGenotypification,
            matchingRules,
            mismatches
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("addAnalysis - Mongo: " + r1)
            println("addAnalysis - Couch: " + r2)
          } else {
            println("addAnalysis - Iguales")
          }
          r1
        }
      }
    }

  override def saveLabels(
                           globalCode: SampleCode,
                           labels: LabeledGenotypification
                         ): Future[SampleCode] = {
      {
        for {
          r1 <-  mongoRepo.saveLabels(
            globalCode,
            labels
          )
          r2 <- couchRepo.saveLabels(
            globalCode,
            labels
          )
        }
        yield {
          if (!r1.equals(r2)) {
            println("saveLabels - Mongo: " + r1)
            println("saveLabels - Couch: " + r2)
          } else {
            println("saveLabels - Iguales")
          }
          r1
        }
      }
    }


  override def existProfile(globalCode: SampleCode): Future[Boolean] = {
    {
      for {
        r1 <- mongoRepo.existProfile(globalCode)
        r2 <- couchRepo.existProfile(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("existProfile - Mongo: " + r1)
          println("existProfile - Couch: " + r2)
        } else {
          println("existProfile - Iguales")
        }
        r1
      }
    }
  }

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = {
      for {
        r1 <- mongoRepo.delete(globalCode)
        r2 <- couchRepo.delete(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("delete - Mongo: " + r1)
          println("delete - Couch: " + r2)
        } else {
          println("delete - Iguales")
        }
        r1
      }
    }


  override def getLabels(globalCode: SampleCode): Future[Option[LabeledGenotypification]] = {
    {
      for {
        r1 <- mongoRepo.getLabels(globalCode)
        r2 <- couchRepo.getLabels(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getLabels - Mongo: " + r1)
          println("getLabels - Couch: " + r2)
        } else {
          println("getLabels - Iguales")
        }
        r1
      }
    }
  }


  override def updateAssocTo(
                              globalCode: SampleCode,
                              to: SampleCode
                            ): Future[(String, String, SampleCode)] =
    mongoRepo.updateAssocTo(
      globalCode,
      to
    )

  override def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    mongoRepo.setMatcheableAndProcessed(globalCode)

  override def getUnprocessed(): Future[Seq[SampleCode]] = {
    {
      for {
        r1 <- mongoRepo.getUnprocessed()
        r2 <- couchRepo.getUnprocessed()
      }
      yield {
        if (!r1.equals(r2)) {
          println("Mongo: " + r1)
          println("Couch: " + r2)
        } else {
          println("Iguales")
        }
        r1
      }
    }
  }

  override def canDeleteKit(id: String): Future[Boolean] = {
    {
      for {
        r1 <- mongoRepo.canDeleteKit(id)
        r2 <- couchRepo.canDeleteKit(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("canDeleteKit - Mongo: " + r1)
          println("canDeleteKit - Couch: " + r2)
        } else {
          println("canDeleteKit - Iguales")
        }
        r1
      }
    }
  }
  override def updateProfile(profile: Profile): Future[SampleCode] = mongoRepo.updateProfile(profile: Profile)

  override def findByCodeWithoutAceptedLocus(
                                              globalCode: SampleCode,
                                              aceptedLocus: Seq[String]
                                            ): Future[Option[Profile]] = {
    {
      for {
        r1 <- mongoRepo.findByCodeWithoutAceptedLocus(
          globalCode,
          aceptedLocus
        )
        r2 <- couchRepo.findByCodeWithoutAceptedLocus(
          globalCode,
          aceptedLocus
        )
      }
      yield {
        if (!r1.equals(r2)) {
          println("findByCodeWithoutAceptedLocus - Mongo: " + r1)
          println("findByCodeWithoutAceptedLocus - Couch: " + r2)
        } else {
          println("findByCodeWithoutAceptedLocus - Iguales")
        }
        r1
      }
    }
  }

  override def addFile(
                        globalCode: SampleCode,
                        analysisId: String,
                        image: Array[Byte],
                        name: String
                      ): Future[Either[String, SampleCode]] = mongoRepo.addFile(
    globalCode,
    analysisId,
    image,
    name
  )

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {
    {
      for {
        r1 <- mongoRepo.getFileByCode(globalCode)
        r2 <- couchRepo.getFileByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFileByCode - Mongo: " + r1)
          println("getFileByCode - Couch: " + r2)
        } else {
          println("getFileByCode - Iguales")
        }
        r1
      }
    }
  }

  override def getFile(
                        profileId: SampleCode,
                        electropherogramId: String
                      ): Future[Option[Array[Byte]]] = {
    {
      for {
        r1 <-  mongoRepo.getFile(
          profileId,
          electropherogramId
        )
        r2 <- couchRepo.getFile(
          profileId,
          electropherogramId
        )
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFile - Mongo: " + r1)
          println("getFile - Couch: " + r2)
        } else {
          println("getFile - Iguales")
        }
        r1
      }
    }
  }

  override def getFileByAnalysisId(
                                    profileId: SampleCode,
                                    analysisId: String
                                  ): Future[List[FileUploadedType]] = {
    {
      for {
        r1 <-  mongoRepo.getFileByAnalysisId(
          profileId,
          analysisId
        )

        r2 <- couchRepo.getFileByAnalysisId(
          profileId,
          analysisId
        )
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFileByAnalysisId - Mongo: " + r1)
          println("getFileByAnalysisId - Couch: " + r2)
        } else {
          println("getFileByAnalysisId - Iguales")
        }
        r1
      }
    }
  }

  override def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]] = {
    {
      for {
        r1 <-  mongoRepo.getFullElectropherogramsById(id)

        r2 <- couchRepo.getFullElectropherogramsById(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFullElectropherogramsById - Mongo: " + r1)
          println("getFullElectropherogramsById - Couch: " + r2)
        } else {
          println("getFullElectropherogramsById - Iguales")
        }
        r1
      }
    }
  }

  override def getFullFilesById(id: String): Future[List[FileInterconnection]] = {
    {
      for {
        r1 <-  mongoRepo.getFullFilesById(id)

        r2 <- couchRepo.getFullFilesById(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getFullFilesById - Mongo: " + r1)
          println("getFullFilesById - Couch: " + r2)
        } else {
          println("getFullFilesById - Iguales")
        }
        r1
      }
    }
  }
  override def getProfilesMarkers(profiles: Array[Profile]): List[String] = {
    {
      for {
        r1 <-  mongoRepo.getProfilesMarkers(profiles)

        r2 <- couchRepo.getProfilesMarkers(profiles)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getProfilesMarkers - Mongo: " + r1)
          println("getProfilesMarkers - Couch: " + r2)
        } else {
          println("getProfilesMarkers - Iguales")
        }
        r1
      }
    }
  }

  override def removeFile(id: String): Future[Either[String, String]] = mongoRepo.removeFile(id)

  override def removeEpg(id: String): Future[Either[String, String]] = mongoRepo.removeEpg(id)

  override def getProfileOwnerByFileId(id: String): Future[(String, SampleCode)] = {
    {
      for {
        r1 <-  mongoRepo.getProfileOwnerByFileId(id)

        r2 <- couchRepo.getProfileOwnerByFileId(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getProfileOwnerByFileId - Mongo: " + r1)
          println("getProfileOwnerByFileId - Couch: " + r2)
        } else {
          println("getProfileOwnerByFileId - Iguales")
        }
        r1
      }
    }
  }
  override def getProfileOwnerByEpgId(id: String): Future[(String, SampleCode)] = {
    {
      for {
        r1 <-  mongoRepo.getProfileOwnerByEpgId(id)

        r2 <- couchRepo.getProfileOwnerByEpgId(id)
      }
      yield {
        if (!r1.equals(r2)) {
          println("getProfileOwnerByEpgId - Mongo: " + r1)
          println("getProfileOwnerByEpgId - Couch: " + r2)
        } else {
          println("getProfileOwnerByEpgId - Iguales")
        }
        r1
      }
    }
  }
  override def getAllProfiles(): Future[List[(SampleCode, String)]] = {
    {
      for {
        r1 <-  mongoRepo.getAllProfiles()

        r2 <- couchRepo.getAllProfiles()
      }
      yield {
        if (!r1.equals(r2)) {
          println("getAllProfiles - Mongo: " + r1)
          println("getAllProfiles - Couch: " + r2)
        } else {
          println("getAllProfiles - Iguales")
        }
        r1
      }
    }
  }
}

class CouchProfileRepository extends ProfileRepository {
  private val backend = HttpURLConnectionBackend()
  private val profilesUrl = "http://localhost:5984/profiles"
  private val filesUrl = "http://localhost:5984/files"
  private val electropherogramsUrl = "http://localhost:5984/electropherograms"
  val username = "admin"
  val password = "genisContra" // des-hardcodear esto

  override def get(id: SampleCode): Future[Option[Profile]] = {
    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Map("selector" -> Map("_id" -> id.text)))
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(profiles) =>
          val json = Json.parse(profiles)
          (json \ "docs").validate[List[Profile]] match {
            case JsSuccess(profileList, _) => profileList.headOption
            case JsError(errors) =>
              //println(s"Error de parseo a JSON: $errors")
              None
          }
        case Left(_) => None
      }
    }

  }

  override def getBy(
                      user: String,
                      isSuperUser: Boolean,
                      internalSampleCode: Option[String],
                      categoryId: Option[String],
                      laboratory: Option[String],
                      hourFrom: Option[Date],
                      hourUntil: Option[Date]
                    ): Future[List[Profile]] = {
    var selector = Json.obj("deleted" -> false)
    if (internalSampleCode.isDefined && internalSampleCode.get.nonEmpty) {
      selector = selector.+("internalSampleCode", JsString(internalSampleCode.get))
    }
    if (categoryId.isDefined && categoryId.get.nonEmpty) {
      selector = selector.+("categoryId", JsString(categoryId.get))
    }
    val dateFilter = (hourFrom, hourUntil) match {
      case (Some(hf), Some(hu)) => Some(Json.obj("$gte" -> MongoDate(hf), "$lte" -> MongoDate(hu)))
      case (Some(hf), None) => Some(Json.obj("$gte" -> MongoDate(hf)))
      case (None, Some(hu)) => Some(Json.obj("$lte" -> MongoDate(hu)))
      case (None, None) => None
    }
    if (dateFilter.isDefined) {
      selector = selector.+("analyses", Json.obj("$elemMatch" -> Json.obj("date" -> dateFilter.get)))
    }
    if (!isSuperUser) {
      selector = selector.+("assignee", JsString(user))
    }

    val query = Json.obj("selector" -> selector)

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[Profile]] match {
            case JsSuccess(profileList, _) =>
              if (laboratory.isDefined && laboratory.get.nonEmpty) {
                profileList.filter(_.globalCode.text.contains(s"-${laboratory.get}-"))
              } else {
                profileList
              }
            case JsError(_) => List.empty
          }
        case Left(_) => List.empty
      }
    }
  }

  override def getBetweenDates(
                                hourFrom: Option[Date],
                                hourUntil: Option[Date]
                              ): Future[List[Profile]] = {
    var selector = Json.obj("deleted" -> false)
    val dateFilter = (hourFrom, hourUntil) match {
      case (Some(hf), Some(hu)) => Some(Json.obj("$gte" -> MongoDate(hf), "$lte" -> MongoDate(hu)))
      case (Some(hf), None) => Some(Json.obj("$gte" -> MongoDate(hf)))
      case (None, Some(hu)) => Some(Json.obj("$lte" -> MongoDate(hu)))
      case (None, None) => None
    }
    if (dateFilter.isDefined) {
      selector = selector.+("analyses", Json.obj("$elemMatch" -> Json.obj("date" -> dateFilter.get)))
    }

    val query = Json.obj("selector" -> selector)

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[Profile]] match {
            case JsSuccess(profileList, _) => profileList
            case JsError(_) => List.empty
          }
        case Left(_) => List.empty
      }
    }
  }

  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Map("selector" -> Map("globalCode" -> globalCode.text)))
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(profiles) =>
          val json = Json.parse(profiles)
          (json \ "docs").validate[List[Profile]] match {
            case JsSuccess(profileList, _) => profileList.headOption
            case JsError(errors) =>
              //println(s"Error de parseo a JSON: $errors")
              None
          }
        case Left(_) => None
      }
    }
  }
  override def add(profile: Profile): Future[SampleCode] = {
    val request = basicRequest
      .put(uri"$profilesUrl/${profile._id.text}")
      .body(Json.stringify(Json.toJson(profile)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => profile._id
        case Left(error) => throw new RuntimeException(s"Error adding profile to CouchDB: $error")
      }
    }
  }

  override def addElectropherogram(
                                    globalCode: SampleCode,
                                    analysisId: String,
                                    image: Array[Byte],
                                    name: String
                                  ): Future[Either[String, SampleCode]] = {
    val electropherogramData = if (name != null) {
      Json.obj(
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> Base64.encodeBase64String(image),
        "name" -> name
      )
    } else {
      Json.obj(
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> Base64.encodeBase64String(image)
      )
    }

    val request = basicRequest
      .post(uri"$electropherogramsUrl")
      .body(Json.stringify(electropherogramData))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => Right(globalCode)
        case Left(error) => Left(s"Error adding electropherogram to CouchDB: $error")
      }
    }
  }

override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("profileId" -> globalCode.text),
      "fields" -> Json.arr("_id", "analysisId", "name") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                val id = (doc \ "_id").asOpt[String].getOrElse("")
                val analysisId = (doc \ "analysisId").asOpt[String].getOrElse("")
                val name = (doc \ "name").asOpt[String].getOrElse {
                  // Fallback to the timestamp from `_id` if `name` is missing
                  this.getDateFromTime(id.takeWhile(_.isDigit).toLong).toString
                }
                (id, analysisId, name)
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP errors
      }
    }
  }

  private def getDateFromTime(time: Long):String = {
    var date = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime
    return date.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
  }

  override def getElectropherogramImage(
                                         profileId: SampleCode,
                                         electropherogramId: String
                                       ): Future[Option[Array[Byte]]] =  {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj(
        "_id" -> electropherogramId,
        "profileId" -> profileId.text
      ),
      "fields" -> Json.arr("electropherogram") // Specify the field to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.headOption.flatMap { doc =>
                (doc \ "electropherogram").asOpt[String].map { base64Encoded =>
                  Base64.decodeBase64(base64Encoded) // Decode Base64 content
                }
              }
            case JsError(_) => None // Handle JSON parsing errors
          }
        case Left(_) => None // Handle HTTP request errors
      }
    }
  }
  override def getElectropherogramsByAnalysisId(
                                                 profileId: SampleCode,
                                                 analysisId: String
                                               ): Future[List[FileUploadedType]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj(
        "analysisId" -> analysisId,
        "profileId" -> profileId.text
      ),
      "fields" -> Json.arr("_id", "name") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                FileUploadedType(
                  (doc \ "_id").asOpt[String].getOrElse(""),
                  (doc \ "name").asOpt[String].getOrElse {
                    val id = (doc \ "_id").asOpt[String].getOrElse("")
                    getDateFromTime(id.takeWhile(_.isDigit).toLong).toString
                  }
                )
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP request errors
      }
    }
  }
  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[connections.FileInterconnection]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("profileId" -> globalCode.text),
      "fields" -> Json.arr("_id", "analysisId", "name", "electropherogram") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                connections.FileInterconnection(
                  (doc \ "_id").asOpt[String].getOrElse(""),
                  globalCode.text,
                  (doc \ "analysisId").asOpt[String].getOrElse(""),
                  (doc \ "name").asOpt[String],
                  "ELECTROPHEROGRAM",
                  (doc \ "electropherogram").asOpt[String].map { base64Encoded =>
                    Base64.encodeBase64String(base64Encoded.getBytes("UTF-8")) // Encode electropherogram
                  }.getOrElse("")
                )
              }
            case JsError(_) =>
              List.empty // Handle JSON parsing errors
          }
        case Left(_) =>
          List.empty // Handle HTTP request errors
      }
    }
  }

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("profileId" -> globalCode.text),
      "fields" -> Json.arr("_id", "analysisId", "name", "content") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                connections.FileInterconnection(
                  (doc \ "_id").asOpt[String].getOrElse(""),
                  globalCode.text,
                  (doc \ "analysisId").asOpt[String].getOrElse(""),
                  (doc \ "name").asOpt[String],
                  "FILE",
                  (doc \ "content").asOpt[String].map { base64Encoded =>
                    Base64.encodeBase64String(base64Encoded.getBytes("UTF-8")) // Encode content
                  }.getOrElse("")
                )
              }
            case JsError(_) =>
              List.empty // Handle JSON parsing errors
          }
        case Left(_) =>
          List.empty // Handle HTTP request errors
      }
    }
  }


  override def addElectropherogramWithId(
                                          globalCode: SampleCode,
                                          analysisId: String,
                                          image: Array[Byte],
                                          name: String,
                                          id: String
                                        ): Future[Either[String, SampleCode]] = {
    val electropherogramData = if (name != null) {
      Json.obj(
        "_id" -> id,
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> Base64.encodeBase64String(image),
        "name" -> name
      )
    } else {
      Json.obj(
        "_id" -> id,
        "profileId" -> globalCode.text,
        "analysisId" -> analysisId,
        "electropherogram" -> Base64.encodeBase64String(image)
      )
    }

    val request = basicRequest
      .put(uri"$electropherogramsUrl/$id")
      .body(Json.stringify(electropherogramData))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => Right(globalCode)
        case Left(error) => Left(s"Error adding electropherogram to CouchDB: $error")
      }
    }
  }

  override def addFileWithId(
                              globalCode: SampleCode,
                              analysisId: String,
                              image: Array[Byte],
                              name: String,
                              id: String
                            ): Future[Either[String, SampleCode]] = {
    val fileData = Json.obj(
      "_id" -> id,
      "profileId" -> globalCode.text,
      "analysisId" -> analysisId,
      "content" -> Base64.encodeBase64String(image),
      "name" -> name
    )

    val request = basicRequest
      .put(uri"$filesUrl/$id")
      .body(Json.stringify(fileData))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => Right(globalCode)
        case Left(error) => Left(s"Error adding file to CouchDB: $error")
      }
    }
  }

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = ???
// Out of use
//  {
//    val request = basicRequest
//      .post(uri"$baseUrl/_find")
//      .body(Map("selector" -> Map("globalCode" -> globalCode.text)))
//      .header("Accept", "application/json")
//      .auth.basic(username, password)
//
//
//    Future {
//      val response = request.send(backend)
//      response.body match {
//        case Right(profiles) =>
//          val json = Json.parse(profiles)
//          (json \ "docs").validate[List[Profile]] match {
//            case JsSuccess(profileList, _) => profileList.headOption.map(_.genotypification)
//            case JsError(errors) =>
//              //println(s"Error de parseo a JSON: $errors")
//              None
//          }
//        case Left(_) => None
//      }
//    }
//  }
  override def findByCodes(globalCodes: Seq[SampleCode]): Future[Seq[Profile]] = {
    val query = Map(
      "selector" -> Map(
        "_id" -> Map(
          "$in" -> globalCodes.map(_.text)
        )
      )
    )

    val request = basicRequest
      .body(query)
      .post(uri"$profilesUrl/_find")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(profiles) =>
          val json = Json.parse(profiles)
          (json \ "docs").validate[List[Profile]] match {
            case JsSuccess(profileList, _) => profileList
            case JsError(errors) =>
              throw new RuntimeException(s"Error parsing JSON: $errors")
          }
        case Left(error) =>
          throw new RuntimeException(s"Error querying CouchDB: $error")
      }
    }
  }
  override def addAnalysis(
                            _id: SampleCode,
                            analysis: Analysis,
                            genotypification: GenotypificationByType,
                            labeledGenotypification: Option[LabeledGenotypification],
                            matchingRules: Option[Seq[MatchingRule]],
                            mismatches: Option[Mismatch]
                          ): Future[SampleCode] = {
    val updateData = Json.obj(
      "genotypification" -> genotypification,
      "labeledGenotypification" -> labeledGenotypification,
      "matchingRules" -> matchingRules,
      "mismatches" -> mismatches,
      "matcheable" -> false,
      "processed" -> false,
      "analyses" -> Json.arr(analysis)
    )

    val request = basicRequest
      .put(uri"$profilesUrl/${_id.text}")
      .body(Json.stringify(Json.obj("$set" -> updateData)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => _id
        case Left(error) => throw new RuntimeException(s"Error adding analysis to CouchDB: $error")
      }
    }
  }

  override def saveLabels(
                           globalCode: SampleCode,
                           labels: LabeledGenotypification
                         ): Future[SampleCode] = {
    val updateData = Json.obj(
      "labeledGenotypification" -> labels,
      "matcheable" -> false,
      "processed" -> false
    )

    val request = basicRequest
      .put(uri"$profilesUrl/${globalCode.text}")
      .body(Json.stringify(Json.obj("$set" -> updateData)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => globalCode
        case Left(error) => throw new RuntimeException(s"Error saving labels to CouchDB: $error")
      }
    }
  }

  override def existProfile(globalCode: SampleCode): Future[Boolean] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("_id" -> globalCode.text),
      "limit" -> 1
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) => docList.nonEmpty
            case JsError(errors) =>
              throw new RuntimeException(s"Parsing error: $errors")
          }
        case Left(error) =>
          throw new RuntimeException(s"HTTP request error: $error")
      }
    }
  }


  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = {
    val updateData = Json.obj(
      "deleted" -> true
    )

    val request = basicRequest
      .put(uri"$profilesUrl/${globalCode.text}")
      .body(Json.stringify(Json.obj("$set" -> updateData)))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(_) => Right(globalCode)
        case Left(error) => Left(s"Error deleting profile from CouchDB: $error")
      }
    }
  }

  override def getLabels(globalCode: SampleCode): Future[Option[LabeledGenotypification]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("_id" -> globalCode.text),
      "fields" -> Json.arr("labeledGenotypification") // Specify the field to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.headOption.flatMap { doc =>
                (doc \ "labeledGenotypification").asOpt[Profile.LabeledGenotypification]
              }
            case JsError(_) => None // Handle JSON parsing errors
          }
        case Left(_) => None // Handle HTTP request errors
      }
    }
  }

  override def updateAssocTo(
                              globalCode: SampleCode,
                              to: SampleCode
                            ): Future[(String, String, SampleCode)] = ???

  override def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]] = ???

  def fetchUnprocessedBatch(skip: Int, limit: Int = 100): Future[Seq[SampleCode]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("processed" -> false),
      "fields" -> Json.arr("globalCode"),
      "limit" -> limit,
      "skip" -> skip // Skip the first `skip` documents
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.flatMap(doc =>
                (doc \ "globalCode").asOpt[String].map(SampleCode(_))
              )
            case JsError(_) => Seq.empty
          }
        case Left(_) => Seq.empty
      }
    }
  }

  override def getUnprocessed(): Future[Seq[SampleCode]] = {
    val step = 100;
    def loop(skip: Int, acc: Seq[SampleCode]): Future[Seq[SampleCode]] = {
      fetchUnprocessedBatch(skip).flatMap { batch =>
        if (batch.isEmpty) Future.successful(acc) // Stop when no more documents
        else loop(skip + step, acc ++ batch) // Fetch the next batch
      }
    }
    loop(0, Seq.empty)
  }
  override def canDeleteKit(id: String): Future[Boolean] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("analyses.kit" -> id),
      "limit" -> 1 // Limit to 1 document to check existence
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) => docList.isEmpty
            case JsError(_) => false // Handle JSON parsing errors
          }
        case Left(_) => false // Handle HTTP request errors
      }
    }
  }

  override def updateProfile(profile: Profile): Future[SampleCode] = ???

  override def findByCodeWithoutAceptedLocus(
                                              globalCode: SampleCode,
                                              aceptedLocus: Seq[String]
                                            ): Future[Option[Profile]] = {



      val query = Map(
        "selector" -> Map("globalCode" -> globalCode.text)
      ) // Mongo sorted it. It was unnecessary so couchDB doesn't

      // Build the CouchDB `_find` request
      val request = basicRequest
        .body(query)
        .post(uri"$profilesUrl/_find")
        .header("Accept", "application/json")
        .auth.basic(username, password)

      Future {
        val response = request.send(backend)

        response.body match {
          case Right(profiles) =>
            val json = Json.parse(profiles)

            // Parse the list of profiles from the response
            (json \ "docs").validate[List[Profile]] match {
              case JsSuccess(profileList, _) =>
                // Process each profile to exclude unwanted loci from `genotypification.1`
                val processedProfiles = profileList.map { profile =>
                  val updatedGenotypificationByType = profile.genotypification.get(1) match {
                    case Some(genotypification) =>
                      // Filter loci in `genotypification.1`
                      val filteredGenotypification = genotypification.filterNot { case (marker, _) =>
                        aceptedLocus.contains(marker)
                      }
                      // Update only key `1` with filtered data
                      profile.genotypification.updated(1, filteredGenotypification)
                    case None =>
                      // If key `1` is not present, return the original `genotypification`
                      profile.genotypification
                  }
                  // Return updated profile
                  profile.copy(genotypification = updatedGenotypificationByType)
                }
                processedProfiles.headOption

              case JsError(errors) =>
                // Handle JSON parsing errors
                None
            }

          case Left(_) =>
            // Handle HTTP errors
            None
        }
      }
    }

    override def addFile(
                        globalCode: SampleCode,
                        analysisId: String,
                        image: Array[Byte],
                        name: String
                      ): Future[Either[String, SampleCode]] = ???

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("profileId" -> globalCode.text),
      "fields" -> Json.arr("_id", "analysisId", "name") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                (
                  (doc \ "_id").asOpt[String].getOrElse(""),
                  (doc \ "analysisId").asOpt[String].getOrElse(""),
                  (doc \ "name").asOpt[String].getOrElse("")
                )
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP request errors
      }
    }
  }

  override def getFile(
                        profileId: SampleCode,
                        electropherogramId: String
                      ): Future[Option[Array[Byte]]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj(
        "_id" -> electropherogramId,
        "profileId" -> profileId.text
      ),
      "fields" -> Json.arr("content") // Specify the field to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.headOption.flatMap { doc =>
                (doc \ "content").asOpt[String].map { base64Encoded =>
                  Base64.decodeBase64(base64Encoded) // Decode Base64 content
                }
              }
            case JsError(_) => None // Handle JSON parsing errors
          }
        case Left(_) => None // Handle HTTP request errors
      }
    }
  }

  override def getFileByAnalysisId(
                                    profileId: SampleCode,
                                    analysisId: String
                                  ): Future[List[FileUploadedType]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj(
        "analysisId" -> analysisId,
        "profileId" -> profileId.text
      ),
      "fields" -> Json.arr("_id", "name") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                FileUploadedType(
                  (doc \ "_id").asOpt[String].getOrElse(""),
                  (doc \ "name").asOpt[String].getOrElse {
                    val id = (doc \ "_id").asOpt[String].getOrElse("")
                    getDateFromTime(id.takeWhile(_.isDigit).toLong).toString
                  }
                )
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP request errors
      }
    }
  }

  override def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("_id" -> id),
      "fields" -> Json.arr("profileId", "analysisId", "electropherogram") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$electropherogramsUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                connections.FileInterconnection(
                  id,
                  (doc \ "profileId").as[String],
                  (doc \ "analysisId").as[String],
                  None,
                  "ELECTROPHEROGRAM",
                  (doc \ "electropherogram").as[String]
                )
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP request errors
      }
    }
  }
  override def getFullFilesById(id: String): Future[List[FileInterconnection]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("_id" -> id),
      "fields" -> Json.arr("profileId", "analysisId", "name", "content") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$filesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsValue]] match {
            case JsSuccess(docList, _) =>
              docList.map { doc =>
                connections.FileInterconnection(
                  id,
                  (doc \ "profileId").as[String],
                  (doc \ "analysisId").as[String],
                  (doc \ "name").asOpt[String],
                  "FILE",
                  (doc \ "content").as[String]
                )
              }
            case JsError(_) => List.empty // Handle JSON parsing errors
          }
        case Left(_) => List.empty // Handle HTTP request errors
      }
    }
  }

  override def getProfilesMarkers(profiles: Array[Profile]): List[String] = {
    profiles.flatMap(x => {
      x.genotypification.get(1).map(result => {
        result.keySet.map(_.toString)
      }).getOrElse(Nil).toList
    }).toSet.toList
  }
  override def removeFile(id: String): Future[Either[String, String]] = ???

  override def removeEpg(id: String): Future[Either[String, String]] = ???

  def getProfileOwnerByFileId(id: String): Future[(String,SampleCode)] = {
    this.getProfileOwnerByEpgOrFileId(id,filesUrl);
  }
  def getProfileOwnerByEpgId(id: String): Future[(String,SampleCode)] = {
    this.getProfileOwnerByEpgOrFileId(id,electropherogramsUrl);
  }
  def getProfileOwnerByEpgOrFileId(id: String, url: String): Future[(String,SampleCode)] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj("_id" -> id),
      "fields" -> Json.arr("profileId") // Specify the field to retrieve
    )

    val request = basicRequest
      .post(uri"$url/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsObject]] match {
            case JsSuccess(docsList, _) =>
              docsList.headOption match {
                case Some(doc) =>
                  val profileId = (doc \ "profileId").as[String]
                  get(SampleCode(profileId)).map {
                    case Some(profile) => (profile.assignee, profile.globalCode)
                    case None => ("", SampleCode(""))
                  }
                case None => Future.successful(("", SampleCode("")))
              }
            case JsError(_) => Future.successful(("", SampleCode("")))
          }
        case Left(_) => Future.successful(("", SampleCode("")))
      }
    }.flatMap(identity)
  }
  override def getAllProfiles(): Future[List[(SampleCode, String)]] = {
    val query: JsValue = Json.obj(
      "selector" -> Json.obj(),
      "fields" -> Json.arr("globalCode", "categoryId") // Specify fields to retrieve
    )

    val request = basicRequest
      .post(uri"$profilesUrl/_find")
      .body(Json.stringify(query))
      .header("Content-Type", "application/json")
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      val response = request.send(backend)
      response.body match {
        case Right(docs) =>
          val json = Json.parse(docs)
          (json \ "docs").validate[List[JsObject]] match {
            case JsSuccess(docsList, _) =>
              docsList.map { doc =>
                val globalCode = (doc \ "globalCode").as[String]
                val categoryId = (doc \ "categoryId").as[String]
                (SampleCode(globalCode), categoryId)
              }
            case JsError(_) => List.empty
          }
        case Left(_) => List.empty
      }
    }
  }
}