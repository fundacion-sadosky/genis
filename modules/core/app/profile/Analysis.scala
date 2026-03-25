package profile

import java.util.Date
import configdata.MatchingRule
import play.api.libs.json.*
import types.{MongoDate, SampleCode, StatOption}

case class Analysis(
  id: String,
  date: MongoDate,
  kit: String,
  genotypification: Profile.Genotypification,
  `type`: Option[Int]
)

object Analysis {
  implicit val format: Format[Analysis] = Json.format[Analysis]
}

case class NewAnalysis(
  globalCode: SampleCode,
  userId: String,
  token: String,
  kit: Option[String] = None,
  `type`: Option[Int] = None,
  genotypification: Profile.Genotypification,
  labeledGenotypification: Option[Profile.LabeledGenotypification],
  contributors: Option[Int],
  mismatches: Option[Profile.Mismatch],
  matchingRules: Option[Seq[MatchingRule]] = None,
  tokenRawFile: Option[String] = None,
  nameRawFile: Option[String] = None
)

object NewAnalysis {
  implicit val format: Format[NewAnalysis] = Json.format[NewAnalysis]
}

case class FileUploadedType(fileId: String, name: String)

object FileUploadedType {
  implicit val format: Format[FileUploadedType] = Json.format[FileUploadedType]
}

case class AnalysisModelView(
  id: String,
  date: Date,
  kit: String,
  genotypification: Profile.Genotypification,
  efgs: List[FileUploadedType],
  `type`: Option[Int],
  files: List[FileUploadedType]
)

object AnalysisModelView {
  implicit val format: Format[AnalysisModelView] = Json.format[AnalysisModelView]
}
