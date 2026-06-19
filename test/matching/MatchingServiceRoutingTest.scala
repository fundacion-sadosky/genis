package matching

import org.mockito.Mockito.{never, verify}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import pedigree.PedigreeSparkMatcher
import types.SampleCode

/**
 * Gap DVI (#280): MatchingServiceSpark.findMatches(globalCode, matchType) ruteaba
 * None -> Spark2Matcher y Some("MPI") -> PedigreeSparkMatcher, pero Some("DVI")
 * caía en `case _ => ()` (no-op). Como la aceptación bloquea esperando un estado
 * del matcher, un perfil DVI colgaba la aceptación. DVI debe rutearse al
 * PedigreeSparkMatcher igual que MPI.
 */
class MatchingServiceRoutingTest extends PlaySpec with MockitoSugar {

  private def buildTarget(spark2Matcher: Spark2Matcher,
                          pedigreeSparkMatcher: PedigreeSparkMatcher): MatchingServiceSparkImpl =
    new MatchingServiceSparkImpl(
      null, null, null, null, null, null,
      spark2Matcher, null, pedigreeSparkMatcher,
      null, null, null, null, null)

  val gc = SampleCode("AR-B-IMBICE-500")

  "findMatches(globalCode, matchType)" should {

    "route a DVI category to the pedigree matcher" in {
      val spark2Matcher = mock[Spark2Matcher]
      val pedigreeSparkMatcher = mock[PedigreeSparkMatcher]

      buildTarget(spark2Matcher, pedigreeSparkMatcher).findMatches(gc, Some("DVI"))

      verify(pedigreeSparkMatcher).findMatchesInBackGround(gc, "DVI")
      verify(spark2Matcher, never()).findMatchesInBackGround(gc)
    }

    "route an MPI category to the pedigree matcher" in {
      val spark2Matcher = mock[Spark2Matcher]
      val pedigreeSparkMatcher = mock[PedigreeSparkMatcher]

      buildTarget(spark2Matcher, pedigreeSparkMatcher).findMatches(gc, Some("MPI"))

      verify(pedigreeSparkMatcher).findMatchesInBackGround(gc, "MPI")
      verify(spark2Matcher, never()).findMatchesInBackGround(gc)
    }

    "route a non-pedigree (None) category to the direct matcher" in {
      val spark2Matcher = mock[Spark2Matcher]
      val pedigreeSparkMatcher = mock[PedigreeSparkMatcher]

      buildTarget(spark2Matcher, pedigreeSparkMatcher).findMatches(gc, None)

      verify(spark2Matcher).findMatchesInBackGround(gc)
    }
  }
}
