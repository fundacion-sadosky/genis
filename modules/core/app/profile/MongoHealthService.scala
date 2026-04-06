package profile

import com.mongodb.client.MongoDatabase
import javax.inject.{Inject, Singleton}

import scala.util.Try

trait MongoHealthService:
  def checkStatus(): Try[(String, String)]

@Singleton
class MongoHealthServiceImpl @Inject()(database: MongoDatabase) extends MongoHealthService:
  override def checkStatus(): Try[(String, String)] =
    Try(database.listCollectionNames().first()).map(_ => ("UP", database.getName))
