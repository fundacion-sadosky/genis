package unit.stats

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import play.api.cache.AsyncCacheApi

import stats.*

class PopulationBaseFrequencyServiceTest extends AnyWordSpec with Matchers with MockitoSugar:

  val duration: Duration = 10.seconds

  // Mapa de alias de locus empleado en las pruebas (los mismos del legacy)
  val mapLocus: Map[String, String] = Map(
    "CSF1PO"  -> "CSF1PO",
    "D13S317" -> "D13S317",
    "D16S539" -> "D16S539",
    "D18S51"  -> "D18S51",
    "D21S11"  -> "D21S11",
    "D3S1358" -> "D3S1358",
    "D5S818"  -> "D5S818",
    "D7S820"  -> "D7S820",
    "D8S1179" -> "D8S1179",
    "FGA"     -> "FGA",
    "PentaD"  -> "PentaD",
    "Penta D" -> "PentaD",
    "Penta_D" -> "PentaD",
    "PentaE"  -> "PentaE",
    "Penta E" -> "PentaE",
    "Penta_E" -> "PentaE",
    "TH01"    -> "TH01",
    "TPOX"    -> "TPOX",
    "vWA"     -> "vWA"
  )

  // Helper: datos de prueba
  val samplePsf: Seq[PopulationSampleFrequency] = Seq(
    PopulationSampleFrequency("TPOX", 5,  BigDecimal("0.00016")),
    PopulationSampleFrequency("TPOX", 6,  BigDecimal("0.00261")),
    PopulationSampleFrequency("TPOX", 7,  BigDecimal("0.00142")),
    PopulationSampleFrequency("FGA",  18, BigDecimal("0.10000")),
    PopulationSampleFrequency("FGA",  20, BigDecimal("0.05000"))
  )

  val samplePbf: PopulationBaseFrequency =
    PopulationBaseFrequency("testBase", 0.0, ProbabilityModel.HardyWeinberg, samplePsf)

  // Mocks reutilizables
  def mockCache: AsyncCacheApi =
    val m = mock[AsyncCacheApi]
    when(m.set(any[String], any[Object], any)).thenReturn(Future.successful(()))
    when(m.get[PopulationBaseFrequency](any[String])(using any)).thenReturn(Future.successful(None))
    m

  "PopulationBaseFrequencyService" when {

    "save" should {
      "return the number of insertions" in {
        val mockRepo = mock[PopulationBaseFrequencyRepository]
        when(mockRepo.add(samplePbf)).thenReturn(Future.successful(Some(samplePsf.size)))

        val svc = PopulationBaseFrequencyServiceImpl(mockCache, mockRepo, null)
        Await.result(svc.save(samplePbf), duration) mustBe samplePsf.size
      }
    }

    "parseFile" should {
      "return Incomplete status when CSV has no Fmin row" in {
        val csvFile = new java.io.File("test/resources/shdg_ar_09_norm.csv")
        assume(csvFile.exists(), "CSV de test no encontrado — asegurate de correr los tests desde la raíz del proyecto")

        val mockRepo      = mock[PopulationBaseFrequencyRepository]
        val mockStrKitSvc = mock[kits.StrKitService]
        val mockCacheApi  = mockCache
        when(mockStrKitSvc.getLocusAlias).thenReturn(Future.successful(mapLocus))
        // set debería capturar la PBF temporal
        when(mockCacheApi.set(any[String], any[Object], any)).thenReturn(Future.successful(()))

        val svc = PopulationBaseFrequencyServiceImpl(mockCacheApi, mockRepo, mockStrKitSvc)
        val r   = Await.result(svc.parseFile("db1", 0.0, ProbabilityModel.HardyWeinberg, csvFile), duration)

        r.status          mustBe "Incomplete"
        r.key.isDefined   mustBe true
        r.loci.get.length mustBe 15
        r.errors          mustBe empty
      }

      "return Invalid status when CSV has duplicated alleles" in {
        val csvFile = new java.io.File("test/resources/base_frequency_duplicated_alleles.csv")
        assume(csvFile.exists())

        val mockStrKitSvc = mock[kits.StrKitService]
        when(mockStrKitSvc.getLocusAlias).thenReturn(Future.successful(mapLocus))

        val svc = PopulationBaseFrequencyServiceImpl(mockCache, null, mockStrKitSvc)
        val r   = Await.result(svc.parseFile("db1", 0.0, ProbabilityModel.HardyWeinberg, csvFile), duration)

        r.status         mustBe "Invalid"
        r.errors.length  mustBe 1
      }
    }

    "getAllNames" should {
      "return mapped views from repository" in {
        val mockRepo = mock[PopulationBaseFrequencyRepository]
        when(mockRepo.getAllNames()).thenReturn(
          Future.successful(Seq((samplePbf.name, samplePbf.theta, samplePbf.model.toString, true, true)))
        )

        val svc  = PopulationBaseFrequencyServiceImpl(mockCache, mockRepo, null)
        val list = Await.result(svc.getAllNames(), duration)

        list.length     mustBe 1
        list.head.name  mustBe samplePbf.name
        list.head.default mustBe true
      }
    }

    "getDefault" should {
      "return Some when there is a default base" in {
        val mockRepo = mock[PopulationBaseFrequencyRepository]
        when(mockRepo.getAllNames()).thenReturn(
          Future.successful(Seq((samplePbf.name, samplePbf.theta, samplePbf.model.toString, true, true)))
        )

        val svc = PopulationBaseFrequencyServiceImpl(mockCache, mockRepo, null)
        Await.result(svc.getDefault(), duration).isDefined mustBe true
      }

      "return None when there are no bases configured" in {
        val mockRepo = mock[PopulationBaseFrequencyRepository]
        when(mockRepo.getAllNames()).thenReturn(Future.successful(Seq.empty))

        val svc = PopulationBaseFrequencyServiceImpl(mockCache, mockRepo, null)
        Await.result(svc.getDefault(), duration) mustBe None
      }
    }

    "getByNamePV" should {
      "return a correctly structured view" in {
        val mockRepo = mock[PopulationBaseFrequencyRepository]
        when(mockRepo.getByName(samplePbf.name)).thenReturn(Future.successful(Some(samplePbf)))

        val svc  = PopulationBaseFrequencyServiceImpl(mockCache, mockRepo, null)
        val view = Await.result(svc.getByNamePV(samplePbf.name), duration)

        view.frequencys.length mustBe view.alleles.length
        view.frequencys.headOption.map(_.length) mustBe Some(view.markers.length)
      }
    }
  }
