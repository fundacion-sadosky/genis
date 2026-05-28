// Package `pedigree` (no `unit.pedigree`) para acceder a getMutationModelKit, que es
// `private[pedigree]` por testabilidad del algoritmo de cálculo de Ki.
package pedigree

import kits.LocusService
import org.apache.pekko.actor.ActorSystem
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import stats.{PopulationBaseFrequencyGrouppedByLocus, PopulationBaseFrequencyService}

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class MutationServiceImplTest extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  private val duration = Duration(10, SECONDS)
  private val actorSystem = ActorSystem("MutationServiceImplTest")

  // Mismos defaults que conf/application.conf (bloque mt.defaultMutation*)
  private val defaultRateI            = "0.001628"
  private val defaultRateF            = "0.000463"
  private val defaultRateM            = "0.001584"
  private val defaultRange            = "0.5"
  private val defaultRateMicrovariant = "0.0000005"

  private def mkService(
    repo: MutationRepository = mock[MutationRepository],
    locus: LocusService = mock[LocusService],
    pbf: PopulationBaseFrequencyService = mock[PopulationBaseFrequencyService]
  ): MutationServiceImpl =
    new MutationServiceImpl(
      repo,
      defaultRateI,
      defaultRateF,
      defaultRateM,
      defaultRange,
      defaultRateMicrovariant,
      pbf,
      locus,
      actorSystem
    )

  private def expectedKi(rate: String, sum: Double): BigDecimal =
    (BigDecimal(rate) / sum).setScale(8, RoundingMode.HALF_EVEN)

  private def param(rate: String = "0.001", range: String = "0.5"): MutationModelParameter =
    MutationModelParameter(
      id = 1L,
      idMutationModel = 1L,
      locus = "D3S1358",
      sex = "F",
      mutationRate = Some(BigDecimal(rate)),
      mutationRange = Some(BigDecimal(range)),
      mutationRateMicrovariant = None
    )

  "MutationServiceImpl.getMutationModelKit (algoritmo de Ki)" must {

    "retornar lista vacía si alleles es None" in {
      val res = Await.result(
        mkService().getMutationModelKit(param(), alleles = None, cantSaltos = 1L),
        duration
      )
      res mustBe Nil
    }

    "retornar lista vacía si alleles está vacío" in {
      val res = Await.result(
        mkService().getMutationModelKit(param(), alleles = Some(Nil), cantSaltos = 1L),
        duration
      )
      res mustBe Nil
    }

    "calcular Ki para 2 alelos vecinos con cantSaltos=1" in {
      // i=10: vecinos {11} (|11-10|=1<=1, entero) → sum = 0.5^1 = 0.5 → ki = 0.001 / 0.5 = 0.002
      // i=11: vecinos {10} (|10-11|=1<=1, entero) → sum = 0.5^1 = 0.5 → ki = 0.001 / 0.5 = 0.002
      val res = Await.result(
        mkService().getMutationModelKit(param(), Some(List(10.0, 11.0)), cantSaltos = 1L),
        duration
      )
      res must have size 2
      val expected = expectedKi("0.001", 0.5)
      res.find(_.allele == 10.0).get.ki mustBe expected
      res.find(_.allele == 11.0).get.ki mustBe expected
    }

    "cantSaltos=1 filtra alelos a distancia > 1" in {
      // i=10: vecinos válidos {11} (j=13 excluido por |13-10|=3 > 1)
      // sum = 0.5, ki = 0.002
      val res = Await.result(
        mkService().getMutationModelKit(param(), Some(List(10.0, 11.0, 13.0)), cantSaltos = 1L),
        duration
      )
      res.find(_.allele == 10.0).get.ki mustBe expectedKi("0.001", 0.5)
    }

    "cantSaltos=2 incluye alelos a distancia 2" in {
      // i=10: vecinos válidos {11, 12} (|11-10|=1 y |12-10|=2, ambos <= 2)
      // sum = 0.5^1 + 0.5^2 = 0.5 + 0.25 = 0.75
      // ki = 0.001 / 0.75
      val res = Await.result(
        mkService().getMutationModelKit(param(), Some(List(10.0, 11.0, 12.0)), cantSaltos = 2L),
        duration
      )
      res.find(_.allele == 10.0).get.ki mustBe expectedKi("0.001", 0.75)
    }

    "excluir microvariantes del cálculo (distancia no entera)" in {
      // i=10.0: candidatos j=10.3 (|0.3|%1=0.3 ≠ 0, EXCLUIDO), j=11.0 (|1|%1=0, INCLUIDO)
      // sum=0.5, ki=0.002
      val res = Await.result(
        mkService().getMutationModelKit(param(), Some(List(10.0, 10.3, 11.0)), cantSaltos = 1L),
        duration
      )
      res.find(_.allele == 10.0).get.ki mustBe expectedKi("0.001", 0.5)
      // i=10.3: ningún vecino entero a distancia <= 1 → sum=0, ki=0
      res.find(_.allele == 10.3).get.ki mustBe BigDecimal("0").setScale(8, RoundingMode.HALF_EVEN)
    }

    "retornar Ki=0 cuando sum=0 (alelo aislado)" in {
      // i=10: no hay vecinos válidos (lista de un elemento) → sum=0, ki=0
      val res = Await.result(
        mkService().getMutationModelKit(param(), Some(List(10.0)), cantSaltos = 1L),
        duration
      )
      res must have size 1
      res.head.ki mustBe BigDecimal("0").setScale(8, RoundingMode.HALF_EVEN)
    }

    "aplicar setScale(8, HALF_EVEN) al Ki resultante" in {
      // rate=0.000463, sum=0.5 → ki=0.000926, scale=8 → "0.00092600"
      val p = param(rate = "0.000463", range = "0.5")
      val res = Await.result(
        mkService().getMutationModelKit(p, Some(List(10.0, 11.0)), cantSaltos = 1L),
        duration
      )
      val ki = res.head.ki
      ki.scale mustBe 8
      ki mustBe BigDecimal("0.00092600")
    }
  }

  "MutationServiceImpl (delegación al repositorio)" must {

    "getMutationModel(None) retorna None sin tocar el repo" in {
      val repo = mock[MutationRepository]
      when(repo.getMutationModel(eqTo(None))).thenReturn(Future.successful(None))

      val res = Await.result(mkService(repo = repo).getMutationModel(None), duration)
      res mustBe None
    }

    "getMutationModel(Some(id)) con repo=None retorna None" in {
      val repo = mock[MutationRepository]
      when(repo.getMutationModel(eqTo(Some(7L)))).thenReturn(Future.successful(None))

      val res = Await.result(mkService(repo = repo).getMutationModel(Some(7L)), duration)
      res mustBe None
    }

    "getMutationModel(Some(id)) con repo=Some compone MutationModelFull con parámetros" in {
      val repo = mock[MutationRepository]
      val model = MutationModel(7L, "Stepwise", 2L, active = true, ignoreSex = false, cantSaltos = 1L)
      val params = List(
        MutationModelParameter(1L, 7L, "D3S1358", "F", Some(BigDecimal("0.001")), Some(BigDecimal("0.5")), None)
      )
      when(repo.getMutationModel(eqTo(Some(7L)))).thenReturn(Future.successful(Some(model)))
      when(repo.getMutatitionModelParameters(eqTo(7L))).thenReturn(Future.successful(params))

      val res = Await.result(mkService(repo = repo).getMutationModel(Some(7L)), duration)
      res mustBe Some(MutationModelFull(model, params))
    }

    "deleteMutationModelById retorna Right(()) SIN llamar al repo (bug legacy preservado)" in {
      val repo = mock[MutationRepository]

      val res = Await.result(mkService(repo = repo).deleteMutationModelById(42L), duration)

      res mustBe Right(())
      verify(repo, never).deleteMutationModelById(any[Long])
    }

    "getMutationModelData(None, _) retorna None sin tocar el repo" in {
      val repo = mock[MutationRepository]

      val res = Await.result(mkService(repo = repo).getMutationModelData(None, List("D3S1358")), duration)

      res mustBe None
      verify(repo, never).getMutatitionModelData(any[Long], any[Long], any)
    }

    "getMutationModelData(Some(m), markers) agrega el modelo a cada tupla" in {
      val repo = mock[MutationRepository]
      val model = MutationModel(7L, "Stepwise", 2L, active = true, ignoreSex = false, cantSaltos = 1L)
      val p = MutationModelParameter(1L, 7L, "D3S1358", "F", Some(BigDecimal("0.001")), Some(BigDecimal("0.5")), None)
      val kis = List(MutationModelKi(1L, 1L, 10.0, BigDecimal("0.002").setScale(8, RoundingMode.HALF_EVEN)))
      when(repo.getMutatitionModelData(eqTo(7L), eqTo(2L), any))
        .thenReturn(Future.successful(List((p, kis))))

      val res = Await.result(
        mkService(repo = repo).getMutationModelData(Some(model), List("D3S1358")),
        duration
      )
      res mustBe Some(List((p, kis, model)))
    }

    "saveLocusAlleles filtra alelos ya presentes en populationBaseFrequency" in {
      val repo = mock[MutationRepository]
      val pbf = mock[PopulationBaseFrequencyService]

      val existing = PopulationBaseFrequencyGrouppedByLocus(base = Map("D3S1358" -> List(10.0, 11.0)))
      when(pbf.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(existing))
      when(repo.insertLocusAlleles(any)).thenReturn(Future.successful(Right(1)))

      val service = mkService(repo = repo, pbf = pbf)
      val input = List(("D3S1358", 10.0), ("D3S1358", 12.0))  // 10.0 ya existe, 12.0 es nuevo

      Await.result(service.saveLocusAlleles(input), duration)

      val captor = ArgumentCaptor.forClass(classOf[List[(String, Double)]])
      verify(repo).insertLocusAlleles(captor.capture())
      captor.getValue mustBe List(("D3S1358", 12.0))
    }

    "refreshAllKisSecuential retorna inmediatamente con ()" in {
      val res = Await.result(mkService().refreshAllKisSecuential(), duration)
      res mustBe (())
    }

    "getAllPossibleAllelesByLocus retorna el mapa base del PopulationBaseFrequencyService" in {
      val pbf = mock[PopulationBaseFrequencyService]
      val grouped = PopulationBaseFrequencyGrouppedByLocus(base = Map("D3S1358" -> List(10.0, 11.0)))
      when(pbf.getAllPossibleAllelesByLocus()).thenReturn(Future.successful(grouped))

      val res = Await.result(mkService(pbf = pbf).getAllPossibleAllelesByLocus(), duration)
      res mustBe Map("D3S1358" -> List(10.0, 11.0))
    }

    "getAllMutationModelType delega al repo (legacy filtra solo Stepwise id=2L — verificado en repo)" in {
      val repo = mock[MutationRepository]
      when(repo.getAllMutationModelType())
        .thenReturn(Future.successful(List(MutationModelType(2L, "Stepwise"))))

      val res = Await.result(mkService(repo = repo).getAllMutationModelType(), duration)
      res mustBe List(MutationModelType(2L, "Stepwise"))
    }

    "getAllMutationDefaultParameters delega al repo" in {
      val repo = mock[MutationRepository]
      val defaults = List(MutationDefaultParam("D3S1358", "F", Some(BigDecimal("0.001"))))
      when(repo.getAllMutationDefaultParameters()).thenReturn(Future.successful(defaults))

      val res = Await.result(mkService(repo = repo).getAllMutationDefaultParameters(), duration)
      res mustBe defaults
    }
  }
}
