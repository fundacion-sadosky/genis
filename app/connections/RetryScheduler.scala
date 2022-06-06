package connections

import javax.inject.{Inject, Named}

import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorRef, ActorSystem}
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration._

class RetryScheduler @Inject()(actorSystem: ActorSystem,interconnectionService:InterconnectionService,@Named("retryInterval") val retryInterval: String){

  actorSystem.scheduler.schedule(initialDelay = 20.seconds, interval = Some(Duration(retryInterval)).collect { case d: FiniteDuration => d }.get) {

    Future{
      interconnectionService.retry()
    }

    ()
  }
}
