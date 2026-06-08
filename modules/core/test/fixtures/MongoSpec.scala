package fixtures

import com.mongodb.client.{MongoClient, MongoClients, MongoDatabase}
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
 * Integration test fixture for MongoDB.
 *
 * Connects to a local Docker Mongo (utils/docker/docker-compose.yml, port 27017)
 * using a dedicated test database name to avoid polluting `pdgdb` (the dev DB).
 *
 * Each suite is responsible for cleaning its own collections in beforeEach/afterEach.
 */
trait MongoSpec extends BeforeAndAfterAll { self: Suite =>

  private val mongoUri = sys.env.getOrElse("MONGODB_TEST_URI", "mongodb://localhost:27017")
  protected val testDbName: String = "genis_test"

  protected val mongoClient: MongoClient = MongoClients.create(mongoUri)
  protected val mongoDb: MongoDatabase   = mongoClient.getDatabase(testDbName)

  override protected def afterAll(): Unit =
    try mongoClient.close() finally super.afterAll()
}
