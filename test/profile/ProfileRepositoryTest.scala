package profile

import java.util.{Calendar, Date}

import profile.GenotypificationByType.GenotypificationByType
import specs.PdgSpec
import stubs.Stubs
import types.{MongoDate, SampleCode}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
class ProfileRepositoryTest(repository: ProfileRepository) extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val sampleCode = SampleCode("AR-B-Z-1147")

  "A Profile repository" must {
    "set matcheable true ok" in {
      val profile = Stubs.newProfile

      Await.result(repository.add(profile), duration)

      val result = Await.result(repository.setMatcheableAndProcessed(profile.globalCode), duration)

      result.isRight mustBe true
      result.right.get mustBe profile.globalCode

      val profileOpt = Await.result(repository.get(result.right.get), duration)

      profileOpt.get.matcheable mustBe true
      profileOpt.get.processed mustBe true
    }

    "get unprocessed 1 element" in {
      val profile = Stubs.newProfile

      Await.result(repository.add(profile), duration)

      val globalCodes: Seq[SampleCode] = Await.result(repository.getUnprocessed(), duration)

      globalCodes.contains(profile.globalCode) mustBe true
    }

    "add new profiles into it and return entity id" in {
      val p = Stubs.newProfile
      val profileFuture = repository.add(p)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null
      profileId mustBe p.globalCode
    }

    "fail if an already existing profileId is attempted to be reinserted" in {

      val pd = Stubs.newProfile
      val profileFuture = repository.add(pd)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      val thrown = intercept[java.lang.Exception] {
        val profileFutureFailed = repository.add(pd)
        val profileIdFailed = Await.result(profileFutureFailed, duration)
      }
      Seq("duplicate key", "Document update conflict.").exists(thrown.getMessage.contains) mustBe true
    }

    "retrieve profiles by global code" in {

      val profileGetFuture = repository.findByCode(sampleCode)

      val p: Option[Profile] = Await.result(profileGetFuture, duration)
      p match {
        case Some(profile) => profile.globalCode mustBe sampleCode
        case None          => fail
      }
    }

    "retrieve profiles by id" in {

      val profileGetFuture = repository.get(sampleCode)

      val p: Option[Profile] = Await.result(profileGetFuture, duration)
      p match {
        case Some(profile) => profile.globalCode mustBe sampleCode
        case None          => fail("Profile 'AR-C-SHDG-1' not found")
      }
    }

//    Commented out because the method is not implemented nor used
//    "retrieve just the genotyfication by globalCode" in {
//      val profileRespository = new MongoProfileRepository
//      val profileToInsert = Stubs.newProfile
//      val profileFuture = profileRespository.add(profileToInsert)
//      val profileId = Await.result(profileFuture, duration)
//
//      val profileGetFuture = profileRespository.getGenotyficationByCode(profileToInsert.globalCode)
//
//      val p = Await.result(profileGetFuture, duration)
//      p match {
//        case Some(g) => {
//          g mustBe profileToInsert.genotypification
//        }
//        case None => fail("Profile '" + profileId + "' not found")
//      }
//    }

    "add a new analysis and update that profile" in {
      val newP = Stubs.newProfile

      // Add a profile
      val profileFuture = repository.add(newP)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      // Get it just to check it was correctly inserted
      val profileGetInsertedFuture = repository.get(profileId)
      val pInserted: Option[Profile] = Await.result(profileGetInsertedFuture, duration)
      pInserted match {
        case Some(profile) => {
          profile.globalCode mustBe newP.globalCode
          profile.genotypification(1).isDefinedAt("LOCUS 1") mustBe true
          profile.genotypification(1).isDefinedAt("LOCUS 2") mustBe true
          profile.genotypification(1).isDefinedAt("LOCUS 10") mustBe false
          profile.analyses.get.length mustBe 0
        }
        case None => fail("Profile '" + profileId + "' not found")
      }

      val newGenotypification: Map[String, List[AlleleValue]] = Map("LOCUS 10" -> List(Allele(10), Allele(14)))
      // ACA PUEDE FALLAR COUCH PORQUE ES UNA MONGODATE
      val toInsertAnalysis = new Analysis("toUpdateAnalysisId", MongoDate(new Date()), "Kit", newGenotypification, None)
      val toUpdateGenotypification: GenotypificationByType = Map(1 -> newP.genotypification(1).toSet.union(newGenotypification.toSet).toMap)

      val gc = Await.result(repository.addAnalysis(newP._id, toInsertAnalysis, toUpdateGenotypification, None, None, None), duration)

      gc mustBe newP._id
      //       Get it to check it was correctly updated      
      val profileGetUpdatedFuture = repository.get(profileId)
      val pUpdated: Option[Profile] = Await.result(profileGetUpdatedFuture, duration)
      pUpdated match {
        case Some(profile) => {
          profile.globalCode mustBe newP.globalCode
          profile.genotypification(1).isDefinedAt("LOCUS 1") mustBe true
          profile.genotypification(1).isDefinedAt("LOCUS 10") mustBe true
          profile.genotypification(1).get("LOCUS 10").get.length mustBe 2
          profile.genotypification(1).get("LOCUS 10").get.contains(Allele(10)) mustBe true
          profile.genotypification(1).get("LOCUS 10").get.contains(Allele(14)) mustBe true
          profile.analyses.get.length mustBe 1
          profile.analyses.get(0).id mustBe "toUpdateAnalysisId"
          profile.analyses.get(0).genotypification.get("LOCUS 10").get.length mustBe 2
          profile.analyses.get(0).genotypification.get("LOCUS 10").get.contains(Allele(10)) mustBe true
          profile.analyses.get(0).genotypification.get("LOCUS 10").get.contains(Allele(14)) mustBe true
        }
        case None => fail("Profile '" + profileId + "' not found")
      }
    }

    "add an electropherogram" in {
      val expectedArrayByte = new Array[Byte](1)
      val imageFuture = repository.addElectropherogram(Stubs.sampleCode, "newAnalysisId", expectedArrayByte)
      val id = Await.result(imageFuture, duration)
      id must not be null
    }

    "retrieve imagesIds by global code" in {

      val profileToInsert = Stubs.newProfile

      val imageToInsert1 = new Array[Byte](1)
      val imageFuture1 = repository.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert1)
      val id1 = Await.result(imageFuture1, duration)
      id1 must not be null

      val imageToInsert2 = new Array[Byte](1)
      val imageFuture2 = repository.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert2)
      val id2 = Await.result(imageFuture2, duration)
      id2 must not be null

      val epgListFuture = repository.getElectropherogramsByCode(profileToInsert.globalCode)
      val epgList: List[(String, String, String)] = Await.result(epgListFuture, duration)

      epgList.length mustBe 2

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => x._2 mustBe "newAnalysisId")
    }

    "retrieve images array[byte] by global code" in {

      val profileToInsert = Stubs.newProfile

      val imageToInsert = new Array[Byte](5)
      val imageFuture = repository.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert)
      val id1 = Await.result(imageFuture, duration)
      id1 must not be null

      val epgListFuture = repository.getElectropherogramsByCode(profileToInsert.globalCode)
      val epgList: List[(String, String, String)] = Await.result(epgListFuture, duration)

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => {
        val fut = repository.getElectropherogramImage(profileToInsert.globalCode, x._1)
        val im = Await.result(fut, duration)
        im.get mustBe imageToInsert
      })
    }

    "retrieve imagesIds by global code and analysisId" in {

      val profileToInsert = Stubs.newProfile
      val newAnalysisId = "newAnalysisId"

      val imageToInsert1 = new Array[Byte](1)
      val imageFuture1 = repository.addElectropherogram(profileToInsert.globalCode, newAnalysisId, imageToInsert1)
      val id1 = Await.result(imageFuture1, duration)
      id1 must not be null

      val imageToInsert2 = new Array[Byte](2)
      val imageFuture2 = repository.addElectropherogram(profileToInsert.globalCode, newAnalysisId, imageToInsert2)
      val id2 = Await.result(imageFuture2, duration)
      id2 must not be null

      val epgListFuture = repository.getElectropherogramsByAnalysisId(profileToInsert.globalCode, newAnalysisId)
      val epgList = Await.result(epgListFuture, duration)

      epgList.length mustBe 2

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => x must not be null)
    }

    "add new profiles with LabeledGenotypification and return entity id" in {
      val p = Stubs.newProfileLabeledGenotypification
      val profileFuture = repository.add(p)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null
      profileId mustBe p.globalCode
    }

    "return a profile with LabeledGenotypification and contributors specified" in {
      val newP = Stubs.newProfileLabeledGenotypification
      val profileFuture = repository.add(newP)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      val profileGetFuture = repository.findByCode(newP.globalCode)

      val p: Option[Profile] = Await.result(profileGetFuture, duration)
      p match {
        case Some(profile) => {
          profile.globalCode mustBe newP.globalCode
          profile.labeledGenotypification.isDefined mustBe true

          val labels = profile.labeledGenotypification.get

          labels.size mustBe newP.labeledGenotypification.get.size
          labels.get("label1").get.size mustBe newP.labeledGenotypification.get.get("label1").get.size
          labels.get("label2").get.size mustBe newP.labeledGenotypification.get.get("label2").get.size

          labels.get("label1").get mustBe Map(
            "LOCUS 1" -> List(Allele(1)), "LOCUS 2" -> List(Allele(1)))

          labels.get("label2").get mustBe Map(
            "LOCUS 1" -> List(Allele(2.1)), "LOCUS 2" -> List(Allele(5)))

          profile.contributors mustBe newP.contributors
        }
        case None => fail("Profile " + newP.globalCode + " should have been returned")
      }

    }

    "retrieve None when there's no labeledGenotypification" in {
      val labelsFuture = repository.getLabels(sampleCode)

      val labels = Await.result(labelsFuture, duration)
      labels mustBe None
    }

    "retrieve just the labeledGenotypification" in {
      val labelsFuture = repository.getLabels(SampleCode("AR-B-LAB-1968909017"))

      val labels = Await.result(labelsFuture, duration)
      val expectedLabels: Profile.LabeledGenotypification = Map(
        "label1" -> Map(
          "LOCUS 1" -> List(Allele(1.0)),
          "LOCUS 2" -> List(Allele(1.0))
        ),
        "label2" -> Map(
          "LOCUS 1" -> List(Allele(2.1)),
          "LOCUS 2" -> List(Allele(5.0))
        )
      )

      println(labels)
      labels.get mustBe expectedLabels
    }

    "retrieve None when the profile doesn't exist" in {
      val labelsFuture = repository.getLabels(SampleCode("AR-C-XXX-1"))

      val labels = Await.result(labelsFuture, duration)
      labels mustBe None
    }

    "be able to delete an unused kit" in {
      val result = Await.result(repository.canDeleteKit("UNUSED"), duration)

      result mustBe true
    }

    "not be able to delete a kit in a profile" in {
      val result = Await.result(repository.canDeleteKit("ArgusX8"), duration) //Un kit que esté en uso

      result mustBe false
    }

  }

}

class MongoProfileRepositoryTest extends ProfileRepositoryTest(new MongoProfileRepository)

class CouchProfileRepositoryTest extends ProfileRepositoryTest(new CouchProfileRepository)