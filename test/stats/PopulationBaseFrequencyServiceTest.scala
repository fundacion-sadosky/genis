package stats

import kits.{FullLocus, StrKitService}

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import pedigree.MutationService
import probability.ProbabilityModel
import services.CacheService
import services.TemporaryFreqDbKey
import specs.PdgSpec
import stubs.Stubs

class PopulationBaseFrequencyServiceTest extends PdgSpec with MockitoSugar {

  val duration = Duration(10, SECONDS)

  val mapLocus = Map(
    "CSF1PO" -> "CSF1PO",
    "D13S317" -> "D13S317",
    "D16S539" -> "D16S539",
    "D18S51" -> "D18S51", 
    "D21S11" -> "D21S11", 
    "D3S1358" -> "D3S1358",
    "D5S818" -> "D5S818", 
    "D7S820" -> "D7S820", 
    "D8S1179" -> "D8S1179",
    "FGA" -> "FGA", 
    "PentaD" -> "PentaD", 
    "Penta D" -> "PentaD", 
    "Penta_D" -> "PentaD", 
    "PentaE" -> "PentaE",
    "Penta E" -> "PentaE",
    "Penta_E" -> "PentaE",
    "TH01" -> "TH01", 
    "TPOX" -> "TPOX", 
    "vWA" -> "vWA")
  
  "PopulationBaseFrequencyService" must {

    "parse a csv" in {

      val csvFile = new java.io.File("test/resources/shdg_ar_09_norm.csv")

      val mockCacheService = mock[CacheService]
      doNothing.when(mockCacheService).set(any[TemporaryFreqDbKey], any[PopulationBaseFrequency])
      
      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.add(any[PopulationBaseFrequency])).thenReturn(Future.successful(Some(245)))

      val mockStrKitService = mock[StrKitService]
      when(mockStrKitService.getLocusAlias).thenReturn(Future.successful(mapLocus))
      
      val target = new PopulationBaseFrequencyImpl(mockCacheService,null,mockStrKitService)

      val r = Await.result(target.parseFile("db1", 0.0, ProbabilityModel.HardyWeinberg, csvFile),duration)
      
      assert(r.status != "Invalid", r.errors.mkString("\n"))
      r.status mustBe "Incomplete"
      r.key.isDefined mustBe true
      r.loci.get.length mustBe 15
      r.inserts.isEmpty mustBe true
      r.errors.length mustBe 0
    }

    "parse a csv with different format" in {

      val csvFile = new java.io.File("test/resources/base_frequency_format.csv")

      val mockCacheService = mock[CacheService]
      doNothing.when(mockCacheService).set(any[TemporaryFreqDbKey], any[PopulationBaseFrequency])

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.add(any[PopulationBaseFrequency])).thenReturn(Future.successful(Some(245)))

      val mockStrKitService = mock[StrKitService]
      when(mockStrKitService.getLocusAlias).thenReturn(Future.successful(mapLocus))

      val target = new PopulationBaseFrequencyImpl(mockCacheService,null,mockStrKitService)

      val r = Await.result(target.parseFile("db1", 0.0, ProbabilityModel.HardyWeinberg, csvFile),duration)

      assert(r.status != "Invalid", r.errors.mkString("\n"))
      r.status mustBe "Incomplete"
      r.key.isDefined mustBe true
      r.loci.get.length mustBe 15
      r.inserts.isEmpty mustBe true
      r.errors.length mustBe 0
    }

    "parse a csv with duplicated alleles failure" in {

      val csvFile = new java.io.File("test/resources/base_frequency_duplicated_alleles.csv")

      val mockCacheService = mock[CacheService]
      doNothing.when(mockCacheService).set(any[TemporaryFreqDbKey], any[PopulationBaseFrequency])

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.add(any[PopulationBaseFrequency])).thenReturn(Future.successful(Some(245)))

      val mockStrKitService = mock[StrKitService]
      when(mockStrKitService.getLocusAlias).thenReturn(Future.successful(mapLocus))

      val target = new PopulationBaseFrequencyImpl(mockCacheService,null,mockStrKitService)

      val r = Await.result(target.parseFile("db1", 0.0, ProbabilityModel.HardyWeinberg, csvFile),duration)

      r.status mustBe "Invalid"
      r.key.isEmpty mustBe true
      r.loci.isEmpty mustBe true
      r.inserts.isEmpty mustBe true
      r.errors.length mustBe 1
    }
        
    "accept a PopulationBaseFrequency and return the number of insertions" in {
      val mutationService = mock[MutationService]
      when(mutationService.refreshAllKis()).thenReturn(Future.successful(()))
      val pbf = Stubs.populationBaseFrequency

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.add(pbf)).thenReturn(Future.successful(Some(pbf.base.size)))

      val target: PopulationBaseFrequencyService = new PopulationBaseFrequencyImpl(null,mockPopBaseFreqRepository,null,mutationService)
      val inserts = Await.result(target.save(pbf), duration)

      inserts mustBe pbf.base.size
    }

    "get a PopulationBaseFrequency by Name" in {

      val returnedPBF = Stubs.populationBaseFrequency

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.getByName(returnedPBF.name)).thenReturn(Future.successful(Some(returnedPBF)))

      val target: PopulationBaseFrequencyService = new PopulationBaseFrequencyImpl(null,mockPopBaseFreqRepository,null)
      val popBaseFreq = Await.result(target.getByNamePV(returnedPBF.name), duration)

      popBaseFreq.frequencys.length mustBe popBaseFreq.alleles.length
      popBaseFreq.frequencys.head.length mustBe popBaseFreq.markers.length
    }

    "get default PopulationBaseFrequencyNameView" in {

      val returnedPBF = Stubs.populationBaseFrequency

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.getAllNames()).thenReturn(
        Future.successful(Seq((returnedPBF.name, returnedPBF.theta, returnedPBF.model.toString(), true, true))))

      val target: PopulationBaseFrequencyService = new PopulationBaseFrequencyImpl(null,mockPopBaseFreqRepository,null)
      val default = Await.result(target.getDefault(), duration)

      default.isDefined mustBe true
    }

    "get default with no bases configured" in {

      val returnedPBF = Stubs.populationBaseFrequency

      val mockPopBaseFreqRepository = mock[PopulationBaseFrequencyRepository]
      when(mockPopBaseFreqRepository.getAllNames()).thenReturn(Future.successful(Seq()))

      val target: PopulationBaseFrequencyService = new PopulationBaseFrequencyImpl(null,mockPopBaseFreqRepository,null)
      val default = Await.result(target.getDefault(), duration)

      default mustBe None
    }
    
  }
}