package profile

import com.mongodb.client.{MongoClient, MongoDatabase}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.Future

@Singleton
class MongoDatabaseProvider @Inject()(
  conf: Configuration,
  lifecycle: ApplicationLifecycle
) extends Provider[MongoDatabase]:

  private val factory = MongoClientFactory(conf.get[Configuration]("mongodb"))
  private lazy val client: MongoClient = factory.createClient()
  private lazy val database: MongoDatabase = factory.createDatabase(client)

  lifecycle.addStopHook(() => Future.successful(client.close()))

  override def get(): MongoDatabase = database
