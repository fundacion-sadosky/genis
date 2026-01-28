package profile

import java.util.{Calendar, Date}

import profile.GenotypificationByType.GenotypificationByType
import specs.PdgSpec
import stubs.Stubs
import types.{MongoDate, SampleCode}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class ProfileRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  val sampleCode = SampleCode("AR-C-SHDG-1")

  "A Profile respository" must {
    "set matcheable true ok" in {
      val profile = Stubs.newProfile

      val profileRepository = new MongoProfileRepository

      Await.result(profileRepository.add(profile), duration)

      val result = Await.result(profileRepository.setMatcheableAndProcessed(profile.globalCode), duration)

      result.isRight mustBe true
      result.right.get mustBe profile.globalCode

      val profileOpt = Await.result(profileRepository.get(result.right.get), duration)

      profileOpt.get.matcheable mustBe true
      profileOpt.get.processed mustBe true
    }

    "get unprocessed 1 element" in {
      val profile = Stubs.newProfile

      val profileRepository = new MongoProfileRepository

      Await.result(profileRepository.add(profile), duration)

      val globalCodes: Seq[SampleCode] = Await.result(profileRepository.getUnprocessed(), duration)

      globalCodes.contains(profile.globalCode) mustBe true
    }

    "add new profiles into it and return entity id" in {
      val p = Stubs.newProfile
      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.add(p)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null
      profileId mustBe p.globalCode
    }

    "fail if an already existing profileId is attempted to be reinserted" in {

      val pd = Stubs.newProfile
      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.add(pd)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      val thrown = intercept[java.lang.Exception] {
        val profileFutureFailed = profileRespository.add(pd)
        val profileIdFailed = Await.result(profileFutureFailed, duration)
      }
      thrown.getMessage.contains("duplicate key") mustBe true
    }

    "retrieve profiles by global code" in {
      val profileRespository = new MongoProfileRepository

      val profileGetFuture = profileRespository.findByCode(sampleCode)

      val p: Option[Profile] = Await.result(profileGetFuture, duration)
      p match {
        case Some(profile) => profile.globalCode mustBe sampleCode
        case None          => fail
      }
    }

    "retrieve profiles by id" in {
      val profileRespository = new MongoProfileRepository

      val profileGetFuture = profileRespository.get(sampleCode)

      val p: Option[Profile] = Await.result(profileGetFuture, duration)
      p match {
        case Some(profile) => profile.globalCode mustBe sampleCode
        case None          => fail("Profile 'AR-C-SHDG-1' not found")
      }
    }

    "retrieve just the genotyfication by globalCode" in {
      val profileRespository = new MongoProfileRepository
      val profileToInsert = Stubs.newProfile
      val profileFuture = profileRespository.add(profileToInsert)
      val profileId = Await.result(profileFuture, duration)

      val profileGetFuture = profileRespository.getGenotyficationByCode(profileToInsert.globalCode)

      val p = Await.result(profileGetFuture, duration)
      p match {
        case Some(g) => {
          g mustBe profileToInsert.genotypification
        }
        case None => fail("Profile '" + profileId + "' not found")
      }
    }

    "add a new analysis and update that profile" in {
      val newP = Stubs.newProfile
      val target = new MongoProfileRepository

      // Add a profile
      val profileFuture = target.add(newP)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      // Get it just to check it was correctly inserted
      val profileGetInsertedFuture = target.get(profileId)
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
      val toInsertAnalysis = new Analysis("toUpdateAnalysisId", MongoDate(new Date()), "Kit", newGenotypification, None)
      val toUpdateGenotypification: GenotypificationByType = Map(1 -> newP.genotypification(1).toSet.union(newGenotypification.toSet).toMap)

      val gc = Await.result(target.addAnalysis(newP._id, toInsertAnalysis, toUpdateGenotypification, None, None, None), duration)

      gc mustBe newP._id
      //       Get it to check it was correctly updated      
      val profileGetUpdatedFuture = target.get(profileId)
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
      val target = new MongoProfileRepository
      val expectedArrayByte = new Array[Byte](1)
      val imageFuture = target.addElectropherogram(Stubs.sampleCode, "newAnalysisId", expectedArrayByte)
      val id = Await.result(imageFuture, duration)
      id must not be null
    }

    "retrieve imagesIds by global code" in {
      val target = new MongoProfileRepository

      val profileToInsert = Stubs.newProfile

      val imageToInsert1 = new Array[Byte](1)
      val imageFuture1 = target.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert1)
      val id1 = Await.result(imageFuture1, duration)
      id1 must not be null

      val imageToInsert2 = new Array[Byte](1)
      val imageFuture2 = target.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert2)
      val id2 = Await.result(imageFuture2, duration)
      id2 must not be null

      val epgListFuture = target.getElectropherogramsByCode(profileToInsert.globalCode)
      val epgList: List[(String, String, String)] = Await.result(epgListFuture, duration)

      epgList.length mustBe 2

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => x._2 mustBe "newAnalysisId")
    }

    "retrieve images array[byte] by global code" in {
      val target = new MongoProfileRepository

      val profileToInsert = Stubs.newProfile

      val imageToInsert = new Array[Byte](5)
      val imageFuture = target.addElectropherogram(profileToInsert.globalCode, "newAnalysisId", imageToInsert)
      val id1 = Await.result(imageFuture, duration)
      id1 must not be null

      val epgListFuture = target.getElectropherogramsByCode(profileToInsert.globalCode)
      val epgList: List[(String, String, String)] = Await.result(epgListFuture, duration)

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => {
        val fut = target.getElectropherogramImage(profileToInsert.globalCode, x._1)
        val im = Await.result(fut, duration)
        im.get mustBe imageToInsert
      })
    }

    "retrieve imagesIds by global code and analysisId" in {
      val target = new MongoProfileRepository

      val profileToInsert = Stubs.newProfile
      val newAnalysisId = "newAnalysisId"

      val imageToInsert1 = new Array[Byte](1)
      val imageFuture1 = target.addElectropherogram(profileToInsert.globalCode, newAnalysisId, imageToInsert1)
      val id1 = Await.result(imageFuture1, duration)
      id1 must not be null

      val imageToInsert2 = new Array[Byte](2)
      val imageFuture2 = target.addElectropherogram(profileToInsert.globalCode, newAnalysisId, imageToInsert2)
      val id2 = Await.result(imageFuture2, duration)
      id2 must not be null

      val epgListFuture = target.getElectropherogramsByAnalysisId(profileToInsert.globalCode, newAnalysisId)
      val epgList = Await.result(epgListFuture, duration)

      epgList.length mustBe 2

      // Cuando sepamos como devolver el id de imagen en el add, verificar _1 tmb
      epgList.foreach(x => x must not be null)
    }

    "add new profiles with LabeledGenotypification and return entity id" in {
      val p = Stubs.newProfileLabeledGenotypification
      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.add(p)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null
      profileId mustBe p.globalCode
    }

    "return a profile with LabeledGenotypification and contributors specified" in {
      val newP = Stubs.newProfileLabeledGenotypification
      val profileRespository = new MongoProfileRepository
      val profileFuture = profileRespository.add(newP)
      val profileId = Await.result(profileFuture, duration)
      profileId must not be null

      val profileGetFuture = profileRespository.findByCode(newP.globalCode)

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
      val profileRespository = new MongoProfileRepository
      val labelsFuture = profileRespository.getLabels(sampleCode)

      val labels = Await.result(labelsFuture, duration)
      labels mustBe None
    }

    "retrieve just the labeledGenotypification" in {
      val profileRespository = new MongoProfileRepository
      val labelsFuture = profileRespository.getLabels(SampleCode("AR-C-SHDG-2"))

      val labels = Await.result(labelsFuture, duration)
      val expectedLabels: Profile.LabeledGenotypification = Map("V" -> Map("D18S51" -> List(Allele(18.0)), "D3S1358" -> List(Allele(17.0)), "D7S820" -> List(Allele(10.0)), "FGA" -> List(Allele(27.0), Allele(20.0))),
        "O" -> Map("D18S51" -> List(Allele(22.0)), "D3S1358" -> List(Allele(17.0), Allele(15.0)), "D7S820" -> List(Allele(12.0)), "FGA" -> List(Allele(27.0))))
      println(labels)
      labels.get mustBe expectedLabels
    }

    "retrieve None when the profile doesn't exist" in {
      val profileRespository = new MongoProfileRepository
      val labelsFuture = profileRespository.getLabels(SampleCode("AR-C-XXX-1"))

      val labels = Await.result(labelsFuture, duration)
      labels mustBe None
    }

    "be able to delete an unused kit" in {
      val repository = new MongoProfileRepository()
      val result = Await.result(repository.canDeleteKit("UNUSED"), duration)

      result mustBe true
    }

    "not be able to delete a kit in a proile" in {
      val repository = new MongoProfileRepository()
      val result = Await.result(repository.canDeleteKit("Powerplex16"), duration)

      result mustBe false
    }

  }

}