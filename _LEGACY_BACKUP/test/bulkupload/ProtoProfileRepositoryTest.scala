package bulkupload

import configdata.{CategoryService, FullCategory}
import kits.StrKitLocus
import models.Tables
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick._
import search.PaginationSearch
import specs.PdgSpec
import stubs.Stubs
import types.AlphanumericId

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}
import scala.slick.lifted.TableQuery

class ProtoProfileRepositoryTest extends PdgSpec with MockitoSugar {

  val protoProfileGcD = "XX-X-XXXX-"
  val duration = Duration(10, SECONDS)
  val geno = Stubs.newProfile.genotypification.flatMap {
    case (at, locusMap) => locusMap.map(x => GenotypificationItem(x._1, x._2)).toList
  }.toList
  val matchingRules = Stubs.listOfMinimumStringencies
  val mismatches = Stubs.mismatches
  val seq = Seq(
    ProtoProfile(0, "sample1", "user1", "CATT", ProtoProfileStatus.ReadyForApproval, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None),
    ProtoProfile(0, "sample2", "user1", "CATT", ProtoProfileStatus.ReadyForApproval, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None))
  val seq2 = Seq(
    ProtoProfile(0, "sample1", "user1", "CATT", ProtoProfileStatus.ReadyForApproval, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None),
    ProtoProfile(0, "sample2", "user1", "CATT", ProtoProfileStatus.Imported, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None))
  val kitMap: Map[String, List[StrKitLocus]] = Map(("kit" -> List(StrKitLocus("LOCUS 1", "LOCUS1", None, 2, 2, None, 1),
    StrKitLocus("LOCUS 2", "LOCUS2", None, 2, 2, None, 5),
    StrKitLocus("LOCUS 3", "LOCUS3", None, 2, 2, None, 4),
    StrKitLocus("LOCUS 4", "LOCUS4", None, 2, 2, None, 2),
    StrKitLocus("LOCUS 5", "LOCUS5", None, 2, 2, None, 3),
    StrKitLocus("LOCUS 6", "LOCUS6", None, 2, 2, None, 6))))

  val fullCat = FullCategory(AlphanumericId("CATTT"),
    "", None, AlphanumericId("GROUP"),
    false, false, true, false,manualLoading=true, Map.empty, Seq.empty, Seq.empty, Seq.empty)

  val protoProfiles: TableQuery[Tables.ProtoProfile] = Tables.ProtoProfile

  private def queryDefineGetProtoProfile(idBatch: Column[Long]) = Compiled(for (
    batch <- protoProfiles if batch.idBatch === idBatch
  ) yield batch)

  "ProtoProfileRepository" must {
    "create a batch" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val res = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)

      res must be > (0l)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(res).delete
      }
    }

    "get a batch" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val id = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,Some("Lote2"),"Autosomal"), duration)

      val rest = Await.result(repo.getBatch(id), duration)

      rest.get.id must be(id)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(id).delete
      }
    }

    "get batches for step 1" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val id = Await.result(repo.createBatch("user", Seq(ProtoProfile(0, "sample1", "user1", "CATT", ProtoProfileStatus.ReadyForApproval, "kit", geno, mismatches, matchingRules, Seq("1error", "2error"), "",None)).toStream, "SHDG", kitMap,None,"Autosomal"), duration)

      val rest = Await.result(repo.getBatchesStep1("user", false), duration)

      rest.size must be(1)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(id).delete
      }
    }

    "get batches for step 1 - superuser" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val id = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)

      val rest = Await.result(repo.getBatchesStep1("SuperUser", true), duration)

      rest.size must be(1)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(id).delete
      }
    }

    "get proto profiles for step 1" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val id1 = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,Some("Lote1"),"Autosomal"), duration)
      val id2 = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)

      val rest = Await.result(repo.getProtoProfilesStep1(id1, Some(PaginationSearch(0, 50))), duration)

      rest.size must be(2)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(id1).delete
        queryDefineGetProtoProfile(id2).delete
      }
    }

    "get proto profiles by id" in {

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val batchId = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)

      var id = 0l
      DB.withSession { implicit session =>
        id = queryDefineGetProtoProfile(batchId).first.id
      }

      val rest = Await.result(repo.getProtoProfile(id), duration)

      rest.get.id must be(id)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(batchId).delete
      }
    }

    "check if profile existes in stage or genias area" in {

      val repo = new SlickProtoProfileRepository(null, protoProfileGcD, app)

      val (a, b) = Await.result(repo.exists("sampleName"), duration)

      a mustBe None
      b mustBe None
    }

    "create a batch with the correct amount of markers" in {
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val batchId = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,Some("Lote"),"Autosomal"), duration)

      var id = 0l
      DB.withSession { implicit session =>
        id = queryDefineGetProtoProfile(batchId).first.id
      }

      val result = Await.result(repo.getProtoProfile(id), duration)

      result.get.genotypification.size mustBe 5

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(batchId).delete
      }
    }

    "be able to delete an unused kit" in {
      val repository = new SlickProtoProfileRepository(null, null, app)
      val result = Await.result(repository.canDeleteKit("UNUSED"), duration)

      result mustBe true
    }

    "not be able to delete a kit in a proto proile" in {
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repository = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val id = Await.result(repository.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)
      val result = Await.result(repository.canDeleteKit("kit"), duration)

      result mustBe false

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(id).delete
      }
    }

    "delete batch ok" in {
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val batchId = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,Some("Lote"),"Autosomal"), duration)

      batchId must be > (0L)

      val persistedBatch = Await.result(repo.getBatch(batchId), duration)
      persistedBatch.get.id mustBe batchId

      val result =  Await.result( repo.deleteBatch(batchId) , duration)
      result mustBe Right(batchId)

      val persistedBatch2 = Await.result(repo.getBatch(batchId), duration)
      persistedBatch2 mustBe None

    }

    "count imported samples" in {
      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(AlphanumericId("CATT"))).thenReturn(Option(fullCat))

      val repo = new SlickProtoProfileRepository(categoryService, protoProfileGcD, app)

      val batchId = Await.result(repo.createBatch("user", seq.toStream, "SHDG", kitMap,None,"Autosomal"), duration)
      batchId must be > (0L)

      val count = Await.result(repo.countImportedProfilesByBatch(batchId), duration)
      count mustBe Right(0L)

      val batchId2 = Await.result(repo.createBatch("user", seq2.toStream, "SHDG", kitMap,Some("Lotes"),"Autosomal"), duration)
      batchId must be > (0L)

      val count2 = Await.result(repo.countImportedProfilesByBatch(batchId2), duration)
      count2 mustBe Right(1L)

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(batchId).delete
      }

      DB.withSession { implicit session =>
        queryDefineGetProtoProfile(batchId2).delete
      }
    }


  }

}
