package connections

import models.Tables.ConnectionRow
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, SECONDS}

class ConnectionRepositoryTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)
  lazy val repo: ConnectionRepository = new SlickConnectionRepository();
  lazy val rowInstanciaSuperior: ConnectionRow = ConnectionRow(0L, "INSTANCIA_SUPERIOR", "192.168.0.1:9000", false)
  lazy val rowPki: ConnectionRow = ConnectionRow(1L, "PKI", "192.168.0.2:9000", false)

  "ConectionRepositoryTest" must {
    "get connections with empty db" in {

      val res = Await.result(repo.getConnections(), duration)
      res mustBe Right(Connection("", ""))
    }

    "get connections with instancia superior " in {

      val id = Await.result(repo.insert(rowInstanciaSuperior), duration)
      id.fold(error => {
        throw new Exception(error)
      }, id => {

        val res = Await.result(repo.getConnections(), duration)
        res mustBe Right(Connection("192.168.0.1:9000", ""))

        val idDelete = Await.result(repo.deleteById(id), duration)
        idDelete mustBe Right(id)
      })


    }
    "get connections with pki " in {

      val id = Await.result(repo.insert(rowPki), duration)
      id.fold(error => {
        throw new Exception(error)
      }, id => {

        val res = Await.result(repo.getConnections(), duration)
        res mustBe Right(Connection("", "192.168.0.2:9000"))

        val idDelete = Await.result(repo.deleteById(id), duration)
        idDelete mustBe Right(id)

      })


    }
    "get connections with pki and instancia superior " in {

      val idPki = Await.result(repo.insert(rowPki), duration)
      val idInstanciaSuperior = Await.result(repo.insert(rowInstanciaSuperior), duration)
      val res = Await.result(repo.getConnections(), duration)
      res mustBe Right(Connection("192.168.0.1:9000", "192.168.0.2:9000"))

      idPki.fold(error => {
        throw new Exception(error)
      }, id => {
        val idDelete = Await.result(repo.deleteById(id), duration)
        idDelete mustBe Right(id)

      })

      idInstanciaSuperior.fold(error => {
        throw new Exception(error)
      }, id => {
        val idDelete = Await.result(repo.deleteById(id), duration)
        idDelete mustBe Right(id)

      })
    }

    "insertOrUpdate connection " in {
      val idInstanciaSuperior = Await.result(repo.insertOrUpdate(rowInstanciaSuperior), duration)
      idInstanciaSuperior.fold(error => {
        throw new Exception(error)
      }, id => {
        val idDelete = Await.result(repo.deleteById(id.get), duration)
        idDelete mustBe Right(id.get)
      })

      val idInstanciaSuperior2 = Await.result(repo.insertOrUpdate(rowInstanciaSuperior), duration)
      idInstanciaSuperior2.fold(error => {
        throw new Exception(error)
      }, id => {
        val row = Await.result(repo.getById(id.get), duration)
        row.isEmpty mustBe false

        val updatedRow = row.get.copy(name = "updated")

        val idInstanciaSuperior3 = Await.result(repo.insertOrUpdate(updatedRow), duration)
        idInstanciaSuperior3 mustBe Right(None)

        val idDelete = Await.result(repo.deleteById(row.get.id), duration)
        idDelete mustBe Right(row.get.id)

      })
    }

    "update connection " in {
      val res1 = Await.result(repo.updateConnections(Connection("A","B")), duration)

      val res2 = Await.result(repo.getConnections(), duration)

      res1 mustBe res2

      val res3 = Await.result(repo.updateConnections(Connection("C","D")), duration)

      res3.right.get.superiorInstance mustBe "C"
      res3.right.get.pki mustBe "D"

      Await.result(repo.deleteById(Await.result(repo.getByName("INSTANCIA_SUPERIOR"),duration).get.id), duration)
      Await.result(repo.deleteById(Await.result(repo.getByName("PKI"),duration).get.id), duration)
    }

    "getSupInstanceUrl " in {

      val id = Await.result(repo.insert(rowInstanciaSuperior), duration)
      id.fold(error => {
        throw new Exception(error)
      }, id => {

        val res = Await.result(repo.getSupInstanceUrl(), duration)
        res mustBe Some("192.168.0.1:9000")

        val idDelete = Await.result(repo.deleteById(id), duration)
        idDelete mustBe Right(id)
      })
    }

    "getSupInstanceUrl no ok" in {

        val res = Await.result(repo.getSupInstanceUrl(), duration)
        res mustBe None

    }

  }
}
