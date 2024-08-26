package connections

import play.api.libs.json.Json
import types.SampleCode

case class ConvertStatusInterconnection(
                            matchId: String,
                            firingCode: SampleCode,
                            leftProfileCode: SampleCode,
                            rightProfileCode: SampleCode,
                            status: String,
                            labOrigin:String,
                            labImmediate:String
                          )

object ConvertStatusInterconnection {
  implicit val format = Json.format[ConvertStatusInterconnection]
}
