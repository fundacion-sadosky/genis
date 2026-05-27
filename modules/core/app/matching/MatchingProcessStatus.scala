package matching

import play.api.Logger

// ---------------------------------------------------------------------------
// MatchingProcessStatus - tracks pedigree/profile matching job lifecycle
// Migrated from legacy matching.MatchingProcessStatus.
// Legacy used Akka Iteratees (Play 2.x only); here simplified to a Logger.
// ---------------------------------------------------------------------------

trait MatchJobStatus

case object MatchJobStarted        extends MatchJobStatus
case object MatchJobEnded          extends MatchJobStatus
case object MatchJobFail           extends MatchJobStatus
case object PedigreeMatchJobStarted extends MatchJobStatus
case object PedigreeMatchJobEnded  extends MatchJobStatus

trait MatchingProcessStatus:
  def pushJobStatus(mjs: MatchJobStatus): Unit

@jakarta.inject.Singleton
class MatchingProcessStatusImpl extends MatchingProcessStatus:
  private val logger = Logger(this.getClass)
  override def pushJobStatus(mjs: MatchJobStatus): Unit =
    logger.info(s"Match job status: $mjs")
