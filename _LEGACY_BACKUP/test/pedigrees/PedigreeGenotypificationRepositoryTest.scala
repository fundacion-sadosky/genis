package pedigrees

import pedigree._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json._
import specs.PdgSpec
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

class PedigreeGenotypificationRepositoryTest extends PdgSpec {

    val duration = Duration(10, SECONDS)
    val pedigreeGenotypification = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("pedigreeGenotypification")), Duration(10, SECONDS))

    "A PedigreeGenotypification repository" must {
      "add new genotypification and return a right result" in {
        val heterocygoteFounders = Map("LOCUS" -> 1)

        val header: Array[String] = Array("Padre_LOCUS_m", "Padre_LOCUS_p", "Hijo_LOCUS_p_s", "Hijo_LOCUS_p", "Probability")
        val matrix: Array[Array[Double]] = Array(
          Array(8.0, 9.0, 1.0, 10.0, 1.0),
          Array(8.0, 9.0, 1.0, 11.0, 1.0))

//        val genotypification = Array(PlainCPT(header, matrix.iterator, matrix.length))
        val genotypification = Array(PlainCPT2(header, matrix))

        val repository = new MongoPedigreeGenotypificationRepository

        val result = Await.result(repository.upsertGenotypification(PedigreeGenotypification(85, genotypification, 0.8, "base freq", Array("PI"))), duration)
        result.isRight mustBe true
        result.right.get mustBe 85

        val pedigree = Await.result(pedigreeGenotypification.find(Json.obj()).one[PedigreeGenotypification], duration)
        pedigree.isDefined mustBe true
        pedigree.get._id mustBe 85
        pedigree.get.boundary mustBe 0.8
        pedigree.get.genotypification.head.matrix.size mustBe 2
        pedigree.get.frequencyTable mustBe "base freq"

        Await.result(pedigreeGenotypification.drop(false), duration)
      }

      "update a genotypification and return a right result" in {
        val heterocygoteFounders = Map("LOCUS" -> 1)

        val header: Array[String] = Array("Padre_LOCUS_m", "Padre_LOCUS_p", "Hijo_LOCUS_p_s", "Hijo_LOCUS_p", "Probability")
        val matrix: Array[Array[Double]] = Array(
          Array(8.0, 9.0, 1.0, 10.0, 1.0),
          Array(8.0, 9.0, 1.0, 11.0, 1.0))

//        var genotypification = Array(PlainCPT(header, matrix.iterator, matrix.length))
        var genotypification = Array(PlainCPT2(header, matrix))

        val existentPedigree = PedigreeGenotypification(345, genotypification, 0.7, "base freq", Array("PI"))
        Await.result(pedigreeGenotypification.insert(existentPedigree), duration)

        val repository = new MongoPedigreeGenotypificationRepository

//        genotypification = Array(PlainCPT(header, matrix.iterator, matrix.length))
        val updatedPedigree = PedigreeGenotypification(345, genotypification, 0.6, "base freq", Array("PI"))
        val result = Await.result(repository.upsertGenotypification(updatedPedigree), duration)
        result.isRight mustBe true
        result.right.get mustBe 345

        val pedigree = Await.result(pedigreeGenotypification.find(Json.obj()).one[PedigreeGenotypification], duration)
        pedigree.isDefined mustBe true
        pedigree.get._id mustBe 345
        pedigree.get.boundary mustBe 0.6
        pedigree.get.genotypification.head.matrix.size mustBe 2
        pedigree.get.frequencyTable mustBe "base freq"

        Await.result(pedigreeGenotypification.drop(false), duration)
      }
    }
}
