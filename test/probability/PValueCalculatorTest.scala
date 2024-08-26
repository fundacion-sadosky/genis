package probability

import configdata._
import laboratories.LaboratoryService
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import profile.{Allele, Analysis}
import profiledata.ProfileDataService
import specs.PdgSpec
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}
import stubs.Stubs
import types.{AlphanumericId, SampleCode, StatOption}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class PValueCalculatorTest extends PdgSpec with MockitoSugar {
  val duration = Duration(10, SECONDS)

  val seqPopulation = List(
    PopulationSampleFrequency("TPOX",10,BigDecimal(0.1)),
    PopulationSampleFrequency("TPOX",11,BigDecimal(0.3)),
    PopulationSampleFrequency("TPOX",-1,BigDecimal(0.2)),
    PopulationSampleFrequency("D21S11",-1,BigDecimal(0.01)),
    PopulationSampleFrequency("D21S11",36.2,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",37,BigDecimal(0.1))
  )

  val populationBaseFrequency = new PopulationBaseFrequency("A", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

  "PValue calculator" should {
    "parse frequency table with minimum value" in {
      val result = PValueCalculator.parseFrequencyTable(populationBaseFrequency)

      result(("TPOX", 10)) mustBe 0.2
      result(("TPOX", 11)) mustBe 0.3
      result(("D21S11", 36.2)) mustBe 0.01
      result(("D21S11", 37)) mustBe 0.1
    }
  }

}
