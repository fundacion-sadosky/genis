package configdata

import javax.inject.{Inject, Singleton}
import slick.jdbc.JdbcBackend.Database

import scala.util.Try

trait PostgresHealthService:
  def checkStatus(): Try[(String, String)]

@Singleton
class PostgresHealthServiceImpl @Inject()(db: Database) extends PostgresHealthService:
  override def checkStatus(): Try[(String, String)] =
    Try {
      val conn = db.source.createConnection()
      try
        val meta = conn.getMetaData
        (meta.getDatabaseProductName, meta.getDatabaseProductVersion)
      finally
        conn.close()
    }.map((product, version) => ("UP", s"$product $version"))
