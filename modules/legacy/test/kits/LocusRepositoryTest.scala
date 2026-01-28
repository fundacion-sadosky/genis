package kits

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Await
import scala.concurrent.duration._

class LocusRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val locus = Stubs.fullLocus.head.locus
  val id = locus.id
  val alias = "ALIAS"
  val link = LocusLink("AMEL", 0.5, 0.5)

  "A LocusRepository" must {

    "add locus" in {
      val repository = new SlickLocusRepository()
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }
    "Update locus" in {
      val repository = new SlickLocusRepository()
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)

      result mustBe Right(id)
      val result2 = Await.result(repository.runInTransactionAsync { implicit session => repository.update(locus) }, duration)
      result2 mustBe Right(())
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }
    "not add duplicated locus E0688" in {
      val repository = new SlickLocusRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)

      result.isLeft mustBe true
      result mustBe Left("E0688: Id de marcador: LOCUS 1 duplicado.")

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "add alias" in {
      val repository = new SlickLocusRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "not add duplicated alias E0640" in {
      val repository = new SlickLocusRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      result.isLeft mustBe true
      result mustBe Left("E0640: Alias ALIAS duplicado.")

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "add links" in {
      val repository = new SlickLocusRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addLink(id, link) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLinks(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete locus" in {
      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)

      result mustBe Right(id)
    }

    "not delete locus with dependencies" in {
      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)

      result.isLeft mustBe true

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete alias" in {
      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete links" in {
      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addLink(id, link) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLinks(id) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "not be able to delete a linked locus" in {
      val link = Stubs.fullLocus(1).locus
      val locusLink = LocusLink(link.id, 0.5, 0.5)

      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(link) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addLink(id, locusLink) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.canDeleteLocusByLink(link.id) }, duration)

      result mustBe false

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLinks(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(link.id) }, duration)
    }

    "not be able to delete a locus in a kit" in {
      val repository = new SlickLocusRepository()
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.canDeleteLocusByKit(link.locus) }, duration)

      result mustBe false

    }

    "be able to delete an independent locus" in {
      val repository = new SlickLocusRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)

      val resultKit = Await.result(repository.runInTransactionAsync { implicit session => repository.canDeleteLocusByKit(id) }, duration)
      val resultLink = Await.result(repository.runInTransactionAsync { implicit session => repository.canDeleteLocusByLink(id) }, duration)

      resultKit mustBe true
      resultLink mustBe true

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "list all locus" in {
      val repository = new SlickLocusRepository()

      val result = Await.result(repository.listFull(), duration)

      result.size must not be 0
    }

    "list full locus with alias and links" in {
      val repository = new SlickLocusRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(locus) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addLink(id, link) }, duration)

      val result = Await.result(repository.listFull(), duration)
      val searched = result.find(_.locus.id == id).get

      searched.locus mustBe locus
      searched.alias mustBe Seq(alias)
      searched.links mustBe Seq(link)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLinks(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "get locus by analysis type name" in {
      val repository = new SlickLocusRepository()

      val result = Await.result(repository.getLocusByAnalysisTypeName("Autosomal"), duration)

      result.size must not be 0
      result.exists(_.analysisType != 1) mustBe false
    }

    "get locus by analysis type" in {
      val repository = new SlickLocusRepository()

      val result = Await.result(repository.getLocusByAnalysisType(1), duration)

      result.size must not be 0
      result.exists(_.analysisType != 1) mustBe false
    }

  }
}
