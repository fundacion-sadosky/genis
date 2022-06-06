package matching

import javax.inject.Singleton

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Enumeratee
import play.api.libs.iteratee.Enumerator

trait MatchJobStatus

case object MatchJobStarted extends MatchJobStatus

case object MatchJobEndend extends MatchJobStatus

case object MatchJobFail extends MatchJobStatus

case object PedigreeMatchJobStarted extends MatchJobStatus

case object PedigreeMatchJobEnded extends MatchJobStatus

trait MatchingProcessStatus {
  def getJobStatus(): Enumerator[MatchJobStatus]
  def pushJobStatus(mjs: MatchJobStatus)
}

@Singleton
class MatchingProcessStatusImpl extends MatchingProcessStatus {
  
  private val (out, in) = Concurrent.broadcast[MatchJobStatus]
  
  private val logger = Logger(this.getClass())
  
  override def pushJobStatus(mjs: MatchJobStatus) = {
    logger.trace(s"Match Job Status push: $mjs")
    in.push(mjs)
  }
  
  override def getJobStatus(): Enumerator[MatchJobStatus] = out
  
}