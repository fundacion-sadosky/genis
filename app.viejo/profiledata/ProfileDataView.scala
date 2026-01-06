package profiledata

import play.api.libs.json.{Format, Json}
import types.{AlphanumericId, SampleCode}

case class ProfileDataView( globalCode: SampleCode,
                            category: AlphanumericId,
                            internalSampleCode: String,
                            assignee: String,
                            associated:Boolean = false
                          )

object ProfileDataView {
  implicit val profiledataViewFormat: Format[ProfileDataView] = Json.format
}
