package pedigree

import akka.actor.{Actor, Props}
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
class MutationActor(mutationService: MutationService) extends Actor {

  private val logger = Logger(this.getClass)

  def receive = {
    case "refreshAllKis" => {
      Await.result(mutationService.refreshAllKis(),Duration(1800, SECONDS))
    }
  }

}
private object MutationActor {
  def props[T](mutationService: MutationService): Props = Props(new MutationActor(mutationService))
}
