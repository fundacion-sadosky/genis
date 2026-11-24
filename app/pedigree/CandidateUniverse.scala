package pedigree

import java.security.MessageDigest
import kits.AnalysisType
import play.api.libs.json.Json
import profile.{Profile, SingleBand}

/**
  * Universo de alelos del cotejo. La CPT persistida de la familia se
  * construyó sobre un universo (panel + alelos observados en la familia); un
  * candidato puede traer alelos que no están ahí, y esos marcadores hay que
  * rehacerlos con el universo ampliado en vez de descartarlos.
  */
object CandidateUniverse {

  /** Alelos numéricos del candidato por marcador. Sin el piso -1.0 (no es un
    * alelo: fuera de escalera y microvariante caen ahí) y sin marcadores de
    * banda única, que quedan fuera del cálculo por otra regla. */
  def candidateAlleles(profile: Profile, analysisType: AnalysisType): Map[String, List[Double]] =
    profile.genotypification.getOrElse(analysisType.id, Map.empty)
      .filterNot { case (_, alleles) => SingleBand.isSingleBand(alleles) }
      .map { case (marker, alleles) =>
        marker -> BayesianNetwork.transformAlleleValues(alleles.distinct.toArray).toList.filter(_ != -1.0).distinct.sorted
      }
      .filter(_._2.nonEmpty)

  /** Lo que el candidato trae y el universo no tiene, en los marcadores pedidos.
    * «Ausente» se mide con `allelesInN`, el mismo criterio que `getMarkersToFilter`. */
  def missingAlleles(
    profile: Profile,
    analysisType: AnalysisType,
    markers: Set[String],
    universe: Map[String, List[Double]]
  ): Map[String, List[Double]] =
    candidateAlleles(profile, analysisType)
      .filter { case (marker, _) => markers.contains(marker) }
      .map { case (marker, alleles) =>
        val known = BayesianNetwork.allelesInN(marker, universe)
        marker -> alleles.filterNot(known.contains)
      }
      .filter(_._2.nonEmpty)

  /** Unión por marcador, sin repetir y ordenada. */
  def extend(universe: Map[String, List[Double]], extra: Map[String, List[Double]]): Map[String, List[Double]] =
    extra.foldLeft(universe) { case (acc, (marker, alleles)) =>
      acc + (marker -> (acc.getOrElse(marker, Nil) ++ alleles).distinct.sorted)
    }

  /** Huella de la familia con que se construyó la CPT: estructura del
    * genograma, sexo de cada individuo (entra al modelo de mutación) y el
    * genotipo autosómico de cada individuo tipificado. Se lee
    * genotypification(1) fijo porque los pedigríes son autosómicos y
    * BayesianNetwork.getAlleles también fija el tipo 1. Independiente del
    * orden de los perfiles. */
  def familyFingerprint(profiles: Array[Profile], genogram: Array[Individual]): String = {
    val canonical = genogram.sortBy(_.alias.text).map { ind =>
      val genotype = ind.globalCode.flatMap(code => profiles.find(_.globalCode == code)).map { p =>
        p.genotypification.getOrElse(1, Map.empty).toList.sortBy(_._1).map {
          case (marker, alleles) => marker + "=" + alleles.map(a => Json.toJson(a).toString).mkString(",")
        }.mkString(";")
      }.getOrElse("")
      List(ind.alias.text, ind.idFather.map(_.text).getOrElse(""), ind.idMother.map(_.text).getOrElse(""),
        ind.sex.toString, ind.unknown.toString, ind.globalCode.map(_.text).getOrElse(""), genotype).mkString("|")
    }.mkString("\n")
    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }
}
