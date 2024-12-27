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

  def findAll(): Future[List[Profile]] = {
    profiles
      .find(Json.obj())
      .cursor[Profile]()
      .collect[List](Int.MaxValue, Cursor.FailOnError[List[Profile]]())
  }

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

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = {

    val cursor = profiles
      .find(Json.obj("globalCode" -> globalCode))
      .cursor[Profile]()

    cursor.headOption.map { opt => opt.map(p => p.genotypification) }
  }

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

  override def get(id: SampleCode): Future[Option[Profile]] = mongoRepo.get(id)

  override def getBy(
                      user: String,
                      isSuperUser: Boolean,
                      internalSampleCode: Option[String],
                      categoryId: Option[String],
                      laboratory: Option[String],
                      hourFrom: Option[Date],
                      hourUntil: Option[Date]
                    ): Future[List[Profile]] =
    mongoRepo.getBy(
      user,
      isSuperUser,
      internalSampleCode,
      categoryId,
      laboratory,
      hourFrom,
      hourUntil
    )

  override def getBetweenDates (
                                 hourFrom: Option[Date],
                                 hourUntil: Option[Date]
                               ): Future[List[Profile]] =
    mongoRepo.getBetweenDates(
      hourFrom,
      hourUntil
    )

  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {
    {
      for {
        r1 <- mongoRepo.findByCode(globalCode)
        r2 <- couchRepo.findByCode(globalCode)
      }
      yield {
        if (!r1.equals(r2)) {
          //println("Mongo encuentra: " + r1)
          //println("Couch encuentra: " + r2)
        } else {
          //println("Iguales")
        }
        r1
      }
    }
  }

  override def add(profile: Profile): Future[SampleCode] =
    mongoRepo.add(profile)

  override def addElectropherogram(
                                    globalCode: SampleCode,
                                    analysisId: String,
                                    image: Array[Byte],
                                    name: String
                                  ): Future[Either[String, SampleCode]] =
    mongoRepo.addElectropherogram(
      globalCode,
      analysisId,
      image,
      name
    )

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] =
    mongoRepo.getElectropherogramsByCode(globalCode: SampleCode)

  override def getElectropherogramImage(
                                         profileId: SampleCode,
                                         electropherogramId: String
                                       ): Future[Option[Array[Byte]]] =
    mongoRepo.getElectropherogramImage(
      profileId,
      electropherogramId
    )

  override def getElectropherogramsByAnalysisId(
                                                 profileId: SampleCode,
                                                 analysisId: String
                                               ): Future[List[FileUploadedType]] =
    mongoRepo.getElectropherogramsByAnalysisId(
      profileId,
      analysisId
    )

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[FileInterconnection]] =
    mongoRepo.getFullElectropherogramsByCode(globalCode)

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]] =
    mongoRepo.getFullFilesByCode(globalCode)

  override def addElectropherogramWithId(
                                          globalCode: SampleCode,
                                          analysisId: String,
                                          image: Array[Byte],
                                          name: String,
                                          id: String
                                        ): Future[Either[String, SampleCode]] =
    mongoRepo.addElectropherogramWithId(
      globalCode,
      analysisId,
      image,
      name,
      id
    )

  override def addFileWithId(
                              globalCode: SampleCode,
                              analysisId: String,
                              image: Array[Byte],
                              name: String,
                              id: String
                            ): Future[Either[String, SampleCode]] =
    mongoRepo.addFileWithId(
      globalCode,
      analysisId,
      image,
      name,
      id
    )

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] =
    mongoRepo.getGenotyficationByCode(globalCode)

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
          println("----------Mongo: " + r1)
          println("----------Couch: " + r2)
          val r1String = r1.mkString
          val r2String = r2.mkString
          val isAnagram = r1String.sorted == r2String.sorted

          println(s"Verificación de string: $isAnagram")

        } else {
          println("********************Iguales**************************")
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
                          ): Future[SampleCode] =
    mongoRepo.addAnalysis(
      _id,
      analysis,
      genotypification,
      labeledGenotypification,
      matchingRules,
      mismatches
    )

  override def saveLabels(
                           globalCode: SampleCode,
                           labels: LabeledGenotypification
                         ): Future[SampleCode] =
    mongoRepo.saveLabels(
      globalCode,
      labels
    )

  override def existProfile(globalCode: SampleCode): Future[Boolean] =
    mongoRepo.existProfile(globalCode)

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] =
    mongoRepo.delete(globalCode)

  override def getLabels(globalCode: SampleCode): Future[Option[LabeledGenotypification]] =
    mongoRepo.getLabels(globalCode)

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

  override def getUnprocessed(): Future[Seq[SampleCode]] = mongoRepo.getUnprocessed()

  override def canDeleteKit(id: String): Future[Boolean] = mongoRepo.canDeleteKit(id: String)

  override def updateProfile(profile: Profile): Future[SampleCode] = mongoRepo.updateProfile(profile: Profile)

  override def findByCodeWithoutAceptedLocus(
                                              globalCode: SampleCode,
                                              aceptedLocus: Seq[String]
                                            ): Future[Option[Profile]] = mongoRepo.findByCodeWithoutAceptedLocus(
    globalCode,
    aceptedLocus
  )

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

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] =
    mongoRepo.getFileByCode(globalCode)

  override def getFile(
                        profileId: SampleCode,
                        electropherogramId: String
                      ): Future[Option[Array[Byte]]] = mongoRepo.getFile(
    profileId,
    electropherogramId
  )

  override def getFileByAnalysisId(
                                    profileId: SampleCode,
                                    analysisId: String
                                  ): Future[List[FileUploadedType]] = mongoRepo.getFileByAnalysisId(
    profileId,
    analysisId
  )

  override def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]] = mongoRepo.getFullElectropherogramsById(id)

  override def getFullFilesById(id: String): Future[List[FileInterconnection]] = mongoRepo.getFullFilesById(id)

  override def getProfilesMarkers(profiles: Array[Profile]): List[String] = mongoRepo.getProfilesMarkers(profiles)

  override def removeFile(id: String): Future[Either[String, String]] = mongoRepo.removeFile(id)

  override def removeEpg(id: String): Future[Either[String, String]] = mongoRepo.removeEpg(id)

  override def getProfileOwnerByFileId(id: String): Future[(String, SampleCode)] = mongoRepo.getProfileOwnerByFileId(id)

  override def getProfileOwnerByEpgId(id: String): Future[(String, SampleCode)] = mongoRepo.getProfileOwnerByEpgId(id)

  override def getAllProfiles(): Future[List[(SampleCode, String)]] = mongoRepo.getAllProfiles()
}

