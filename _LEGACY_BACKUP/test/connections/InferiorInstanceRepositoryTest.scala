package connections

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class InferiorInstanceRepositoryTest extends PdgSpec with MockitoSugar {

  lazy val repo: InferiorInstanceRepository = new SlickInferiorInstanceRepository();
  val duration = Duration(10, SECONDS)

  "InferiorInstanceRepositoryTest" must {

    "insert inferior instance ok" in {
      val res = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true
      Await.result(repo.deleteById(res.right.get), duration)
    }

    "fail on insert duplicate" in {
      val res = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true
      val count = Await.result(repo.countByURL("pdg-uat"), duration)
      count mustBe 1

      val res2 = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true

      Await.result(repo.deleteById(res.right.get), duration)
    }

    "gel all ok" in {
      val res = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true
      val res2 = Await.result(repo.findAll(), duration)
      res2.isRight mustBe true
      res2.right.get.size mustBe 1
      Await.result(repo.deleteById(res.right.get), duration)
    }
    "update ok" in {
      val res = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true
      val res2 = Await.result(repo.findAll(), duration)
      res2.isRight mustBe true

      res2.right.get.exists(x => x.idStatus == 2) mustBe false
      val modifiedList =  res2.right.get.map(x => x.copy(idStatus = 2))

      Await.result(repo.update(modifiedList.head), duration)
      val res4 = Await.result(repo.findAll(), duration)
      res4.isRight mustBe true
      res4.right.get.exists(x => x.idStatus == 2) mustBe true

      Await.result(repo.deleteById(res.right.get), duration)
    }
    "findAllInstanceStatus ok" in {

      val res = Await.result(repo.findAllInstanceStatus(), duration)
      res.isRight mustBe true
    }
    "isInferiorInstanceEnabled ok" in {
      var enabled = Await.result(repo.isInferiorInstanceEnabled("pdg-uat"), duration)
      enabled mustBe false

      val res = Await.result(repo.insert(InferiorInstance(url="pdg-uat")), duration)
      res.isRight mustBe true
      val res2 = Await.result(repo.findAll(), duration)
      res2.isRight mustBe true

      Await.result(repo.update(res2.right.get.map(x => x.copy(idStatus = 1)).head), duration)
      enabled = Await.result(repo.isInferiorInstanceEnabled("pdg-uat"), duration)
      enabled mustBe false

      Await.result(repo.update(res2.right.get.map(x => x.copy(idStatus = 3)).head), duration)
      enabled = Await.result(repo.isInferiorInstanceEnabled("pdg-uat"), duration)
      enabled mustBe false

      Await.result(repo.update(res2.right.get.map(x => x.copy(idStatus = 2)).head), duration)
      enabled = Await.result(repo.isInferiorInstanceEnabled("pdg-uat"), duration)
      enabled mustBe true

      enabled = Await.result(repo.isInferiorInstanceEnabled("pdg-uat"), duration)
      enabled mustBe true

      val res4 = Await.result(repo.findAll(), duration)
      res4.isRight mustBe true
      res4.right.get.exists(x => x.idStatus == 2) mustBe true

      Await.result(repo.deleteById(res.right.get), duration)
    }

    "findByURL ok" in {
      val url = "pdg-uat"
      val res = Await.result(repo.insert(InferiorInstance(url=url)), duration)
      res.isRight mustBe true

      val res2 = Await.result(repo.findByURL(url), duration)
      res2.get.url mustBe url

      Await.result(repo.deleteById(res.right.get), duration)
    }

    "findByLabCode ok" in {
      val url = "pdg-uat"
      val lab = "SHDG"
      val res = Await.result(repo.insert(InferiorInstance(url=url,laboratory = lab)), duration)
      res.isRight mustBe true

      val res2 = Await.result(repo.findByLabCode(lab), duration)
      res2.get.laboratory mustBe lab

      Await.result(repo.deleteById(res.right.get), duration)
    }

  }

}
