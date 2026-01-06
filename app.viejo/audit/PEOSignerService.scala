package audit

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import scala.reflect.ClassTag
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.dispatch.BoundedMessageQueueSemantics
import akka.dispatch.RequiresMessageQueue
import akka.pattern.ask
import akka.util.Timeout
import play.api.Logger
import java.security.SecureRandom
import scala.util.Random
import akka.actor.ActorLogging

abstract class PEOSignerService {

  def addEntry[E <: Serializable, S <: Signature, R](entry: PEOEntry[E, S, R])(implicit ct: ClassTag[R]): Future[R]

}

class AkkaPEOSignerService(
    akkaSystem: ActorSystem,
    logReposiotry: OperationLogRepository,
    randomAlg: String,
    lotSize: Int) extends PEOSignerService {

  val logger = Logger(this.getClass)

  val peoActor: ActorRef = akkaSystem.actorOf(PEOSignerActor.props(
    logReposiotry,
    SecureRandom.getInstance(randomAlg),
    lotSize))

  override def addEntry[E <: Serializable, S <: Signature, R](entry: PEOEntry[E, S, R])(implicit ct: ClassTag[R]): Future[R] = {
    implicit val timeout = Timeout(Duration(10, SECONDS))
    (peoActor ? entry).mapTo[R]
  }

}

case class PEOEntry[E <: Serializable, S <: Signature, R](obj: E, build: (E, Key, Long) => S, persist: S => Future[R])

class PEOSignerActor(logRepository: OperationLogRepository, random: Random, lotSize: Int) extends Actor with ActorLogging with RequiresMessageQueue[BoundedMessageQueueSemantics] {

  import context.dispatcher

  val duration = Duration(100, SECONDS)

  def generateNewLot() = {
    val kZero = Key(random)
    val lotId = Await.result(logRepository.createLot(kZero), duration)
    (kZero, lotId)
  }

  def logLotBehavior(count: Int, lotId: Long, prevKey: Key): Actor.Receive = {
    case PEOEntry(entry, build, persist) =>
      val signature = build(entry, prevKey, lotId)
      val promise = persist(signature)
      val result = Await.result(promise, duration)
      sender() ! result
      //      import akka.pattern.pipe
      //      promise pipeTo sender()
      log.debug(s"signing entry #$count (${signature.stringify}, $prevKey) -> ${signature.signature}")
      if (count >= lotSize -1) {
        val (kZero, lotId) = generateNewLot()
        context.become(logLotBehavior(0, lotId, kZero))
      } else {
        context.become(logLotBehavior(count + 1, lotId, signature.signature))
      }
  }

  val (kZero, lotId) = generateNewLot()
  def receive = logLotBehavior(0, lotId, kZero)

}

object PEOSignerActor {
  def props[T](logRepository: OperationLogRepository, random: Random, lotSize: Int): Props = Props(new PEOSignerActor(logRepository, random, lotSize))
}

