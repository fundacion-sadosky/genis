package pedigree

import play.api.mvc.{PathBindable, QueryStringBindable}
import types.ConstrainedText

case class NodeAlias(override val text: String)
  extends ConstrainedText(text, NodeAlias.validationRe)

object NodeAlias:
  val validationRe = """^[a-zA-Z0-9\-]{1,15}$""".r
  implicit val reads: play.api.libs.json.Reads[NodeAlias]            = ConstrainedText.readsOf(NodeAlias.apply)
  implicit val writes: play.api.libs.json.Writes[NodeAlias]          = ConstrainedText.writesOf[NodeAlias]
  implicit val qsBinder: QueryStringBindable[NodeAlias]              = ConstrainedText.qsBinderOf(NodeAlias.apply)
  implicit val pathBinder: PathBindable[NodeAlias]                   = ConstrainedText.pathBinderOf(NodeAlias.apply)
