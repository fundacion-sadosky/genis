package util

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

import play.api.Play.current
import play.api.db.slick.DBAction
import play.api.libs.concurrent.Akka

trait ContextDb {
  
  implicit val slickExecutionContext: ExecutionContext
  
  implicit def date2date(date: java.util.Date) = new java.sql.Date(date.getTime)
  
}

trait DefaultDb extends ContextDb {
  
  override implicit val slickExecutionContext: ExecutionContext = DBAction.attributes(DBAction.defaultName).executionContext

}

trait LogDb extends ContextDb {
  
  override implicit val slickExecutionContext: ExecutionContext = Akka.system.dispatchers.lookup("play.akka.actor.log-context")
  
}