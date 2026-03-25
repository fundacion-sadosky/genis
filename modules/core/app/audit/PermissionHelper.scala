package audit

import security.StaticAuthorisationOperation
import types.Permission

/** Translates an HTTP (method, path) pair to its permission description key.
 *  Replaces the legacy user.RoleService.translatePermission dependency. */
object PermissionHelper {

  private def allOperations(): Set[StaticAuthorisationOperation] =
    Permission.list.flatMap(_.operations)

  private def matchQuality(action: String, resource: String)(op: StaticAuthorisationOperation): (Int, Int) =
    (op.action.findAllIn(action).size, op.resource.findAllIn(resource).size)

  def translatePermission(method: String, path: String): String = {
    val ops = allOperations()
    val valid = ops.filterNot { op =>
      matchQuality(method, path)(op) match {
        case (0, _) => true
        case (_, 0) => true
        case _      => false
      }
    }
    if (valid.isEmpty) "Unknown"
    else valid.minBy(op => matchQuality(method, path)(op) match {
      case (a, b) => a + b
    }).descriptionKey
  }
}
