package matching

/**
 * Utility for matching mitochondrial DNA bases including IUPAC ambiguity codes.
 */
object MtDnaUtils {

  val baseMtDnaWildcards: Map[Char, Set[Char]] = Map(
    'A' -> Set('A'),
    'T' -> Set('T'),
    'C' -> Set('C'),
    'G' -> Set('G'),
    'R' -> Set('A', 'G'),
    'Y' -> Set('C', 'T'),
    'S' -> Set('G', 'C'),
    'W' -> Set('A', 'T'),
    'K' -> Set('G', 'T'),
    'M' -> Set('A', 'C'),
    'B' -> Set('C', 'G', 'T'),
    'D' -> Set('A', 'G', 'T'),
    'H' -> Set('A', 'C', 'T'),
    'V' -> Set('A', 'C', 'G'),
    'N' -> Set('C', 'G', 'A', 'T'),
    '-' -> Set('-')
  )

  def baseMatch(base1: Char, base2: Char): Boolean =
    baseMtDnaWildcards
      .getOrElse(base1, Set.empty)
      .intersect(
        baseMtDnaWildcards
          .getOrElse(base2, Set.empty)
      )
      .nonEmpty
}
