package fixtures

import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.jdbc.PostgresProfile.api._

trait PostgresSpec extends BeforeAndAfterAll { self: Suite =>

  protected val db: Database = Database.forURL(
    url = "jdbc:postgresql://localhost:5432/genisdb",
    user = "genissqladmin",
    password = "genissqladminp",
    driver = "org.postgresql.Driver",
    executor = AsyncExecutor("test", numThreads = 2, queueSize = 100)
  )

  override protected def afterAll(): Unit =
    db.close()
    super.afterAll()
}
