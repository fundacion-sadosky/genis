package pedigree

import akka.actor.{Actor, Props}
import kits.AnalysisType
import matching.Spark2MatcherCollapsing
import pedigree.BayesianNetwork.Linkage
import play.api.Logger
import profile.Profile
import akka.actor._
import pedigree.SaveGenotypification
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
import pedigree.PedigreeGenotypificationService
import scala.util.Random
import akka.actor.ActorLogging
import play.api.i18n.Messages
import play.api.libs.ws.WSRequestHolder
import scala.concurrent.Future

class BayesianGenotypificationActor(pedigreeGenotypificationService: PedigreeGenotypificationService) extends Actor {

  private val logger = Logger(this.getClass)

  def receive = {
    case (saveGenotypification:SaveGenotypification)=> {
      val result = Await.result(pedigreeGenotypificationService.saveGenotypification(saveGenotypification.pedigree,
        saveGenotypification.profiles,
        saveGenotypification.frequencyTable,
        saveGenotypification.analysisType,
        saveGenotypification.linkage,
        saveGenotypification.mutationModel),Duration.Inf)
      sender ! result
    }
    case (calculateProbabilityScenarioPed:CalculateProbabilityScenarioPed)=> {
      val result = pedigreeGenotypificationService.calculateProbability(calculateProbabilityScenarioPed)
      sender ! result
    }
  }

}

private object BayesianGenotypificationActor {
  def props[T](pedigreeGenotypificationService: PedigreeGenotypificationService): Props = Props(new BayesianGenotypificationActor(pedigreeGenotypificationService))
}


