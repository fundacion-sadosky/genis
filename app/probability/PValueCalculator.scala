package probability

import profile.Profile.Marker
import stats.{PopulationBaseFrequency, PopulationSampleFrequency}

object PValueCalculator {

  type FrequencyTable = Map[(Marker, BigDecimal), Double]

  def parseFrequencyTable(populationBaseFrequency: PopulationBaseFrequency): FrequencyTable = {

    var minimumValuesPerMarker: Map[Marker, Double] = Map()

    val list = populationBaseFrequency.base.map { entry: PopulationSampleFrequency =>
      if (entry.allele == -1) {
        minimumValuesPerMarker += ((entry.marker, entry.frequency.toDouble))
      }

      ((entry.marker, BigDecimal(entry.allele)), entry.frequency.toDouble)
    }

    val listWithMinimumValues = list.map { entry =>
      (entry._1, math.max(entry._2, minimumValuesPerMarker(entry._1._1)))
    }

    listWithMinimumValues.toMap[(String, BigDecimal), Double]
  }

}
