package pedigrees

import models.Tables
import org.scalatest.mock.MockitoSugar
import pedigree._
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration._
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB


class PedigreeDataRepositoryTest extends PdgSpec with MockitoSugar {

  val courtCases: TableQuery[Tables.CourtCase] = Tables.CourtCase
  val courtCasesFiliationData: TableQuery[Tables.CourtCaseFiliationData] = Tables.CourtCaseFiliationData

  private def queryDefineGetCourtCase(courtCaseId: Column[Long]) = Compiled(for (
    courtCase <- courtCases if courtCase.id === courtCaseId
  ) yield courtCase)

  private def queryDefineGetCourtCaseFiliationData(courtCaseId: Column[Long]) = Compiled(for (
    courtCaseFD <- courtCasesFiliationData if courtCaseFD.courtCaseId === courtCaseId
  ) yield courtCaseFD)

  val duration = Duration(10, SECONDS)

  "A PedigreeDataRepository" must {

    "get all court cases with default values" in {
      val repository = new SlickPedigreeDataRepository()
      val id = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest", None, None, "asignee", None, None, None, "MPI")), duration).right.get

      val result: Seq[CourtCaseModelView] = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "asignee", false)), duration)

      result.size mustBe 1

      result(0).assignee mustBe "asignee"
      result(0).status mustBe PedigreeStatus.withName("Open")
      result(0).internalSampleCode mustBe "PedigreeTest"
      result(0).id mustBe id

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(id).delete
        queryDefineGetCourtCase(id).delete
      }
    }

    "get all court cases with case Type" in {
      val repository = new SlickPedigreeDataRepository()
      val idMPI = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTestMPI", None, None, "asignee", None, None, None, "MPI")), duration).right.get
      val idDVI = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTestDVI", None, None, "asignee", None, None, None, "DVI")), duration).right.get

      val result: Seq[CourtCaseModelView] = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "asignee", false, caseType = Some("DVI"))), duration)

      result.size mustBe 1

      result(0).assignee mustBe "asignee"
      result(0).status mustBe PedigreeStatus.withName("Open")
      result(0).internalSampleCode mustBe "PedigreeTestDVI"
      result(0).id mustBe idDVI

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(idDVI).delete
        queryDefineGetCourtCase(idDVI).delete

        queryDefineGetCourtCaseFiliationData(idMPI).delete
        queryDefineGetCourtCase(idMPI).delete
      }
    }

    "get total court cases" in {
      val repository = new SlickPedigreeDataRepository()
      val id1 = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest1", None, None, "asignee", None, None, None, "MPI")), duration).right.get
      val id2 = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest2", None, None, "asignee", None, None, None, "MPI")), duration).right.get

      val result: Int = Await.result(repository.getTotalCourtCases(None, PedigreeSearch(0, 30, "asignee", false)), duration)

      result mustBe 2

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(id1).delete
        queryDefineGetCourtCaseFiliationData(id2).delete
        queryDefineGetCourtCase(id1).delete
        queryDefineGetCourtCase(id2).delete
      }
    }

    "create a court case with default values" in {
      val repository = new SlickPedigreeDataRepository()
      val id = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest", None, None, "asignee", None, None, None, "MPI")), duration).right.get

      val result: Seq[CourtCaseModelView] = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "asignee", false)), duration)

      result.size mustBe 1
      result(0).id mustBe id

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(id).delete
        queryDefineGetCourtCase(id).delete
      }
    }

    "get court case by id" in {
      val repository = new SlickPedigreeDataRepository()
      val id = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest", None, None, "asignee", None, None, None, "MPI")), duration).right.get

      val result: Option[CourtCase] = Await.result(repository.getCourtCase(id), duration)

      result.isDefined mustBe true

      result.get.assignee mustBe "asignee"
      result.get.status mustBe PedigreeStatus.withName("Open")
      result.get.internalSampleCode mustBe "PedigreeTest"
      result.get.id mustBe id

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(id).delete
        queryDefineGetCourtCase(id).delete
      }
    }

    "change pedigree status" in {
      val repository = new SlickPedigreeDataRepository()
      val id = Await.result(repository.createCourtCase(CourtCaseAttempt("PedigreeTest", None, None, "asignee", None, None, None, "MPI")), duration).right.get

      val result = Await.result(repository.changeCourCaseStatus(id, PedigreeStatus.Active), duration)

      val courtCaseOpt = Await.result(repository.getCourtCase(id), duration)

      result mustBe Right(id)
      courtCaseOpt.isDefined mustBe true
      courtCaseOpt.get.status mustBe PedigreeStatus.Active

      DB.withSession { implicit session =>
        queryDefineGetCourtCaseFiliationData(id).delete
        queryDefineGetCourtCase(id).delete }
    }

    def previousSettings(repository: SlickPedigreeDataRepository) = {
      val id1 = Await.result(repository.createCourtCase(CourtCaseAttempt("Pedigree1", None, None, "assignee", None, None, None, "MPI")), duration).right.get
      val id2 = Await.result(repository.createCourtCase(CourtCaseAttempt("Pedigree2", None, None, "pdg", None, None, None, "MPI")), duration).right.get
      val id3 = Await.result(repository.createCourtCase(CourtCaseAttempt("Pedigree3", None, None, "pdg", None, None, None, "MPI")), duration).right.get
      Await.result(repository.changeCourCaseStatus(id3, PedigreeStatus.Closed), duration)
      (id1, id2, id3)
    }

    def afterSettings(ids: (Long, Long, Long), repository: SlickPedigreeDataRepository) =  {
      DB.withSession { implicit session =>
        ids match {
          case (id1, id2, id3) => {
            queryDefineGetCourtCaseFiliationData(id1).delete
            queryDefineGetCourtCaseFiliationData(id2).delete
            queryDefineGetCourtCaseFiliationData(id3).delete
            queryDefineGetCourtCase(id1).delete
            queryDefineGetCourtCase(id2).delete
            queryDefineGetCourtCase(id3).delete
          }
        }
      }
    }

    "count search results" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val qty = Await.result(repository.getTotalCourtCases(None, PedigreeSearch(0, 30, "pdg", false)), duration)

      qty mustBe 2

      afterSettings(ids, repository)
    }

    "filter by user" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val ccs = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "assignee", false)), duration)

      ccs.size mustBe 1

      afterSettings(ids, repository)
    }

    "filter by ids" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val ccs = Await.result(repository.getAllCourtCases(Option(Seq(ids._2)), PedigreeSearch(0, 30, "pdg", false)), duration)

      ccs.size mustBe 1

      afterSettings(ids, repository)
    }

    "filter by internal sample code case insensitive contains" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val results = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, Some("PEDIGREE"))), duration)
      val noResults = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, Some("no_existente"))), duration)

      results.size mustBe 2
      noResults.size mustBe 0

      afterSettings(ids, repository)
    }

    "filter by status" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val open = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, None, None, Some(PedigreeStatus.Open))), duration)
      val closed = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, None, None, Some(PedigreeStatus.Closed))), duration)

      open.size mustBe 1
      closed.size mustBe 1

      afterSettings(ids, repository)
    }

    "sort by internal sample code" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val ascending = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, None, None, None, Some("code"), Some(true))), duration)
      val descending = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "pdg", false, None, None, None, Some("code"), Some(false))), duration)

      ascending(0).internalSampleCode.compareTo(ascending(1).internalSampleCode) mustBe <(0)
      descending(0).internalSampleCode.compareTo(descending(1).internalSampleCode) mustBe >(0)

      afterSettings(ids, repository)
    }

    "paginate search results" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val ccs = Await.result(repository.getAllCourtCases(None, PedigreeSearch(1, 1, "pdg", false)), duration)

      ccs.size mustBe 1

      afterSettings(ids, repository)
    }

    "get all when superuser" in {
      val repository = new SlickPedigreeDataRepository
      val ids = previousSettings(repository)

      val ccs = Await.result(repository.getAllCourtCases(None, PedigreeSearch(0, 30, "assignee", true)), duration)

      ccs.size mustBe 3

      afterSettings(ids, repository)
    }
  }
}
