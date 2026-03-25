package audit

import java.security.SecureRandom

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag

import org.apache.pekko.actor.{Actor, ActorLogging, ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import play.api.Logger

abstract class PEOSignerService {
  def addEntry[E <: Stringifiable, S <: Signature, R](
    entry: PEOEntry[E, S, R]
  )(implicit ct: ClassTag[R]): Future[R]
}

case class PEOEntry[E <: Stringifiable, S <: Signature, R](
  obj:     E,
  build:   (E, Key, Long) => S,
  persist: S => Future[R]
)

class AkkaPEOSignerService(
  actorSystem:   ActorSystem,
  logRepository: OperationLogRepository,
  randomAlg:     String,
  lotSize:       Int
) extends PEOSignerService {

  private val logger    = Logger(this.getClass)
  private val random    = SecureRandom.getInstance(randomAlg)
  private val peoActor  = actorSystem.actorOf(PEOSignerActor.props(logRepository, random, lotSize))

  override def addEntry[E <: Stringifiable, S <: Signature, R](
    entry: PEOEntry[E, S, R]
  )(implicit ct: ClassTag[R]): Future[R] = {
    implicit val timeout: Timeout = Timeout(10.seconds)
    (peoActor ? entry).mapTo[R]
  }
}

class PEOSignerActor(
  logRepository: OperationLogRepository,
  random:        SecureRandom,
  lotSize:       Int
) extends Actor with ActorLogging {

  import context.dispatcher

  private val awaitTimeout = 100.seconds

  private def generateNewLot(): (Key, Long) = {
    val kZero = Key(random)
    val lotId = Await.result(logRepository.createLot(kZero), awaitTimeout)
    (kZero, lotId)
  }

  private def lotBehavior(count: Int, lotId: Long, prevKey: Key): Receive = {
    case PEOEntry(entry, build, persist) =>
      val signed   = build(entry, prevKey, lotId)
      val result   = Await.result(persist(signed), awaitTimeout)
      sender() ! result
      log.debug(s"signed entry #$count -> ${signed.signature}")
      if (count >= lotSize - 1) {
        val (k0, id) = generateNewLot()
        context.become(lotBehavior(0, id, k0))
      } else {
        context.become(lotBehavior(count + 1, lotId, signed.signature))
      }
  }

  private val (initKey, initLotId) = generateNewLot()

  override def receive: Receive = lotBehavior(0, initLotId, initKey)
}

object PEOSignerActor {
  def props(logRepository: OperationLogRepository, random: SecureRandom, lotSize: Int): Props =
    Props(new PEOSignerActor(logRepository, random, lotSize))
}
