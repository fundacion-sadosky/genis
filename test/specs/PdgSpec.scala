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
      "db.default.url" -> "jdbc:postgresql://localhost:5432/pdgdb-test",
      "db.default.driver" -> "org.postgresql.Driver",
      "db.default.slickdriver" -> "pdgconf.ExtendedPostgresDriver",
      "db.default.user" -> "pdg",
      "db.default.password" -> "pdg"
    ) ++
    Helpers.inMemoryDatabase("logDb") ++
    Map(
      "ldap.default.url" -> "memserver:test/import.ldif",
      "ldap.default.adminDn" -> "uid=esurijon,ou=Users,dc=pdg,dc=org",
      "ldap.default.adminPassword" -> "sarasa",
      "mongodb.uri" -> "mongodb://localhost:27017/pdgdb-unit-test",
      "mongodb.connection.strictUri" -> "true")
    map
  }

  // Override app if you need a FakeApplication with other than default parameters.
  implicit override lazy val app: FakeApplication = FakeApplication(
    additionalConfiguration = configurations, withGlobal = Some(new Global()))

}

class Global extends GlobalSettings{

}