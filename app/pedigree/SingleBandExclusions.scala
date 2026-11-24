package pedigree

import kits.AnalysisType
import profile.{AlleleValue, Profile, SingleBand}

/**
  * Lo que el motor de parentesco necesita saber de la banda única. Espeja los
  * dos lectores de BayesianNetwork: ofQueryProfile a getQueryProfileAlleles,
  * byIndividual a getAlleles (que lee el tipo de análisis 1).
  */
object SingleBandExclusions {

  /** Marcadores de banda única del candidato, restringidos a la tabla. */
  def ofQueryProfile(
    p: Profile,
    analysisType: AnalysisType,
    frequencyTable: BayesianNetwork.FrequencyTable
  ): Set[Profile.Marker] = {
    val strs = p.genotypification.getOrElse(analysisType.id, Map.empty[Profile.Marker, List[AlleleValue]])
    SingleBand.markers(strs).filter(frequencyTable.contains).toSet
  }

  /** alias -> marcadores de banda única, para los individuos tipificados y no
    * consultados del genograma, restringido a los marcadores de la tabla. */
  def byIndividual(
    profiles: Array[Profile],
    genogram: Array[Individual],
    frequencyTable: BayesianNetwork.FrequencyTable
  ): Map[String, List[Profile.Marker]] = {
    genogram
      .filterNot(_.unknown)
      .flatMap { individual =>
        for {
          code <- individual.globalCode
          p <- profiles.find(_.globalCode == code)
          strs = p.genotypification.getOrElse(1, Map.empty[Profile.Marker, List[AlleleValue]])
          markers = SingleBand.markers(strs).filter(frequencyTable.contains)
          if markers.nonEmpty
        } yield individual.alias.text -> markers
      }
      .toMap
  }

  /** Un detalle singleBand con LR 1 por marcador excluido del candidato. */
  def details(markers: Set[Profile.Marker]): Map[Profile.Marker, MarkerLRDetail] =
    markers.map(m => m -> MarkerLRDetail(1.0, MarkerLRDetail.SingleBand)).toMap

  /** Vuelca los alias de los familiares excluidos en los marcadores ya
    * evaluados. No agrega marcadores: uno que el candidato no tiene no afecta
    * ningún número. */
  def withFamily(
    details: Map[Profile.Marker, MarkerLRDetail],
    family: Map[String, List[Profile.Marker]]
  ): Map[Profile.Marker, MarkerLRDetail] = {
    val byMarker: Map[Profile.Marker, List[String]] = family
      .toList
      .flatMap { case (alias, markers) => markers.map(_ -> alias) }
      .groupBy(_._1)
      .map { case (marker, pairs) => marker -> pairs.map(_._2).sorted }
    details.map {
      case (marker, detail) =>
        marker -> detail.copy(singleBandIndividuals = byMarker.getOrElse(marker, Nil))
    }
  }
}
