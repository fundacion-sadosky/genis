package pedigree

import org.apache.pekko.actor.{Actor, Props}
import play.api.Logger

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class MutationActor(mutationService: MutationService) extends Actor:

  private val logger = Logger(this.getClass)

  def receive: Receive =
    case "refreshAllKis" =>
      Await.result(mutationService.refreshAllKis(), Duration(1800, SECONDS))

private object MutationActor:
  def props(mutationService: MutationService): Props =
    Props(new MutationActor(mutationService))
