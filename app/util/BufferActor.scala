package util

import scala.concurrent.Promise
import scala.util.Try
import akka.actor.Actor
import akka.actor.Props
import akka.actor.Terminated
import scala.collection.immutable.Queue

class BufferActor[T](promise: Promise[Seq[T]]) extends Actor {

  var buffer = Queue[T]()

  def receive = {
    case Terminated => {
      promise complete Try { buffer }
      context.stop(self)
    }
    case element: T => {
      buffer = buffer.enqueue(element)
      ()
    }
  }

}

object BufferActor {
  def props[T](promise: Promise[Seq[T]]): Props = Props(new BufferActor[T](promise))
}
