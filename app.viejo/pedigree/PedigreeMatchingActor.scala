package pedigree

import akka.actor.{Actor, Props}
import play.api.Logger
import types.SampleCode

class PedigreeMatchingActor(matcher: PedigreeSparkMatcher) extends Actor {

  private val logger = Logger(this.getClass)

  def receive = {
    case (sampleCode: SampleCode, matchType: String) =>
      logger.debug(s"Deque profile $sampleCode and process with spark matcher")
      matcher.findMatchesBlocking(sampleCode, matchType)
    case pedigreeId: Long =>
      logger.debug(s"Deque pedigree $pedigreeId and process with spark matcher")
      matcher.findMatchesBlocking(pedigreeId)
  }

}

private object PedigreeMatchingActor {
  def props[T](matcher: PedigreeSparkMatcher): Props = Props(new PedigreeMatchingActor(matcher))
}