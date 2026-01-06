package util

import scala.concurrent.Future
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import play.api.Play.current
import play.api.db.slick.DBAction

trait Transaction extends ContextDb {
 
  def runInTransactionAsync[T](f: Session => T): Future[T] = Future {
    DB.withTransaction { implicit session => f(session) }
  }
  
  def runInTransaction[T](f: Session => T): T = {
    DB.withTransaction { implicit session => f(session) }
  }
}