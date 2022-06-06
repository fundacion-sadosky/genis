package matching

import akka.actor.{Actor, Props}
import play.api.Logger
import types.SampleCode

class MatchingActor(matcher: Spark2Matcher) extends Actor {

  private val logger = Logger(this.getClass)

  def receive = {
    case sampleCode: SampleCode =>
      logger.debug(s"Deque $sampleCode and process with spark matcher")
      matcher.findMatchesBlocking(sampleCode)
  }

}

private object MatchingActor {
  def props[T](matcher: Spark2Matcher): Props = Props(new MatchingActor(matcher))
}

