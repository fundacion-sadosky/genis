package util

import scala.annotation.tailrec
import play.api.Logger

object Misc {

  val logger = Logger(Misc.getClass())

  /**
   * Test whether every element in source collection can be converted into an element of target collection.
   * So relations between source and target elements define an injection. This is each element in
   */
  def existsInjection[A, B](source: List[A], target: List[B], isRelated: (A, B) => Boolean): Boolean = {
    _existsInjection(source, target, 0, isRelated)
  }

  //@tailrec
  private def _existsInjection[A, B](source: List[A], target: List[B], from: Int, isRelated: (A, B) => Boolean): Boolean = {

    def findFirstMatch(element: A, list: List[B], from: Int): Option[(Int, B)] = {
      val index = list.indexWhere(isRelated(element, _), from)
      if (index >= 0) Some((index, list(index)))
      else None
    }

    source match {
      case Nil => true
      case head :: cons => {
        findFirstMatch(head, target, from) match {
          case None => {
            logger.trace(s"Can't relate '$head' to any element in $target")
            false
          }
          case Some((index, element)) => {
            {
              logger.trace(s"match found: $head -> $element. Going down ...")
              val (l, r) = target.splitAt(index)
              _existsInjection(cons, l ::: r.tail, 0, isRelated)
            } || {
              logger.trace(s"match: $head -> $element produce no result ... branching, will find matches from ${index + 1}")
              _existsInjection(source, target, index + 1, isRelated)
            }
          }
        }
      }
    }

  }

}