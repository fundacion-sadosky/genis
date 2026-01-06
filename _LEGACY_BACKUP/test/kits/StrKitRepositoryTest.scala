package kits

import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import stubs.Stubs

import scala.concurrent.Await
import scala.concurrent.duration._

class StrKitRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val fullKit = Stubs.fullKits.head
  val kit = StrKit(fullKit.id, fullKit.name, fullKit.`type`, fullKit.locy_quantity, fullKit.representative_parameter)
  val id = kit.id
  val alias = "ALIAS"
  val locus = NewStrKitLocus("YIndel", None, 1)

  "A StrKitRepository" must {

    "add kit" in {

      val repository = new SlickKitDataRepository()
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "not add duplicated kit E0694" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)

      result.isLeft mustBe true
      result mustBe Left("E0694: Id de kit: KIT 1 duplicado.")

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "add alias" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "not add duplicated alias E0640" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      result.isLeft mustBe true
      result mustBe Left("E0640: Alias ALIAS duplicado.")

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "add locus" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.addLocus(id, locus) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLocus(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete kit" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)

      result mustBe Right(id)
    }

    "not delete kit with dependencies" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)

      result.isLeft mustBe true

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete alias" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "delete locus" in {
      val repository = new SlickKitDataRepository()
      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addLocus(id, locus) }, duration)

      val result = Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLocus(id) }, duration)

      result mustBe Right(id)

      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "list all kits" in {
      val repository = new SlickKitDataRepository()

      val result = Await.result(repository.listFull(), duration)

      result.size must not be 0
    }

    "list full kits with alias and locus" in {
      val repository = new SlickKitDataRepository()

      Await.result(repository.runInTransactionAsync { implicit session => repository.add(kit) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addAlias(id, alias) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.addLocus(id, locus) }, duration)

      val result = Await.result(repository.listFull(), duration)
      val searched = result.find(_.id == id).get

      searched.alias mustBe Seq(alias)
      searched.locus mustBe Seq(locus)

      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteLocus(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.deleteAlias(id) }, duration)
      Await.result(repository.runInTransactionAsync { implicit session => repository.delete(id) }, duration)
    }

    "get kit by id" in {
      val repository = new SlickKitDataRepository()

      val result = Await.result(repository.get("Powerplex16"), duration)

      result.isDefined mustBe true
    }

  }
}
