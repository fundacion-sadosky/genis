package fixtures

import kits._

object LocusFixtures:

  val locus1 = Locus(
    id = "LOCUS 1",
    name = "LOCUS 1",
    chromosome = Some("1"),
    minimumAllelesQty = 1,
    maximumAllelesQty = 2,
    analysisType = 1,
    required = true,
    minAlleleValue = Some(BigDecimal(5)),
    maxAlleleValue = Some(BigDecimal(30))
  )

  val locus2 = Locus(
    id = "LOCUS 2",
    name = "LOCUS 2",
    chromosome = Some("2"),
    minimumAllelesQty = 2,
    maximumAllelesQty = 3,
    analysisType = 1
  )

  val locus3NonAutosomal = Locus(
    id = "LOCUS 3",
    name = "LOCUS 3",
    chromosome = Some("X"),
    minimumAllelesQty = 1,
    maximumAllelesQty = 1,
    analysisType = 2
  )

  val link1 = LocusLink(locus = "LOCUS 2", factor = 0.5, distance = 10.0)

  val fullLocus1 = FullLocus(
    locus = locus1,
    alias = List.empty,
    links = List.empty
  )

  val fullLocus2 = FullLocus(
    locus = locus2,
    alias = List.empty,
    links = List.empty
  )

  val fullLocusWithAliasAndLinks = FullLocus(
    locus = locus1,
    alias = List("ALIAS1", "ALIAS2"),
    links = List(link1)
  )

  val fullLocus3NonAutosomal = FullLocus(
    locus = locus3NonAutosomal,
    alias = List.empty,
    links = List.empty
  )

  val fullLocusList: Seq[FullLocus] = Seq(fullLocus1, fullLocus2)
