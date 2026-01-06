package connections
import akka.actor._

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
import play.api.i18n.Messages
import play.api.libs.ws.WSRequestHolder
class SendRequestActor() extends Actor  {
  private val logger = Logger(this.getClass)

  def receive = {
    case (holder:WSRequestHolder,body:String,stop:Boolean,timeOut:String) if holder.method == "GET" => {
      try{
        val result = Await.result(holder.get(),Duration(timeOut))
        sender ! result
      }catch {
        case e: java.net.ConnectException => {
          logger.debug(e.getMessage)
        }
        case e: java.util.concurrent.TimeoutException => {
          logger.debug(e.getMessage)
        }
        case e: Exception => {
          logger.error(e.getMessage,e)
        }
      }finally {
        if(stop){
          context stop self
        }
      }
    }
    case (holder:WSRequestHolder,body:String,stop:Boolean,timeOut:String) if holder.method == "POST" => {
      try{
        val result = Await.result(holder.post(body),Duration(timeOut))
        sender ! result
      }catch {
        case e: java.net.ConnectException => {
          logger.debug(e.getMessage)
        }
        case e: java.util.concurrent.TimeoutException => {
          logger.debug(e.getMessage)
        }
        case e: Exception => {
          logger.error(e.getMessage,e)
        }
      }finally {
        if(stop){
          context stop self
        }
      }
    }
    case (holder:WSRequestHolder,body:String,stop:Boolean,timeOut:String) if holder.method == "PUT" => {
      try{
        val result = Await.result(holder.put(body),Duration(timeOut))
        sender ! result
      }catch {
        case e: java.net.ConnectException => {
          logger.debug(e.getMessage)
        }
        case e: java.util.concurrent.TimeoutException => {
          logger.debug(e.getMessage)
        }
        case e: Exception => {
          logger.error(e.getMessage,e)
        }
      }finally {
        if(stop){
          context stop self
        }
      }
    }
    case (holder:WSRequestHolder,body:String,stop:Boolean,timeOut:String) if holder.method == "DELETE" => {
      try{
        val result = Await.result(holder.delete(),Duration(timeOut))
        sender ! result
      }catch {
        case e: java.net.ConnectException => {
          logger.debug(e.getMessage)
        }
        case e: java.util.concurrent.TimeoutException => {
          logger.debug(e.getMessage)
        }
        case e: Exception => {
          logger.error(e.getMessage,e)
        }
      }finally {
        if(stop){
          context stop self
        }
      }
    }
    case _ => {
      throw new IllegalArgumentException();
    }
  }
}
object SendRequestActor {
  def props[T](): Props = Props(new SendRequestActor())
}
