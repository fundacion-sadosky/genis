package fixtures

import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.jdbc.PostgresProfile.api.*
import slick.jdbc.JdbcBackend

/** Connects to the LOG_DB database (genislogdb) used by audit tests.
 *  Mirrors [[PostgresSpec]] but points at the separate audit database
 *  defined in application.conf -> slick.dbs.logDb. */
trait LogDbSpec extends BeforeAndAfterAll { self: Suite =>

  protected val logDb: JdbcBackend.Database = JdbcBackend.Database.forURL(
    url = "jdbc:postgresql://localhost:5432/genislogdb",
    user = "genissqladmin",
    password = "genissqladminp",
    driver = "org.postgresql.Driver",
    executor = AsyncExecutor("logdb-test", numThreads = 2, queueSize = 100)
  )

  override protected def afterAll(): Unit =
    logDb.close()
    super.afterAll()
}