class CouchProfileRepository extends ProfileRepository {
  private val backend = HttpURLConnectionBackend()
  private val baseUrl = "http://localhost:5984/profiles"
  val username = "admin"
  val password = "genisContra" // des-hardcodear esto

  override def get(id: SampleCode): Future[Option[Profile]] = ???

  override def getBy(
                      user: String,
                      isSuperUser: Boolean,
                      internalSampleCode: Option[String],
                      categoryId: Option[String],
                      laboratory: Option[String],
                      hourFrom: Option[Date],
                      hourUntil: Option[Date]
                    ): Future[List[Profile]] = ???

  override def getBetweenDates(
                                hourFrom: Option[Date],
                                hourUntil: Option[Date]
                              ): Future[List[Profile]] = ???

  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = {
    //println("Couch busca: " + globalCode.text)

    val request = basicRequest
      .post(uri"$baseUrl/_find")
      .body(Map("selector" -> Map("globalCode" -> globalCode.text)))
      .header("Accept", "application/json")
      .auth.basic(username, password)

    Future {
      //println("couch request: " + request)
      val response = request.send(backend)
      //println("couch response: " + response)
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
  override def add(profile: Profile): Future[SampleCode] = ???

  override def addElectropherogram(
                                    globalCode: SampleCode,
                                    analysisId: String,
                                    image: Array[Byte],
                                    name: String
                                  ): Future[Either[String, SampleCode]] = ???

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = ???

  override def getElectropherogramImage(
                                         profileId: SampleCode,
                                         electropherogramId: String
                                       ): Future[Option[Array[Byte]]] = ???

  override def getElectropherogramsByAnalysisId(
                                                 profileId: SampleCode,
                                                 analysisId: String
                                               ): Future[List[FileUploadedType]] = ???

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = ???

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = ???

  override def addElectropherogramWithId(
                                          globalCode: SampleCode,
                                          analysisId: String,
                                          image: Array[Byte],
                                          name: String,
                                          id: String
                                        ): Future[Either[String, SampleCode]] = ???

  override def addFileWithId(
                              globalCode: SampleCode,
                              analysisId: String,
                              image: Array[Byte],
                              name: String,
                              id: String
                            ): Future[Either[String, SampleCode]] = ???

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = ???

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
      .post(uri"$baseUrl/_find")
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
                          ): Future[SampleCode] = ???

  override def saveLabels(
                           globalCode: SampleCode,
                           labels: LabeledGenotypification
                         ): Future[SampleCode] = ???

  override def existProfile(globalCode: SampleCode): Future[Boolean] = ???

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = ???

  override def getLabels(globalCode: SampleCode): Future[Option[LabeledGenotypification]] = ???

  override def updateAssocTo(
                              globalCode: SampleCode,
                              to: SampleCode
                            ): Future[(String, String, SampleCode)] = ???

  override def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]] = ???

  override def getUnprocessed(): Future[Seq[SampleCode]] = ???

  override def canDeleteKit(id: String): Future[Boolean] = ???

  override def updateProfile(profile: Profile): Future[SampleCode] = ???

  override def findByCodeWithoutAceptedLocus(
                                              globalCode: SampleCode,
                                              aceptedLocus: Seq[String]
                                            ): Future[Option[Profile]] = ???

  override def addFile(
                        globalCode: SampleCode,
                        analysisId: String,
                        image: Array[Byte],
                        name: String
                      ): Future[Either[String, SampleCode]] = ???

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = ???

  override def getFile(
                        profileId: SampleCode,
                        electropherogramId: String
                      ): Future[Option[Array[Byte]]] = ???

  override def getFileByAnalysisId(
                                    profileId: SampleCode,
                                    analysisId: String
                                  ): Future[List[FileUploadedType]] = ???

  override def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]] = ???

  override def getFullFilesById(id: String): Future[List[FileInterconnection]] = ???

  override def getProfilesMarkers(profiles: Array[Profile]): List[String] = ???

  override def removeFile(id: String): Future[Either[String, String]] = ???

  override def removeEpg(id: String): Future[Either[String, String]] = ???

  override def getProfileOwnerByFileId(id: String): Future[(String, SampleCode)] = ???

  override def getProfileOwnerByEpgId(id: String): Future[(String, SampleCode)] = ???

  override def getAllProfiles(): Future[List[(SampleCode, String)]] = ???
}