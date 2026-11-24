package profile

import types.SampleCode

/**
 * Asociaciones de un perfil mezcla con perfiles conocidos. Viven en labeledGenotypification:
 * la etiqueta es el código del perfil asociado y su valor, los marcadores que se descuentan.
 * Un perfil admite una asociación por tipo de análisis (autosómico, Cromosoma Y), con el mismo
 * perfil o con perfiles distintos; como cada tipo tiene sus marcadores, nunca se pisan.
 */
object AssociationLabels {

  type Labels = Profile.LabeledGenotypification

  def isProfile(label: Profile.MixLabel): Boolean = label.matches(SampleCode.validationRe.toString())

  private def associations(labels: Labels): Labels = labels.filterKeys(isProfile)

  /** Tipos de análisis que ya tienen asociación. */
  def analysisTypes(labels: Labels, typeOf: Profile.Marker => Int): Set[Int] =
    associations(labels).values.flatMap(_.keys).map(typeOf).toSet

  /**
   * Genotipo que el cotejo descuenta de la mezcla. Con etiquetas manuales, o con una sola
   * asociación, es el de la primera etiqueta, como siempre. Con una asociación por tipo de
   * análisis se juntan: cada comparación mira solo los marcadores de su tipo.
   */
  def discountGenotype(labels: Option[Labels]): Option[Profile.Genotypification] =
    labels.flatMap { l =>
      if (l.size > 1 && l.keys.forall(isProfile)) Some(l.values.reduce(_ ++ _))
      else l.values.toVector.lift(0)
    }

  /**
   * Suma a un perfil ya asociado lo que trae `incoming`. Devuelve las etiquetas resultantes y
   * los códigos con marcadores nuevos, o None si no se puede: no trae nada nuevo, lo nuevo no
   * es una asociación con un perfil, o es de un tipo de análisis que ya tiene asociación.
   */
  def add(existing: Labels, incoming: Labels, typeOf: Profile.Marker => Int): Option[(Labels, Seq[String])] = {
    val added = incoming
      .map { case (label, genotype) =>
        val known = existing.getOrElse(label, Map.empty)
        label -> genotype.filter { case (locus, alleles) => !known.get(locus).contains(alleles) }
      }
      .filter(_._2.nonEmpty)

    val taken = analysisTypes(existing, typeOf)
    val addedTypes = added.values.flatMap(_.keys).map(typeOf).toSet

    if (added.isEmpty || !added.keys.forall(isProfile) || addedTypes.exists(taken.contains)) {
      None
    } else {
      val merged = added.foldLeft(existing) { case (labels, (label, genotype)) =>
        labels + (label -> (labels.getOrElse(label, Map.empty) ++ genotype))
      }
      Some((merged, added.keys.toSeq))
    }
  }

  /**
   * Quita la asociación de un tipo de análisis. Devuelve las etiquetas que quedan y los códigos
   * de los perfiles que perdieron marcadores (vacío si no había asociación de ese tipo).
   */
  def remove(existing: Labels, analysisType: Int, typeOf: Profile.Marker => Int): (Labels, Seq[String]) = {
    val affected = associations(existing).filter(_._2.keys.exists(typeOf(_) == analysisType)).keys.toSeq
    val remaining = existing
      .map { case (label, genotype) =>
        if (affected.contains(label)) label -> genotype.filter { case (locus, _) => typeOf(locus) != analysisType }
        else label -> genotype
      }
      .filter(_._2.nonEmpty)
    (remaining, affected)
  }
}
