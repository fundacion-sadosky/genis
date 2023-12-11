package types

import org.scalatest.FlatSpec

class PermissionTest extends FlatSpec {

  "each regex in Permission.list permissions" should "match one or two Operations" in {

    val allOperations = Permission
      .list
      .flatMap {_.operations}

    val allResourcesAndAction: List[(String, String)] = allOperations
      .toList
      .flatMap {
        x => x.resource
          .toString()
          .split("[|]")
          .map(y => (y , x.action.toString()))
          .toList
      }
      .map {
        case (r, a) => (
          r.replace(".*", "x"),
          a.replace(".*", "x")
        )
      }
    val result = allResourcesAndAction
      .map {
        case (rsrc, act) =>
          allOperations
            .map {
              op => {
                (
                  op.resource.findAllIn(rsrc).length,
                  op.action.findAllIn(act).length
                )
              }
            }
            .count {
              case (a, b) => a > 0 && b > 0
            }
      }
    assert(result.forall( x => x == 1 || x == 2))
  }
}

