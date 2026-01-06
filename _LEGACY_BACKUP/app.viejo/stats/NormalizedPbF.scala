package stats

case class NormalizedPbF(
    markers: List[String], 
    alleles: List[Double],
    freqs: List[List[Option[BigDecimal]]],
    fmins: List[BigDecimal])

