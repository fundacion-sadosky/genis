package fixtures

import org.scalatest.{BeforeAndAfterAll, Suite}
import slick.jdbc.PostgresProfile.api._

trait PostgresSpec extends BeforeAndAfterAll { self: Suite =>

  protected val db: Database = Database.forURL(
    url = "jdbc:postgresql://localhost:5432/genisdb",
    user = "genissqladmin",
    password = "genissqladminp",
    driver = "org.postgresql.Driver"
  )

  override protected def afterAll(): Unit =
    db.close()
    super.afterAll()
}
