package profile

import java.util.{Base64, Date}
import javax.inject.{Inject, Singleton}
import com.mongodb.client.{MongoClient, MongoClients, MongoCollection, MongoDatabase}
import com.mongodb.client.model.{Filters, Projections, Sorts, Updates}
import configdata.MatchingRule
import connections.FileInterconnection
import org.bson.Document
import org.bson.types.{Binary, ObjectId}
import play.api.Configuration
import play.api.libs.json.*
import profile.GenotypificationByType.GenotypificationByType
import types.*

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.*

@Singleton
class MongoProfileRepository @Inject()(
  config: Configuration
)(implicit ec: ExecutionContext) extends ProfileRepository {

  private val mongoUri = config.get[String]("mongodb.uri")
  private val dbName = config.get[String]("mongodb.database")

  private lazy val mongoClient: MongoClient = MongoClients.create(mongoUri)
  private lazy val database: MongoDatabase = mongoClient.getDatabase(dbName)

  private def profiles: MongoCollection[Document] = database.getCollection("profiles")
  private def electropherograms: MongoCollection[Document] = database.getCollection("electropherograms")
  private def files: MongoCollection[Document] = database.getCollection("files")

  private def profileToDocument(profile: Profile): Document =
    Document.parse(Json.toJson(profile).toString())

  private def documentToProfile(doc: Document): Profile =
    Json.parse(doc.toJson()).as[Profile]

  override def get(id: SampleCode): Future[Option[Profile]] = Future {
    Option(profiles.find(Filters.eq("_id", id.text)).first()).map(documentToProfile)
  }

  override def getBy(
    user: String,
    isSuperUser: Boolean,
    internalSampleCode: Option[String],
    categoryId: Option[String],
    laboratory: Option[String],
    hourFrom: Option[Date],
    hourUntil: Option[Date]
  ): Future[List[Profile]] = Future {
    val filters = new java.util.ArrayList[org.bson.conversions.Bson]()

    internalSampleCode.filter(_.nonEmpty).foreach(isc =>
      filters.add(Filters.eq("internalSampleCode", isc))
    )
    categoryId.filter(_.nonEmpty).foreach(cid =>
      filters.add(Filters.eq("categoryId", cid))
    )

    val dateFilters = new java.util.ArrayList[org.bson.conversions.Bson]()
    hourFrom.foreach(hf => dateFilters.add(Filters.gte("date", hf)))
    hourUntil.foreach(hu => dateFilters.add(Filters.lte("date", hu)))

    if (!dateFilters.isEmpty) {
      filters.add(Filters.elemMatch("analyses", Filters.and(dateFilters)))
    }

    if (!isSuperUser) {
      filters.add(Filters.eq("assignee", user))
    }
    filters.add(Filters.eq("deleted", false))

    val query = if (filters.isEmpty) new Document() else Filters.and(filters)
    val result = profiles.find(query).into(new java.util.ArrayList[Document]()).asScala.toList.map(documentToProfile)

    laboratory.filter(_.nonEmpty) match {
      case Some(lab) => result.filter(_.globalCode.text.contains(s"-$lab-"))
      case None => result
    }
  }

  override def getBetweenDates(hourFrom: Option[Date], hourUntil: Option[Date]): Future[List[Profile]] = Future {
    val filters = new java.util.ArrayList[org.bson.conversions.Bson]()

    val dateFilters = new java.util.ArrayList[org.bson.conversions.Bson]()
    hourFrom.foreach(hf => dateFilters.add(Filters.gte("date", hf)))
    hourUntil.foreach(hu => dateFilters.add(Filters.lte("date", hu)))

    if (!dateFilters.isEmpty) {
      filters.add(Filters.elemMatch("analyses", Filters.and(dateFilters)))
    }

    filters.add(Filters.eq("deleted", false))
    val query = Filters.and(filters)
    profiles.find(query).into(new java.util.ArrayList[Document]()).asScala.toList.map(documentToProfile)
  }

  override def findByCode(globalCode: SampleCode): Future[Option[Profile]] = Future {
    Option(profiles.find(Filters.eq("globalCode", globalCode.text)).sort(Sorts.descending("globalCode")).first()).map(documentToProfile)
  }

  override def add(profile: Profile): Future[SampleCode] = Future {
    profiles.insertOne(profileToDocument(profile))
    profile._id
  }

  override def addElectropherogram(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String
  ): Future[Either[String, SampleCode]] = Future {
    try {
      val doc = new Document("profileId", globalCode.text)
        .append("analysisId", analysisId)
        .append("electropherogram", new Binary(image))
      if (name != null) doc.append("name", name)
      electropherograms.insertOne(doc)
      Right(globalCode)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  override def addElectropherogramWithId(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String,
    id: String
  ): Future[Either[String, SampleCode]] = Future {
    try {
      val doc = new Document("_id", new ObjectId(id))
        .append("profileId", globalCode.text)
        .append("analysisId", analysisId)
        .append("electropherogram", new Binary(image))
      if (name != null) doc.append("name", name)
      electropherograms.insertOne(doc)
      Right(globalCode)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  override def getElectropherogramsByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = Future {
    electropherograms.find(Filters.eq("profileId", globalCode.text))
      .projection(Projections.include("_id", "analysisId", "name"))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        val id = doc.getObjectId("_id").toString
        val analysisId = doc.getString("analysisId")
        val name = Option(doc.getString("name")).getOrElse(getDateFromTime(doc.getObjectId("_id").getTimestamp * 1000L))
        (id, analysisId, name)
      }
  }

  private def getDateFromTime(time: Long): String = {
    val date = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime
    date.format(java.time.format.DateTimeFormatter.ISO_DATE_TIME)
  }

  override def getFullElectropherogramsByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = Future {
    electropherograms.find(Filters.eq("profileId", globalCode.text))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileInterconnection(
          doc.getObjectId("_id").toString,
          globalCode.text,
          doc.getString("analysisId"),
          Option(doc.getString("name")),
          "ELECTROPHEROGRAM",
          Base64.getEncoder.encodeToString(doc.get("electropherogram", classOf[Binary]).getData)
        )
      }
  }

  override def getFullFilesByCode(globalCode: SampleCode): Future[List[FileInterconnection]] = Future {
    files.find(Filters.eq("profileId", globalCode.text))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileInterconnection(
          doc.getObjectId("_id").toString,
          globalCode.text,
          doc.getString("analysisId"),
          Option(doc.getString("name")),
          "FILE",
          Base64.getEncoder.encodeToString(doc.get("content", classOf[Binary]).getData)
        )
      }
  }

  override def getElectropherogramImage(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = Future {
    Option(
      electropherograms.find(
        Filters.and(Filters.eq("_id", new ObjectId(electropherogramId)), Filters.eq("profileId", profileId.text))
      ).first()
    ).map(doc => doc.get("electropherogram", classOf[Binary]).getData)
  }

  override def getElectropherogramsByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = Future {
    electropherograms.find(
      Filters.and(Filters.eq("analysisId", analysisId), Filters.eq("profileId", profileId.text))
    ).projection(Projections.include("_id", "name"))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileUploadedType(
          doc.getObjectId("_id").toString,
          Option(doc.getString("name")).getOrElse(getDateFromTime(doc.getObjectId("_id").getTimestamp * 1000L))
        )
      }
  }

  override def existProfile(globalCode: SampleCode): Future[Boolean] = Future {
    profiles.countDocuments(Filters.eq("_id", globalCode.text)) > 0
  }

  override def getGenotyficationByCode(globalCode: SampleCode): Future[Option[GenotypificationByType]] = Future {
    Option(profiles.find(Filters.eq("globalCode", globalCode.text)).first())
      .map(doc => documentToProfile(doc).genotypification)
  }

  override def findByCodes(globalCodes: Seq[SampleCode]): Future[Seq[Profile]] = Future {
    val codes = globalCodes.map(_.text).asJava
    profiles.find(Filters.in("_id", codes))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq.map(documentToProfile)
  }

  override def addAnalysis(
    _id: SampleCode,
    analysis: Analysis,
    genotypification: GenotypificationByType,
    labeledGenotypification: Option[Profile.LabeledGenotypification],
    matchingRules: Option[Seq[MatchingRule]],
    mismatches: Option[Profile.Mismatch]
  ): Future[SampleCode] = Future {
    val genJson = Document.parse(Json.toJson(genotypification).toString())
    val labeledJson = labeledGenotypification.map(lg => Document.parse(Json.toJson(lg).toString())).orNull
    val rulesJson = matchingRules.map(mr => Document.parse(Json.toJson(mr).toString())).orNull
    val mismatchJson = mismatches.map(m => Document.parse(Json.toJson(m).toString())).orNull
    val analysisDoc = Document.parse(Json.toJson(analysis).toString())

    val setUpdate = new Document("genotypification", genJson)
      .append("labeledGenotypification", labeledJson)
      .append("matchingRules", rulesJson)
      .append("mismatches", mismatchJson)
      .append("matcheable", false)
      .append("processed", false)

    val update = new Document("$set", setUpdate).append("$push", new Document("analyses", analysisDoc))
    val result = profiles.updateOne(Filters.eq("_id", _id.text), update)
    if (result.getModifiedCount > 0) _id
    else throw new RuntimeException("Error updating profile analysis")
  }

  override def updateProfile(profile: Profile): Future[SampleCode] = Future {
    val setDoc = new Document()
      .append("internalSampleCode", profile.internalSampleCode)
      .append("assignee", profile.assignee)
      .append("categoryId", profile.categoryId.text)
      .append("genotypification", Document.parse(Json.toJson(profile.genotypification).toString()))
      .append("analyses", profile.analyses.map(a => Document.parse(Json.toJson(a).toString())).orNull)
      .append("labeledGenotypification", profile.labeledGenotypification.map(lg => Document.parse(Json.toJson(lg).toString())).orNull)
      .append("contributors", profile.contributors.map(Int.box).orNull)
      .append("matchingRules", profile.matchingRules.map(mr => Document.parse(Json.toJson(mr).toString())).orNull)
      .append("associatedTo", profile.associatedTo.map(at => at.map(_.text).asJava).orNull)
      .append("deleted", profile.deleted)
      .append("matcheable", false)
      .append("isReference", profile.isReference)
      .append("processed", false)

    val result = profiles.updateOne(Filters.eq("_id", profile._id.text), new Document("$set", setDoc))
    if (result.getModifiedCount > 0) profile._id
    else throw new RuntimeException("Error updating profile")
  }

  override def saveLabels(globalCode: SampleCode, labels: Profile.LabeledGenotypification): Future[SampleCode] = Future {
    val labelsDoc = Document.parse(Json.toJson(labels).toString())
    val setDoc = new Document("labeledGenotypification", labelsDoc)
      .append("matcheable", false)
      .append("processed", false)

    val result = profiles.updateOne(Filters.eq("_id", globalCode.text), new Document("$set", setDoc))
    if (result.getModifiedCount > 0) globalCode
    else throw new RuntimeException("Error saving labels")
  }

  override def delete(globalCode: SampleCode): Future[Either[String, SampleCode]] = Future {
    val result = profiles.updateOne(Filters.eq("_id", globalCode.text), new Document("$set", new Document("deleted", true)))
    if (result.getModifiedCount > 0) Right(globalCode)
    else Left("Error deleting profile")
  }

  override def getLabels(globalCode: SampleCode): Future[Option[Profile.LabeledGenotypification]] = Future {
    Option(
      profiles.find(Filters.eq("_id", globalCode.text))
        .projection(Projections.include("labeledGenotypification"))
        .first()
    ).flatMap { doc =>
      val js = Json.parse(doc.toJson())
      (js \ "labeledGenotypification").asOpt[Map[Profile.MixLabel, Profile.Genotypification]]
    }
  }

  override def updateAssocTo(globalCode: SampleCode, to: SampleCode): Future[(String, String, SampleCode)] = Future {
    val doc = profiles.findOneAndUpdate(
      Filters.eq("_id", globalCode.text),
      Updates.push("associatedTo", to.text)
    )
    val assignee = doc.getString("assignee")
    val internalSampleCode = doc.getString("internalSampleCode")
    val gc = SampleCode(doc.getString("globalCode"))
    (assignee, internalSampleCode, gc)
  }

  override def setMatcheableAndProcessed(globalCode: SampleCode): Future[Either[String, SampleCode]] = Future {
    val setDoc = new Document("matcheable", true).append("processed", true)
    val result = profiles.updateOne(Filters.eq("_id", globalCode.text), new Document("$set", setDoc))
    if (result.getModifiedCount > 0) Right(globalCode)
    else Left("Error setting matcheable and processed")
  }

  override def getUnprocessed(): Future[Seq[SampleCode]] = Future {
    profiles.find(Filters.eq("processed", false))
      .projection(Projections.include("globalCode"))
      .into(new java.util.ArrayList[Document]()).asScala.toSeq
      .map(doc => SampleCode(doc.getString("globalCode")))
  }

  override def canDeleteKit(id: String): Future[Boolean] = Future {
    Option(profiles.find(Filters.eq("analyses.kit", id)).first()).isEmpty
  }

  override def findByCodeWithoutAceptedLocus(globalCode: SampleCode, aceptedLocus: Seq[String]): Future[Option[Profile]] = Future {
    val excludeFields = aceptedLocus.map(x => "genotypification.1." + x)
    val projection = Projections.exclude(excludeFields.asJava)
    Option(
      profiles.find(Filters.eq("globalCode", globalCode.text))
        .projection(projection)
        .sort(Sorts.descending("globalCode"))
        .first()
    ).map(documentToProfile)
  }

  override def addFile(globalCode: SampleCode, analysisId: String, image: Array[Byte], name: String): Future[Either[String, SampleCode]] = Future {
    try {
      val doc = new Document("profileId", globalCode.text)
        .append("analysisId", analysisId)
        .append("content", new Binary(image))
        .append("name", name)
      files.insertOne(doc)
      Right(globalCode)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  override def addFileWithId(
    globalCode: SampleCode,
    analysisId: String,
    image: Array[Byte],
    name: String,
    id: String
  ): Future[Either[String, SampleCode]] = Future {
    try {
      val doc = new Document("_id", new ObjectId(id))
        .append("profileId", globalCode.text)
        .append("analysisId", analysisId)
        .append("content", new Binary(image))
        .append("name", name)
      files.insertOne(doc)
      Right(globalCode)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  override def getFileByCode(globalCode: SampleCode): Future[List[(String, String, String)]] = Future {
    files.find(Filters.eq("profileId", globalCode.text))
      .projection(Projections.include("_id", "analysisId", "name"))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        (doc.getObjectId("_id").toString, doc.getString("analysisId"), doc.getString("name"))
      }
  }

  override def getFile(profileId: SampleCode, electropherogramId: String): Future[Option[Array[Byte]]] = Future {
    Option(
      files.find(
        Filters.and(Filters.eq("_id", new ObjectId(electropherogramId)), Filters.eq("profileId", profileId.text))
      ).first()
    ).map(doc => doc.get("content", classOf[Binary]).getData)
  }

  override def getFileByAnalysisId(profileId: SampleCode, analysisId: String): Future[List[FileUploadedType]] = Future {
    files.find(
      Filters.and(Filters.eq("analysisId", analysisId), Filters.eq("profileId", profileId.text))
    ).projection(Projections.include("_id", "name"))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileUploadedType(doc.getObjectId("_id").toString, doc.getString("name"))
      }
  }

  override def getFullElectropherogramsById(id: String): Future[List[FileInterconnection]] = Future {
    electropherograms.find(Filters.eq("_id", new ObjectId(id)))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileInterconnection(
          id,
          doc.getString("profileId"),
          doc.getString("analysisId"),
          None,
          "ELECTROPHEROGRAM",
          Base64.getEncoder.encodeToString(doc.get("electropherogram", classOf[Binary]).getData)
        )
      }
  }

  override def getFullFilesById(id: String): Future[List[FileInterconnection]] = Future {
    files.find(Filters.eq("_id", new ObjectId(id)))
      .into(new java.util.ArrayList[Document]()).asScala.toList.map { doc =>
        FileInterconnection(
          id,
          doc.getString("profileId"),
          doc.getString("analysisId"),
          Option(doc.getString("name")),
          "FILE",
          Base64.getEncoder.encodeToString(doc.get("content", classOf[Binary]).getData)
        )
      }
  }

  override def getProfilesMarkers(profiles: Array[Profile]): List[String] = {
    profiles.flatMap { x =>
      x.genotypification.get(1).map(result =>
        result.keySet.map(_.toString)
      ).getOrElse(Nil).toList
    }.toSet.toList
  }

  override def removeFile(id: String): Future[Either[String, String]] = Future {
    files.findOneAndDelete(Filters.eq("_id", new ObjectId(id)))
    Right(id)
  }

  override def removeEpg(id: String): Future[Either[String, String]] = Future {
    electropherograms.findOneAndDelete(Filters.eq("_id", new ObjectId(id)))
    Right(id)
  }

  override def removeAll(): Future[Either[String, String]] = Future {
    profiles.deleteMany(new Document())
    files.deleteMany(new Document())
    electropherograms.deleteMany(new Document())
    Right("all")
  }

  override def removeProfile(globalCode: SampleCode): Future[Either[String, String]] = Future {
    profiles.findOneAndDelete(Filters.eq("_id", globalCode.text))
    Right(globalCode.text)
  }

  override def getProfileOwnerByFileId(id: String): Future[(String, SampleCode)] =
    getProfileOwnerByDocId(id, files)

  override def getProfileOwnerByEpgId(id: String): Future[(String, SampleCode)] =
    getProfileOwnerByDocId(id, electropherograms)

  private def getProfileOwnerByDocId(id: String, collection: MongoCollection[Document]): Future[(String, SampleCode)] = Future {
    val doc = collection.find(Filters.eq("_id", new ObjectId(id))).first()
    if (doc != null) {
      val profileId = doc.getString("profileId")
      val profile = Option(profiles.find(Filters.eq("_id", profileId)).first()).map(documentToProfile)
      profile.map(p => (p.assignee, p.globalCode)).getOrElse(("", SampleCode("AR-X-EMPTY-1")))
    } else {
      ("", SampleCode("AR-X-EMPTY-1"))
    }
  }

  override def getAllProfiles(): Future[List[(SampleCode, String)]] = Future {
    profiles.find()
      .into(new java.util.ArrayList[Document]()).asScala.toList
      .map(documentToProfile)
      .map(p => (p.globalCode, p.categoryId.text))
  }
}
