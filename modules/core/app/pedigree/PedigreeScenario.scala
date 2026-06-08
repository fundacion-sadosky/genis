package pedigree

import org.bson.types.ObjectId
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import scenarios.ScenarioStatus
import matching.MongoId

// ---------------------------------------------------------------------------
// PedigreeScenario — a hypothesis scenario on a pedigree for LR calculation.
// ---------------------------------------------------------------------------

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

object PedigreeScenario:
  given longReads: Reads[Long]   = Reads(jv => JsSuccess(jv.as[String].toLong))
  given longWrites: Writes[Long] = Writes(l => JsString(l.toString))
  given longFormat: Format[Long] = Format(longReads, longWrites)

  implicit val scenarioReads: Reads[PedigreeScenario] = (
    (__ \ "_id").read[MongoId].orElse(Reads.pure(MongoId(new ObjectId().toString))) ~
    (__ \ "pedigreeId").read[Long] ~
    (__ \ "name").read[String] ~
    (__ \ "description").read[String] ~
    (__ \ "genogram").read[Seq[Individual]] ~
    (__ \ "status").read[ScenarioStatus.Value].orElse(Reads.pure(ScenarioStatus.Pending)) ~
    (__ \ "frequencyTable").read[String] ~
    (__ \ "isProcessing").read[Boolean].orElse(Reads.pure(false)) ~
    (__ \ "lr").readNullable[String] ~
    (__ \ "validationComment").readNullable[String] ~
    (__ \ "matchId").readNullable[String] ~
    (__ \ "mutationModelId").readNullable[Long]
  )(PedigreeScenario.apply)

  implicit val scenarioWrites: OWrites[PedigreeScenario] = (
    (__ \ "_id").write[MongoId] ~
    (__ \ "pedigreeId").write[Long] ~
    (__ \ "name").write[String] ~
    (__ \ "description").write[String] ~
    (__ \ "genogram").write[Seq[Individual]] ~
    (__ \ "status").write[ScenarioStatus.Value] ~
    (__ \ "frequencyTable").write[String] ~
    (__ \ "isProcessing").write[Boolean] ~
    (__ \ "lr").writeNullable[String] ~
    (__ \ "validationComment").writeNullable[String] ~
    (__ \ "matchId").writeNullable[String] ~
    (__ \ "mutationModelId").writeNullable[Long]
  )((s: PedigreeScenario) => (
    s._id, s.pedigreeId, s.name, s.description, s.genogram, s.status, s.frequencyTable,
    s.isProcessing, s.lr.map(_.toString), s.validationComment, s.matchId, s.mutationModelId))

  implicit val format: OFormat[PedigreeScenario] = OFormat(scenarioReads, scenarioWrites)
