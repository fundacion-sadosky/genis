package motive

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import scala.language.postfixOps
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class MotiveRepositoryTest extends PdgSpec with MockitoSugar{

  lazy val repo: MotiveRepository = new SlickMotiveRepository()
  val duration = Duration(10, SECONDS)

  "MotiveRepositoryTest" must {

    "get motive types ok" in {
      val result1 = Await.result(repo.getMotivesTypes(), duration)
      result1.isEmpty mustBe false
    }

    "get all motives" in {
      val motives = Await.result(repo.getMotivesTypes(), duration)
      val motiveType:MotiveType = motives.head
      val result1 = Await.result(repo.getMotives(motiveType.id,false), duration)
      result1.isEmpty mustBe false
    }
    "get motive" in {
      val motives = Await.result(repo.getMotivesTypes(), duration)
      val motiveType:MotiveType = motives.head
      val result1 = Await.result(repo.getMotives(motiveType.id,true), duration)
      result1.isEmpty mustBe true
      val idMotive = 987
      val desMotive = "Otros Test"
      val desMotiveUpdate = "Otros Test2"

      val result2 = Await.result(repo.insert(Motive(idMotive,motiveType.id,desMotive,false)), duration)

      val result3 = Await.result(repo.getMotives(motiveType.id,true), duration)
      result3.filter(x => x.description == desMotive).isEmpty mustBe false

      val resultupdate = Await.result(repo.update(result3.filter(x => x.description == desMotive).head.copy(description = desMotiveUpdate)), duration)

      val resultget = Await.result(repo.getMotives(motiveType.id,true), duration)
      resultget.filter(x => x.description == desMotive).isEmpty mustBe true
      resultget.filter(x => x.description == desMotiveUpdate).isEmpty mustBe false

      val deleteLogical = Await.result(repo.deleteLogicalMotiveById(result3.filter(x => x.description == desMotive).head.id), duration)

      val result4 = Await.result(repo.deleteMotiveById(result3.filter(x => x.description == desMotive).head.id), duration)

      val result5 = Await.result(repo.getMotives(motiveType.id,true), duration)
      result5.filter(x => x.description == desMotiveUpdate).isEmpty mustBe true

    }

  }

}

