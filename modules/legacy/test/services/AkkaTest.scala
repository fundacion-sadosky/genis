package services

import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import specs.PdgSpec
import akka.pattern._
import akka.actor._
import scala.collection.mutable.ListBuffer
import play.api.libs.concurrent.Akka
import scala.concurrent.Promise
import scala.util.Try
import scala.concurrent.Await
import play.api.Logger
import scala.collection.mutable.MutableList
import scala.reflect.ClassTag
import scala.util.Random

class AkkaTest extends PdgSpec {

  val logger = Logger(this.getClass())

  class SimpleMutableList[T](n: Int)(implicit ct: ClassTag[T]) {

    val delay = 50

    val buffer = new Array[T](n)
    var curr = -1

    def add(e: T) = {
      val next = curr + 1
      Thread.sleep(delay)
      buffer(next) = e
      curr = next
    }

    def toSeq = buffer.toSeq.filter { _ != null }
    
  }

  class BufferActor(promise: Promise[Seq[String]]) extends Actor {

    val buffer = new SimpleMutableList[String](10)

    def receive = {
      case Terminated => {
        logger.debug("TERMINATE Message reveiced")
        promise complete Try { buffer.toSeq }
        context.stop(self)
      }
      case msg: String => {
        logger.debug(s"Procesing message: $msg")
        buffer.add(msg)
        ()
      }
    }

    def flush(): Seq[String] = buffer.toSeq

  }

  object BufferActor {
    def props(promise: Promise[Seq[String]]): Props = Props(new BufferActor(promise))
  }

  val duration = Duration(10, SECONDS)

  val n = 10

  val messages = (for (i <- 0 until n) yield {
    s"msg $i"
  }).toSet

  "A mutable list" must {
    "store values properly in no concurrent scenario " in {

      val list = new SimpleMutableList[String](n)

      messages.foreach { list.add(_) }

      list.toSeq.toSet mustBe messages

    }

  }

  "A mutable list" must {
    "override values in concurrent scenario with forced race conditions" in {

      val list = new SimpleMutableList[String](n)

      messages.foreach { msg =>
        val t = new Thread {
          override def run() = {
            logger.debug(s"add message $msg from thread ${this.getName}")
            list.add(msg)
          }
        }
        t.start()
        t.join(1000)
      }
      
      list.toSeq.size mustBe 10
    }
  }

  "actor system" must {
    "process concurrent messages one after each other" in {

      val promise = Promise[Seq[String]]

      val bufferActor = Akka.system.actorOf(BufferActor.props(promise))
      
      val rnd = Random

      messages.foreach { msg =>
        val t = new Thread {
          override def run() = {
            Thread.sleep(rnd.nextInt(10)*10)
            logger.debug(s"send message: $msg from thread ${this.getName}")
            bufferActor ! msg
          }
        }
        t.start()
      }

      Thread.sleep(3000)
      bufferActor ! Terminated

      val result = Await.result(promise.future, duration)

      result.toSet mustBe messages

    }

  }

}
