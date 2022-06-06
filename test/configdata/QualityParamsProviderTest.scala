package configdata

import specs.PdgSpec
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import stubs.Stubs
import scala.concurrent.Future
import types.AlphanumericId

class QualityParamsProviderTest extends PdgSpec with MockitoSugar {

  "QualityParamsProvider" must {

    "calculate the minLocusQuantityAllowedPerProfile" in {

      val target: QualityParamsProviderImpl = new QualityParamsProviderImpl()
      val result = target.minLocusQuantityAllowedPerProfile(Stubs.categoryMap.get(AlphanumericId("SOSPECHOSO")).get, Stubs.strKits(0))

      result mustBe 14
    }

    "calculate the maxAllelesPerLocus" in {

      val target: QualityParamsProviderImpl = new QualityParamsProviderImpl()

      val result = target.maxAllelesPerLocus(Stubs.categoryMap.get(AlphanumericId("SOSPECHOSO")).get, Stubs.strKits(0))

      result mustBe 2
    }

    "calculate the maxOverageDeviatedLociPerProfile" in {

      val target: QualityParamsProviderImpl = new QualityParamsProviderImpl()

      val result = target.maxOverageDeviatedLociPerProfile(Stubs.categoryMap.get(AlphanumericId("SOSPECHOSO")).get, Stubs.strKits(0))

      result mustBe 2
    }

  }
}