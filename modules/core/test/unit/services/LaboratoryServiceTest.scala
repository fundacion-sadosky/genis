package services

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, Future}

import configdata.LaboratoryRepository
import types.Laboratory

class LaboratoryServiceTest extends PlaySpec with MockitoSugar {

  private val duration = Duration(10, SECONDS)
  private val labCode  = "LAB-LOCAL"

  private def lab(code: String): Laboratory =
    Laboratory("Nombre", code, "AR", "BA", "Calle 1", "123", "a@b.com", 0.0, 0.0)

  "LaboratoryServiceImpl.get" should {

    "set instance = Some(true) when the lab code matches laboratory.code (C2)" in {
      val repo = mock[LaboratoryRepository]
      when(repo.get(labCode)).thenReturn(Future.successful(Some(lab(labCode))))
      val service = new LaboratoryServiceImpl(repo, labCode)

      Await.result(service.get(labCode), duration).flatMap(_.instance) mustBe Some(true)
    }

    "set instance = Some(false) when the lab code does not match (C2)" in {
      val repo = mock[LaboratoryRepository]
      when(repo.get("OTRO")).thenReturn(Future.successful(Some(lab("OTRO"))))
      val service = new LaboratoryServiceImpl(repo, labCode)

      Await.result(service.get("OTRO"), duration).flatMap(_.instance) mustBe Some(false)
    }

    "return None when the repository finds no lab" in {
      val repo = mock[LaboratoryRepository]
      when(repo.get("NOPE")).thenReturn(Future.successful(None))
      val service = new LaboratoryServiceImpl(repo, labCode)

      Await.result(service.get("NOPE"), duration) mustBe None
    }
  }

  "LaboratoryServiceImpl add/update" should {

    "propagate Right(code) from the repository on add (C1)" in {
      val repo = mock[LaboratoryRepository]
      val l    = lab("LAB1")
      when(repo.add(l)).thenReturn(Future.successful[Either[String, String]](Right("LAB1")))
      val service = new LaboratoryServiceImpl(repo, labCode)

      Await.result(service.add(l), duration) mustBe Right("LAB1")
    }

    "propagate Left(error) from the repository on update (C1)" in {
      val repo = mock[LaboratoryRepository]
      val l    = lab("LAB1")
      when(repo.update(l)).thenReturn(Future.successful[Either[String, String]](Left("Código de laboratorio ya utilizado")))
      val service = new LaboratoryServiceImpl(repo, labCode)

      Await.result(service.update(l), duration) mustBe Left("Código de laboratorio ya utilizado")
    }
  }
}
