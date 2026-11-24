package profile

/**
  * Regla de banda única: en un locus autosómico de referencia, un homocigoto
  * se escribe [a,a]; [a] no es un dato válido.
  */
object SingleBand {

  /** Exactamente un elemento, y numérico. XY (AMEL) y mitocondrial no cuentan. */
  def isSingleBand(alleles: Seq[AlleleValue]): Boolean = alleles match {
    case Seq(Allele(_)) => true
    case Seq(OutOfLadderAllele(_, _)) => true
    case Seq(MicroVariant(_)) => true
    case _ => false
  }

  /** Marcadores con banda única, ordenados. Sin filtro por locus: es la
    * variante del motor de parentesco. */
  def markers(genotypification: Profile.Genotypification): List[Profile.Marker] =
    genotypification
      .collect { case (marker, alleles) if isSingleBand(alleles) => marker }
      .toList
      .sorted

  /** Sólo los loci con mínimo de alelos >= 2. Un locus que no está en el mapa
    * se trata como mínimo 2. Es la variante de la carga. */
  def markers(
    genotypification: Profile.Genotypification,
    minimumAlleles: Map[Profile.Marker, Int]
  ): List[Profile.Marker] =
    markers(genotypification).filter(m => minimumAlleles.getOrElse(m, 2) >= 2)
}
