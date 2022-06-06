package pedigree
import play.api.libs.json.Json
//{"courtcaseId":16,"batches":[180]}
case class CaseBatchAdd(courtcaseId:Long,batches:List[Long],tipo:Int)

object CaseBatchAdd {
  implicit val caseBatchAddFormat = Json.format[CaseBatchAdd]
}
