package profile

import com.mongodb.client.{MongoClient, MongoClients, MongoDatabase}
import play.api.{Configuration, Logger}

class MongoClientFactory(conf: Configuration):

  private val logger = Logger(this.getClass)

  private val mongoUri = conf.get[String]("uri")
  private val dbName = conf.get[String]("database")

  def createClient(): MongoClient =
    logger.info(s"Creating MongoClient for database '$dbName'")
    MongoClients.create(mongoUri)

  def createDatabase(client: MongoClient): MongoDatabase =
    client.getDatabase(dbName)
