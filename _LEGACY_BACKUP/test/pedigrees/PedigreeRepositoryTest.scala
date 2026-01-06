package pedigrees

import pedigree._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import specs.PdgSpec
import types.{SampleCode, Sex}

import scala.concurrent.Await
import scala.concurrent.duration._
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor

class PedigreeRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val pedigrees = Await.result(new reactivemongo.api.MongoDriver().connection("localhost:27017").get.database("pdgdb-unit-test").map(_.collection[JSONCollection]("pedigrees")), Duration(10, SECONDS))
  val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None))
  val id = 555

  "A Pedigree repository" must {
    "get unprocessed " in {
      val newIndividuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, None, true, None))
      Await.result(pedigrees.insert(PedigreeGenogram(54, "user", newIndividuals, PedigreeStatus.Active, None,false,0.5,false,None,"MPI",None,7l)), duration)

      val repository = new MongoPedigreeRepository

      val result = Await.result(repository.getUnprocessed(), duration)

      result.size mustBe 1
      result(0) mustBe 54

      Await.result(pedigrees.drop(false), duration)
    }

    "get unprocessed status underconstruction - retrieves zero results " in {
      val newIndividuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, None, true, None))
      Await.result(pedigrees.insert(PedigreeGenogram(54, "user", newIndividuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)), duration)

      val repository = new MongoPedigreeRepository

      val result = Await.result(repository.getUnprocessed(), duration)

      result.size mustBe 0

      Await.result(pedigrees.drop(false), duration)
    }

    "set processed right result" in {
      val newIndividuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, None, true, None))
      Await.result(pedigrees.insert(PedigreeGenogram(54, "user", newIndividuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)), duration)

      val repository = new MongoPedigreeRepository

      val result = Await.result(repository.setProcessed(54), duration)

      result.isRight mustBe true
      result.right.get mustBe 54

      val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)
      pedigreeList.length mustBe 1
      pedigreeList.foreach(x => {
        x.processed mustBe true
      })

      Await.result(pedigrees.drop(false), duration)
    }

    "add new genogram with one element into it and return a right result" in {
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, None, true, None))

      val repository = new MongoPedigreeRepository

      val result = Await.result(repository.addGenogram(PedigreeGenogram(1, "user", individuals, PedigreeStatus.UnderConstruction, Some("base"),false,0.5,false,None,"MPI",None,7l)), duration)
      result.isRight mustBe true
      result.right.get mustBe 1

      val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)
      pedigreeList.length mustBe 1
      pedigreeList.foreach(x => {
        x.genogram.length mustBe 1
        x.boundary mustBe 0.5
        x.genogram.foreach(y => {
          y.alias mustBe NodeAlias("PI")

          y.idFather.isDefined mustBe false
          y.idMother.isDefined mustBe false
          y.sex mustBe Sex.Unknown
          y.globalCode.isDefined mustBe false
          y.unknown mustBe true
        })
        x.frequencyTable mustBe Some("base")
        x._id mustBe 1
        x.status mustBe PedigreeStatus.UnderConstruction
      }
      )

      Await.result(pedigrees.drop(false), duration)
    }

    "add new genogram with boundary equal to 0.8 return a right result" in {
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), None, None, Sex.Unknown, None, true, None))

      val repository = new MongoPedigreeRepository

      val result = Await.result(repository.addGenogram(PedigreeGenogram(1, "user", individuals, PedigreeStatus.UnderConstruction, Some("base"), false, 0.8, false,None,"MPI",None,7l)), duration)
      result.isRight mustBe true
      result.right.get mustBe 1

      val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)
      pedigreeList.length mustBe 1
      pedigreeList.foreach(x => {
        x.genogram.length mustBe 1
        x.boundary mustBe 0.8
        x.genogram.foreach(y => {
          y.alias mustBe NodeAlias("PI")

          y.idFather.isDefined mustBe false
          y.idMother.isDefined mustBe false
          y.sex mustBe Sex.Unknown
          y.globalCode.isDefined mustBe false
          y.unknown mustBe true
        })
        x.frequencyTable mustBe Some("base")
        x._id mustBe 1
        x.status mustBe PedigreeStatus.UnderConstruction
      }
      )

      Await.result(pedigrees.drop(false), duration)
    }

    "add new genogram with three complete elements into it and return a right result" in {
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, None, true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("Mother"), None, None, Sex.Female, None, false, None))

      val repository = new MongoPedigreeRepository

      try {
        val result = Await.result(repository.addGenogram(PedigreeGenogram(1, "user", individuals, PedigreeStatus.UnderConstruction, Some("base"),false,0.5,false,None,"MPI",None,7l)), duration)
        result.isRight mustBe true
        result.right.get mustBe 1

        val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)
        pedigreeList.length mustBe 1
        pedigreeList.foreach(x => {
          x.genogram.length mustBe 3
          x.boundary mustBe 0.5
          x.genogram(0).idMother.get mustBe NodeAlias("Mother")
          x.genogram(0).idFather.get mustBe NodeAlias("Father")
          x.genogram(0).sex mustBe Sex.Unknown
          x.genogram(0).unknown mustBe true
          x.genogram(0).alias mustBe NodeAlias("PI")
          x.genogram(1).idMother.isDefined mustBe false
          x.genogram(1).idFather.isDefined mustBe false
          x.genogram(1).sex mustBe Sex.Male
          x.genogram(1).unknown mustBe false
          x.genogram(1).alias mustBe NodeAlias("Father")
          x.genogram(2).idMother.isDefined mustBe false
          x.genogram(2).idFather.isDefined mustBe false
          x.genogram(2).sex mustBe Sex.Female
          x.genogram(2).unknown mustBe false
          x.genogram(2).alias mustBe NodeAlias("Mother")
          x.frequencyTable mustBe Some("base")
          x._id mustBe 1
          x.status mustBe PedigreeStatus.UnderConstruction
        }
        )

      }
      finally {
        Await.result(pedigrees.drop(false), duration)
      }
    }

    "get by pedigreeId return a pedigreeGenogram" in {
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, None, true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("Mother"), None, None, Sex.Female, None, false, None))

      val repository = new MongoPedigreeRepository

      try {
        Await.result(repository.addGenogram(PedigreeGenogram(345, "user", individuals, PedigreeStatus.UnderConstruction,None,false,0.5,false,None,"MPI",None,7l)), duration)
        val result = Await.result(repository.get(345), duration)
        result.isDefined mustBe true
        val x = result.get
        x.boundary mustBe 0.5
        x.genogram.length mustBe 3
        x.genogram(0).idMother.get mustBe NodeAlias("Mother")
        x.genogram(0).idFather.get mustBe NodeAlias("Father")
        x.genogram(0).sex mustBe Sex.Unknown
        x.genogram(0).unknown mustBe true
        x.genogram(0).alias mustBe NodeAlias("PI")
        x.genogram(1).idMother.isDefined mustBe false
        x.genogram(1).idFather.isDefined mustBe false
        x.genogram(1).sex mustBe Sex.Male
        x.genogram(1).unknown mustBe false
        x.genogram(1).alias mustBe NodeAlias("Father")
        x.genogram(2).idMother.isDefined mustBe false
        x.genogram(2).idFather.isDefined mustBe false
        x.genogram(2).sex mustBe Sex.Female
        x.genogram(2).unknown mustBe false
        x.genogram(2).alias mustBe NodeAlias("Mother")
        x.frequencyTable mustBe None
        x._id mustBe 345
        x.status mustBe PedigreeStatus.UnderConstruction
      }
      finally {
        Await.result(pedigrees.drop(false), duration)
      }
    }

    "get by pedigreeId return a pedigreeGenogram with the right boundary" in {
      val individuals: Seq[Individual] = Seq(
        Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, None, true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, None, false, None),
        Individual(NodeAlias("Mother"), None, None, Sex.Female, None, false, None))

      val repository = new MongoPedigreeRepository

      try {
        Await.result(repository.addGenogram(PedigreeGenogram(345, "user", individuals, PedigreeStatus.UnderConstruction, None, false, 0.8,false,None,"MPI",None,7l)), duration)
        val result = Await.result(repository.get(345), duration)
        result.isDefined mustBe true
        val x = result.get
        x.boundary mustBe 0.8
        x.genogram.length mustBe 3
        x.genogram(0).idMother.get mustBe NodeAlias("Mother")
        x.genogram(0).idFather.get mustBe NodeAlias("Father")
        x.genogram(0).sex mustBe Sex.Unknown
        x.genogram(0).unknown mustBe true
        x.genogram(0).alias mustBe NodeAlias("PI")
        x.genogram(1).idMother.isDefined mustBe false
        x.genogram(1).idFather.isDefined mustBe false
        x.genogram(1).sex mustBe Sex.Male
        x.genogram(1).unknown mustBe false
        x.genogram(1).alias mustBe NodeAlias("Father")
        x.genogram(2).idMother.isDefined mustBe false
        x.genogram(2).idFather.isDefined mustBe false
        x.genogram(2).sex mustBe Sex.Female
        x.genogram(2).unknown mustBe false
        x.genogram(2).alias mustBe NodeAlias("Mother")
        x.frequencyTable mustBe None
        x._id mustBe 345
        x.status mustBe PedigreeStatus.UnderConstruction
      }
      finally {
        Await.result(pedigrees.drop(false), duration)
      }
    }

    "change pedigree status" in {
      val repository = new MongoPedigreeRepository

      Await.result(repository.addGenogram(PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction, None,false,0.5,false,None,"MPI",None,7l)), duration)

      val result = Await.result(repository.changeStatus(id, PedigreeStatus.Active), duration)
      val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)

      result mustBe Right(id)
      pedigreeList.head.status mustBe PedigreeStatus.Active

      Await.result(pedigrees.drop(false), duration)
    }

    "change pedigree status to UnderConstruction - set pedigree unprocessed" in {
      val repository = new MongoPedigreeRepository

      Await.result(repository.addGenogram(PedigreeGenogram(id, "user", individuals, PedigreeStatus.Active,None,false,0.5,false,None,"MPI",None,7l)), duration)

      val result = Await.result(repository.changeStatus(id, PedigreeStatus.UnderConstruction), duration)
      val pedigreeList = Await.result(pedigrees.find(Json.obj()).cursor[PedigreeGenogram]().collect[List](Int.MaxValue, Cursor.FailOnError[List[PedigreeGenogram]]()), duration)

      result mustBe Right(id)
      pedigreeList.head.status mustBe PedigreeStatus.UnderConstruction
      pedigreeList.head.processed mustBe false

      Await.result(pedigrees.drop(false), duration)
    }

    "find by profile" in {
      val repository = new MongoPedigreeRepository

      Await.result(repository.addGenogram(PedigreeGenogram(id, "user", individuals, PedigreeStatus.UnderConstruction, None,false,0.5,false,None,"MPI",None,7l)), duration)

      val result = Await.result(repository.findByProfile("AR-C-SHDG-1"), duration)

      result(0) mustBe id

      Await.result(pedigrees.drop(false), duration)
    }

  }
}
