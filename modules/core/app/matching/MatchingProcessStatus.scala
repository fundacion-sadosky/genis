package matching

import javax.inject.{Inject, Singleton}

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Source}
import play.api.Logger

// -----------------------------------------------------------------------
// Tipos de estado (idénticos al legacy)
// -----------------------------------------------------------------------

sealed trait MatchJobStatus
case object MatchJobStarted        extends MatchJobStatus
case object MatchJobEndend         extends MatchJobStatus
case object MatchJobFail           extends MatchJobStatus
case object PedigreeMatchJobStarted extends MatchJobStatus
case object PedigreeMatchJobEnded  extends MatchJobStatus

// -----------------------------------------------------------------------
// Trait e implementación
// -----------------------------------------------------------------------

trait MatchingProcessStatus:
  def getJobStatus(): Source[MatchJobStatus, ?]
  def pushJobStatus(mjs: MatchJobStatus): Unit

@Singleton
class MatchingProcessStatusImpl @Inject() ()(using mat: Materializer) extends MatchingProcessStatus:

  private val logger = Logger(this.getClass)

  // Reemplaza Concurrent.broadcast[MatchJobStatus] de Play 2 con Pekko BroadcastHub
  private val (broadcastSink, broadcastSource) =
    MergeHub.source[MatchJobStatus](perProducerBufferSize = 16)
      .toMat(BroadcastHub.sink[MatchJobStatus](bufferSize = 64))(Keep.both)
      .run()

  override def pushJobStatus(mjs: MatchJobStatus): Unit =
    logger.trace(s"Match Job Status push: $mjs")
    Source.single(mjs).runWith(broadcastSink)

  override def getJobStatus(): Source[MatchJobStatus, ?] = broadcastSource