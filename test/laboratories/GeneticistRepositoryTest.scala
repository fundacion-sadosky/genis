package laboratories

import models.Tables
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import specs.PdgSpec
import stubs.Stubs
import types.Email
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import slick.lifted.TableQuery

class GeneticistRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val geneticist = Stubs.geneticist

  val geneticists: TableQuery[Tables.Geneticist] = Tables.Geneticist

  private def queryDefineGetGeneticist(name: Column[String], lastName: Column[String]) = Compiled(for (
    geneticist <- geneticists if geneticist.name === name && geneticist.lastname === lastName
  ) yield geneticist)

  "A GeneticistRepository" must {
    "add a new geneticist to a Laboratory" in {
      
      val repoGen = new SlickGeneticistRepository
      val geneticist = Geneticist("Test", "SHDG", "Test", Email("xx@yy.zz"), "12345678", Some(9l))
      val result = Await.result(repoGen.add(geneticist), duration)
      
      result mustBe 1

      DB.withSession { implicit session =>
        queryDefineGetGeneticist("Test", "Test").delete
      }
    }
    
    "list all geneticist from a laboratory" in {
      
      val repoGen = new SlickGeneticistRepository
      
      val result = Await.result(repoGen.getAll("SHDG"), duration)
      
      result.size mustBe 3
    }
    
    "get a geneticist by Id" in {
      
      val repoGen = new SlickGeneticistRepository
      val result = Await.result(repoGen.get(7), duration)
      
      result.get.name mustBe "Ana"
      result.get.lastname mustBe "Porres"
      result.get.email.text mustBe "aporres@example.com"
    }
    
    "update a geneticist" in {
      val repoGen = new SlickGeneticistRepository
      val newGeneticist = Geneticist("Jay","SHDG","Last",Email("mail@example.com"),"123",Some(6))
      val result = Await.result(repoGen.update(newGeneticist), duration)
      
      result mustBe 1
    }

    "update a geneticist laboratory" in {
      val repoGen = new SlickGeneticistRepository
      val newGeneticist = Geneticist("Jay","SECE","Last",Email("mail@example.com"),"123",Some(6))

      val updateResult = Await.result(repoGen.update(newGeneticist), duration)
      val labResult = Await.result(repoGen.getAll("SECE"), duration)

      updateResult mustBe 1
      labResult.contains(newGeneticist) mustBe true
    }
  }
}