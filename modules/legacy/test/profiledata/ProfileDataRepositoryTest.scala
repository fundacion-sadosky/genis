package profiledata

import java.text.SimpleDateFormat
import java.util.Date

import models.Tables
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import specs.PdgSpec
import stubs.Stubs
import types.SampleCode

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class ProfileDataRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)
  val sampleCode = new SampleCode("AR-C-SHDG-1100")
  val pda = Stubs.profileData
  val sdf = new SimpleDateFormat("yyyy-MM-dd")
  val onlyDate = sdf.parse("2015-01-01")

  val minimalPd = ProfileData(pda.category, null, None, None, None, None, None, None, pda.internalSampleCode+"_1", pda.assignee, "SHDG", false, None, None, None, None, None, None,false)

  val pd = ProfileData(pda.category, sampleCode, pda.attorney, Option("SANGRE"), pda.court,
    Option("ASESINATO"), Option("PERSONAS"), pda.criminalCase,
    pda.internalSampleCode, pda.assignee, "SHDG", false, None, pda.responsibleGeneticist,
    pda.profileExpirationDate, pda.sampleDate, pda.sampleEntryDate, None,false)

  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData
  val profilesDataFiliation: TableQuery[Tables.ProfileDataFiliation] = Tables.ProfileDataFiliation

  private def queryDefineGetProfileDataFiliation(globalCode: Column[String]) = Compiled(for (
    profileDataFiliation <- profilesDataFiliation if profileDataFiliation.profileData === globalCode
  ) yield profileDataFiliation)

  private def queryDefineGetProfileData(globalCode: Column[String]) = Compiled(for (
    profileData <- profilesData if profileData.globalCode === globalCode
  ) yield profileData)

  "A ProfileData respository" must {
    "add new profiles into it and return entity id" in {
      val profileDataRepository = new SlickProfileDataRepository

      val gc = Await.result(profileDataRepository.add(pda, "AR-C-SHDG"), duration)

      val globalCode = Await.result(profileDataRepository.getGlobalCode("internalSampleCode"), duration)

      gc.text mustBe globalCode.get.text

      DB.withSession { implicit session =>
        queryDefineGetProfileDataFiliation(gc.text).delete
        queryDefineGetProfileData(gc.text).delete
      }
    }

    "add a minimal pd" in {
      val profileDataRepo = new SlickProfileDataRepository

      val gc = Await.result(profileDataRepo.add(minimalPd, "AR-C-SHDG"), duration)

      val globalCode = Await.result(profileDataRepo.getGlobalCode("internalSampleCode_1"), duration)

      gc.text mustBe globalCode.get.text

      DB.withSession { implicit session =>
        queryDefineGetProfileData(gc.text).delete
      }
    }

    "get profileData ok" in {

      val profileDataRepo = new SlickProfileDataRepository

      val rr = Await.result(profileDataRepo.findByCode(SampleCode("AR-C-SHDG-1")), duration).get

      rr.globalCode.text mustBe "AR-C-SHDG-1"
    }

    "retrieve a profile by Id" in {
      val profileDataRepository = new SlickProfileDataRepository
      val profileData = Await.result(profileDataRepository.get(1), duration)
      profileData.globalCode.text mustBe "AR-C-SHDG-1"
    }

    "update a profile without data filiation" in {

      val profileDataRepository = new SlickProfileDataRepository

      val gc = Await.result(profileDataRepository.add(pda, "AR-C-SHDG"), duration)

      val result = Await.result(profileDataRepository.updateProfileData(gc, pda), duration)

      DB.withSession { implicit session =>
        queryDefineGetProfileDataFiliation(gc.text).delete
        queryDefineGetProfileData(gc.text).delete
      }

      result mustBe true
    }

    "update a profile with data filiation" in {

      val profileDataRepository = new SlickProfileDataRepository

      val gc = Await.result(profileDataRepository.add(pda, "AR-C-SHDG"), duration)

      val pmdf = new DataFiliation(Some("SA LO"), Some("nick"), Some(new Date(0)), Some("AR"), Some("AR"), Some("11"), Some("RCP"), Some("SA"), List.empty[Long], List.empty[Long], List.empty[Long])

      val profileData = ProfileData(pda.category, gc,
        Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
        Option("PERSONAS"), Option("case number"), "internalSampleCode", "assignee", "SHDG",
        false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
        Option(onlyDate), Some(pmdf),false)

      val result = Await.result(profileDataRepository.updateProfileData(gc, profileData), duration)

      result mustBe true

      DB.withSession { implicit session =>
        queryDefineGetProfileDataFiliation(gc.text).delete
        queryDefineGetProfileData(gc.text).delete
      }
    }

    "get all profiles by user" in {
      val profileDataRepository = new SlickProfileDataRepository

      val search = ProfileDataSearch("tst-admintist", false, 0, Int.MaxValue, "", true, true)
      val result = Await.result(profileDataRepository.getProfilesByUser(search), duration)

      result.length mustBe 1005
    }

    "get all profiles by super user" in {
      val profileDataRepository = new SlickProfileDataRepository

      val search = ProfileDataSearch("SuperUser", true, 0, Int.MaxValue, "", true, true)
      val result = Await.result(profileDataRepository.getProfilesByUser(search), duration)

      result.length mustBe 1021
    }
    
    "get several profiles" in {

      val profileDataRepo = new SlickProfileDataRepository

      val res = Await.result(profileDataRepo.findByCodes(List[SampleCode](SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-2"))), duration)

      res.length mustBe 2
      res(0).globalCode.text mustBe "AR-C-SHDG-1"
      res(1).globalCode.text mustBe "AR-C-SHDG-2"
    }

    "addExternalProfile profile" in {
      val profileDataRepo = new SlickProfileDataRepository
      val res = Await.result(profileDataRepo.addExternalProfile(Stubs.profileData,"SHDG","SHDG"), duration)

      DB.withSession { implicit session =>
        queryDefineGetProfileData(res.text).delete
      }
    }

  }
}