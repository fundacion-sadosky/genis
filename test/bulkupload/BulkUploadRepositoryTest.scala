package bulkupload

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import specs.PdgSpec
import types.SampleCode
import types.AlphanumericId
import models.Tables
import models.Tables.ProfileDataRow
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.Column
import configdata.{SlickCategoryRepository, Group, Category}

class BulkUploadRepositoryTest extends PdgSpec {

  val duration = Duration(10, SECONDS)

  // Test data constants
  // Note: globalCode must match pattern /^[A-Z]{2,3}-[A-Z]-[A-Z]+-\d+$/
  val testInternalSampleCode = "TEST-SMG-99999"
  val testGlobalCode = "AR-C-SHDG-99999"
  val testGroupId = AlphanumericId("TEST_BULK_GROUP")
  val testCategoryId = AlphanumericId("TEST_BULK_CAT")
  val testAssignee = "testuser"
  val testLaboratory = "SHDG"

  val profilesData: TableQuery[Tables.ProfileData] = Tables.ProfileData

  // Compiled query for delete operation
  private def queryDefineGetProfileData(globalCode: Column[String]) = Compiled(for (
    profileData <- profilesData if profileData.globalCode === globalCode
  ) yield profileData)

  private def ensureTestCategoryExists(): Unit = {
    val categoryRepo = new SlickCategoryRepository

    // Create test group and category for the FK constraint
    try {
      Await.result(categoryRepo.addGroup(Group(testGroupId, "Test Bulk Group", None)), duration)
    } catch {
      case _: Exception => // Group might already exist
    }

    val existingCategories = Await.result(categoryRepo.listCategories, duration).map(_.id)
    if (!existingCategories.contains(testCategoryId)) {
      Await.result(categoryRepo.addCategory(
        Category(testCategoryId, testGroupId, "Test Bulk Category", isReference = true, None)
      ), duration)
    }
  }

  private def cleanupTestCategory(): Unit = {
    val categoryRepo = new SlickCategoryRepository

    try {
      Await.result(categoryRepo.removeCategory(testCategoryId), duration)
    } catch { case _: Exception => }
    try {
      Await.result(categoryRepo.removeGroup(testGroupId), duration)
    } catch { case _: Exception => }
  }

  private def insertTestProfileData()(implicit session: Session): Unit = {
    val testRow = ProfileDataRow(
      id = 0,
      category = testCategoryId.text,
      globalCode = testGlobalCode,
      internalCode = testInternalSampleCode,
      internalSampleCode = testInternalSampleCode,
      assignee = testAssignee,
      laboratory = testLaboratory,
      deleted = false
    )
    profilesData += testRow
  }

  private def deleteTestProfileData()(implicit session: Session): Unit = {
    queryDefineGetProfileData(testGlobalCode).delete
  }

  "A BulkUploadRepository" must {
    "get batches for step 1 returns empty for non-existent user" in {
      // Use a unique user ID that is guaranteed not to have any batches
      val nonExistentUserId = s"NONEXISTENT_TEST_USER_${System.currentTimeMillis}"

      val repo = new SlickProtoProfileRepository(null, "", app)

      val result = Await.result(repo.getBatchesStep1(nonExistentUserId, false, 0, 10), duration)

      result.size mustBe 0
    }

    "tell if a sample already exists" in {
      // Setup: Create category (FK constraint) and insert test data
      ensureTestCategoryExists()

      DB.withTransaction { implicit session =>
        insertTestProfileData()
      }

      try {
        val repo = new SlickProtoProfileRepository(null, "", app)

        val result = Await.result(repo.exists(testInternalSampleCode), duration)

        result._1 mustBe Some(SampleCode(testGlobalCode))
        result._2 mustBe None
      } finally {
        // Cleanup: Remove test data and category
        DB.withTransaction { implicit session =>
          deleteTestProfileData()
        }
        cleanupTestCategory()
      }
    }

    "return None when sample does not exist" in {
      val repo = new SlickProtoProfileRepository(null, "", app)

      val result = Await.result(repo.exists("NONEXISTENT-SAMPLE"), duration)

      result._1 mustBe None
      result._2 mustBe None
    }

  }

}