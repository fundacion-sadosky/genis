package scenarios

import org.bson.types.ObjectId
import play.api.libs.functional.syntax._
import play.api.libs.json._
import probability.LRResult
import java.util.Date
import scenarios.ScenarioStatus.ScenarioStatus
import types._

case class Scenario(
     _id: MongoId,
     name: String,
     state: ScenarioStatus,
     geneticist: String,
     calculationScenario: CalculationScenario,
     date: MongoDate,
     isRestricted: Boolean,
     result: Option[LRResult],
     description: Option[String]
)

object Scenario {

  implicit val scenarioReads: Reads[Scenario] = (
      (__ \ "_id").read[MongoId].orElse(Reads.pure(MongoId(new ObjectId().toString))) and
      (__ \ "name").read[String] and
      (__ \ "state").read[ScenarioStatus].orElse(Reads.pure(ScenarioStatus.Pending)) and
      (__ \ "geneticist").read[String] and
      (__ \ "calculationScenario").read[CalculationScenario] and
      (__ \ "date").read[MongoDate].orElse(Reads.pure(MongoDate(new Date()))) and
      (__ \ "isRestricted").read[Boolean] and
      (__ \ "result").readNullable[LRResult] and
      (__ \ "description").readNullable[String])(Scenario.apply _)

  implicit val scenarioWrites: OWrites[Scenario] = (
      (__ \ "_id").write[MongoId] and
      (__ \ "name").write[String] and
      (__ \ "state").write[ScenarioStatus] and
      (__ \ "geneticist").write[String] and
      (__ \ "calculationScenario").write[CalculationScenario] and
      (__ \ "date").write[MongoDate] and
      (__ \ "isRestricted").write[Boolean] and
      (__ \ "result").writeNullable[LRResult] and
      (__ \ "description").writeNullable[String])((scenario: Scenario) => (
    scenario._id,
    scenario.name,
    scenario.state,
    scenario.geneticist,
    scenario.calculationScenario,
    scenario.date,
    scenario.isRestricted,
    scenario.result,
    scenario.description))

  implicit val scenarioFormat: OFormat[Scenario] = OFormat(scenarioReads, scenarioWrites)
}