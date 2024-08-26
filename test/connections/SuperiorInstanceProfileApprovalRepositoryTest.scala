package connections

import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SuperiorInstanceProfileApprovalRepositoryTest extends PdgSpec with MockitoSugar {
  lazy val repo: SuperiorInstanceProfileApprovalRepository = new SlickSuperiorInstanceProfileApprovalRepository();

  "SuperiorInstanceProfileApprovalTest" must {
    val approvalSearch = ProfileApprovalSearch(1,5)

    "upsert ok" in {

      val row = SuperiorInstanceProfileApproval(
        id = 0L,
        globalCode = "AR-C-SHDG-1101",
        profile = Json.toJson(Stubs.mixtureProfile).toString(),
        laboratory = "SHDG",
        laboratoryInstanceOrigin = "SHDG",
        laboratoryImmediateInstance = "SHDG",
        sampleEntryDate = None,
        errors = None,
        receptionDate = None)

      val resultInsert = Await.result(repo.upsert(row),Duration.Inf)

      resultInsert.isRight mustBe true

      val resultUpdate = Await.result(repo.upsert(row),Duration.Inf)

      resultUpdate.isRight mustBe true

      val resDelete = Await.result(repo.delete(row.globalCode),Duration.Inf)
      resDelete.isRight mustBe true

    }

    "findByGlobalCode ok" in {


      val row = SuperiorInstanceProfileApproval(
        id = 0L,
        globalCode = "AR-C-SHDG-1101",
        profile = Json.toJson(Stubs.mixtureProfile).toString(),
        laboratory = "SHDG",
        laboratoryInstanceOrigin = "SHDG",
        laboratoryImmediateInstance = "SHDG",
        sampleEntryDate = None,
        errors = None,
        receptionDate = None)

      val resultInsert = Await.result(repo.upsert(row),Duration.Inf)

      resultInsert.isRight mustBe true

      val resultFound = Await.result(repo.findByGlobalCode(row.globalCode),Duration.Inf)

      resultFound.isRight mustBe true
      resultFound.right.get.globalCode  mustBe row.globalCode


      val resDelete = Await.result(repo.delete(row.globalCode),Duration.Inf)
      resDelete.isRight mustBe true

    }

    "findAll ok" in {

      val row = SuperiorInstanceProfileApproval(
        id = 0L,
        globalCode = "AR-C-SHDG-1101",
        profile = Json.toJson(Stubs.mixtureProfile).toString(),
        laboratory = "SHDG",
        laboratoryInstanceOrigin = "SHDG",
        laboratoryImmediateInstance = "SHDG",
        sampleEntryDate = None,
        errors = None,
        receptionDate = None)

      val resultInsert = Await.result(repo.upsert(row),Duration.Inf)

      resultInsert.isRight mustBe true

      val resultFound = Await.result(repo.findAll(approvalSearch),Duration.Inf)

      resultFound.size mustBe 1

      val resDelete = Await.result(repo.delete(row.globalCode),Duration.Inf)
      resDelete.isRight mustBe true
    }


  }

}
