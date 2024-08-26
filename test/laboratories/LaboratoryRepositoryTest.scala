package laboratories

import models.Tables
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import specs.PdgSpec
import stubs.Stubs
import types.Email
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.slick.lifted.TableQuery

class LaboratoryRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val labo = Stubs.laboratory

  val laboratories: TableQuery[Tables.Laboratory] = Tables.Laboratory

  private def queryDefineGetLaboratory(codeName: Column[String]) = Compiled(for (
    laboratory <- laboratories if laboratory.codeName === codeName
  ) yield laboratory)

  "A LabotatoryRepository" must {
    "list the laboratories" in {
      val repoLab = new SlickLaboratoryRepository

      val result = Await.result(repoLab.getAll, duration)

      result.size mustBe 5
    }

    "add a new laboratory" in {
      val repoLab = new SlickLaboratoryRepository

      val laboratory = Laboratory("Test", "Test", "AR", "C", "Test", "Test", Email("xx@yy.zz"), 0.0, 0.0)
      val result = Await.result(repoLab.add(laboratory), duration)

      result mustBe Right("Test")

      DB.withSession { implicit session =>
        queryDefineGetLaboratory("Test").delete
      }
    }

    "get a laboratory" in {
      val repoLab = new SlickLaboratoryRepository

      val result = Await.result(repoLab.get("SHDG"), duration)

      result.get.code mustBe "SHDG"
    }

    "update an existent laboratory" in {
      val repoLab = new SlickLaboratoryRepository
      val lab = Laboratory("Updated Name", "SHDG", "AR", "C", "Address", "1234", Email("mail@mail.com"), 0, 0)

      val result = Await.result(repoLab.update(lab), duration)

      result mustBe Right("SHDG")
    }

    "show an error updating an existent laboratory" in {
      val repoLab = new SlickLaboratoryRepository
      val lab = Laboratory("Updated Name", "SHDG", "ARGENTINA", "C", "Address", "1234", Email("mail@mail.com"), 0, 0)

      val result = Await.result(repoLab.update(lab), duration)
      result mustBe Left("ERROR: value too long for type character varying(2)")
    }
  }
}