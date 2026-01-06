package specs

import org.scalatestplus.play.OneAppPerSuite
import org.scalatestplus.play.PlaySpec
import play.api.GlobalSettings
import play.api.test.FakeApplication
import play.api.test.Helpers

class PdgSpec extends PlaySpec with OneAppPerSuite {

  val configurations: Map[String, String] = {
    //Helpers.inMemoryDatabase(options = Map("MODE"->"PostgreSQL")) ++
    val map = Map(
      "db.default.url" -> "jdbc:postgresql://genis_postgres:5432/genisdb",
      "db.default.driver" -> "org.postgresql.Driver",
      "db.default.slickdriver" -> "pdgconf.ExtendedPostgresDriver",
      "db.default.user" -> "genissqladmin",
      "db.default.password" -> "genissqladminp"
    ) ++
    Helpers.inMemoryDatabase("logDb") ++
    Map(
      "ldap.default.url" -> "genis_ldap",
      "ldap.default.adminDn" -> "cn=admin,dc=genis,dc=local",
      "ldap.default.adminPassword" -> "adminp",
      "mongodb.uri" -> "mongodb://genis_mongo:27017/pdgdb",
      "mongodb.connection.strictUri" -> "true")
    map
  }

  // Override app if you need a FakeApplication with other than default parameters.
  implicit override lazy val app: FakeApplication = FakeApplication(
    additionalConfiguration = configurations, withGlobal = Some(new Global()))

}

class Global extends GlobalSettings{

}