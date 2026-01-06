package matching
import akka.actor.{Actor, Props}
import play.api.Logger
import types.SampleCode

class CollapsingActor(matcher: Spark2MatcherCollapsing) extends Actor {

  private val logger = Logger(this.getClass)

  def receive = {
    case (idCourtCase: Long,user:String) => {
      logger.debug(s"Deque court case $idCourtCase and process with spark matcher collapsing")
      matcher.collapseBlocking(idCourtCase,user)
    }
  }

}

private object CollapsingActor {
  def props[T](matcher: Spark2MatcherCollapsing): Props = Props(new CollapsingActor(matcher))
}


