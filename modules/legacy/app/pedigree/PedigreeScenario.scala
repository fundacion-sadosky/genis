package pedigree

import org.bson.types.ObjectId
import play.api.libs.json._
import types.MongoId
import play.api.libs.functional.syntax._
import scenarios.ScenarioStatus
import scenarios.ScenarioStatus.ScenarioStatus

case class PedigreeScenario(
   _id: MongoId,
   pedigreeId: Long,
   name: String,
   description: String,
   genogram: Seq[Individual],
   status: ScenarioStatus.Value = ScenarioStatus.Pending,
   frequencyTable: String,
   isProcessing: Boolean = false,
   lr: Option[String] = None,
   validationComment: Option[String] = None,
   matchId: Option[String] = None,
   mutationModelId: Option[Long] = None
)

object PedigreeScenario {
  implicit val longReads: Reads[Long] = new Reads[Long] {
    def reads(jv: JsValue): JsResult[Long] = JsSuccess(jv.as[String].toLong)
  }

  implicit val longWrites: Writes[Long] = new Writes[Long] {
    def writes(l: Long): JsValue = JsString(l.toString)
  }
  implicit val doubleReads: Reads[Double] = new Reads[Double] {
    def reads(jv: JsValue): JsResult[Double] = JsSuccess(jv.as[String].toDouble)
  }

  implicit val doubleWrites: Writes[Double] = new Writes[Double] {
    def writes(l: Double): JsValue = Json.toJson(l)
  }
  implicit val mapFormat: Format[Long] = Format(longReads, longWrites)
  implicit val mapFormatDouble: Format[Double] = Format(doubleReads, doubleWrites)

  implicit val scenarioReads: Reads[PedigreeScenario] = (
    (__ \ "_id").read[MongoId].orElse(Reads.pure(MongoId(new ObjectId().toString))) and
    (__ \ "pedigreeId").read[Long] and
    (__ \ "name").read[String] and
    (__ \ "description").read[String] and
    (__ \ "genogram").read[Seq[Individual]] and
    (__ \ "status").read[ScenarioStatus].orElse(Reads.pure(ScenarioStatus.Pending)) and
    (__ \ "frequencyTable").read[String] and
    (__ \ "isProcessing").read[Boolean].orElse(Reads.pure(false)) and
    (__ \ "lr").readNullable[String] and
    (__ \ "validationComment").readNullable[String] and
    (__ \ "matchId").readNullable[String] and
    (__ \ "mutationModelId").readNullable[Long])(PedigreeScenario.apply _)


  implicit val scenarioWrites: OWrites[PedigreeScenario] = (
    (__ \ "_id").write[MongoId] and
    (__ \ "pedigreeId").write[Long] and
    (__ \ "name").write[String] and
    (__ \ "description").write[String] and
    (__ \ "genogram").write[Seq[Individual]] and
    (__ \ "status").write[ScenarioStatus] and
    (__ \ "frequencyTable").write[String] and
    (__ \ "isProcessing").write[Boolean] and
    (__ \ "lr").writeNullable[String] and
    (__ \ "validationComment").writeNullable[String] and
    (__ \ "matchId").writeNullable[String] and
    (__ \ "mutationModelId").writeNullable[Long])((pedigreeScenario: PedigreeScenario) => (
    pedigreeScenario._id,
    pedigreeScenario.pedigreeId,
    pedigreeScenario.name,
    pedigreeScenario.description,
    pedigreeScenario.genogram,
    pedigreeScenario.status,
    pedigreeScenario.frequencyTable,
    pedigreeScenario.isProcessing,
    pedigreeScenario.lr.map(_.toString),
    pedigreeScenario.validationComment,
    pedigreeScenario.matchId,
    pedigreeScenario.mutationModelId))

  implicit val scenarioFormat: OFormat[PedigreeScenario] = OFormat(scenarioReads, scenarioWrites)
}