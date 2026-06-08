package unit.pedigree

import matching.{Algorithm, MatchingProfile, MatchStatus, MongoId, NewMatchingResult, Stringency}
import matching.MatchResult
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pedigree.*
import profile.*
import profile.GenotypificationByType.GenotypificationByType
import types.{AlphanumericId, MongoDate, SampleCode}

import java.util.Date

/**
 * Unit tests for the pure helpers in `pedigree.PedigreeMatchingAlgorithm`.
 *
 * `findCompatibilityMatches` is intentionally left out — it integrates with the
 * Bayesian network and requires substantial setup; see Bundle C review notes.
 */
class PedigreeMatchingAlgorithmTest extends AnyWordSpec with Matchers:

  // ─── shared fixtures ──────────────────────────────────────────────────────

  private val catIR = AlphanumericId("IR")

  private val genoType1: GenotypificationByType = Map(
    1 -> Map("CSF1PO" -> List(Allele(10), Allele(11)),
             "TH01"   -> List(Allele(7),  Allele(9)))
  )

  private def makeProfile(
    code: String,
    geno: GenotypificationByType = genoType1
  ): Profile =
    Profile(
      _id = SampleCode(code),
      globalCode = SampleCode(code),
      internalSampleCode = code,
      assignee = "tester",
      categoryId = catIR,
      genotypification = geno,
      analyses = None,
      labeledGenotypification = None,
      contributors = None,
      mismatches = None,
      matchingRules = None,
      associatedTo = None,
      deleted = false,
      matcheable = true,
      isReference = false,
      processed = false
    )

  // ─── extractMarker ────────────────────────────────────────────────────────

  "PedigreeMatchingAlgorithm.extractMarker" must {

    "return the marker between the first and last underscore" in {
      PedigreeMatchingAlgorithm.extractMarker("_CSF1PO_") mustBe "CSF1PO"
    }

    "use the FIRST '_' and the LAST '_' as boundaries" in {
      PedigreeMatchingAlgorithm.extractMarker("_a_b_c_") mustBe "a_b_c"
    }

    "return empty string when no underscore is present" in {
      PedigreeMatchingAlgorithm.extractMarker("noseparator") mustBe ""
    }

    "handle a header with internal underscores" in {
      PedigreeMatchingAlgorithm.extractMarker("_D3S1358_extra_") mustBe "D3S1358_extra"
    }
  }

  // ─── getMarkers ───────────────────────────────────────────────────────────

  "PedigreeMatchingAlgorithm.getMarkers" must {

    "wrap type-1 markers with underscores" in {
      val markers = PedigreeMatchingAlgorithm.getMarkers(makeProfile("AR-B-PED-1")).toSet
      markers mustBe Set("_CSF1PO_", "_TH01_")
    }

    "return Nil when the profile has no type-1 genotypification" in {
      val noType1 = makeProfile("AR-B-PED-2", geno = Map(4 -> Map("HV1" -> List(Mitocondrial('A', 1)))))
      PedigreeMatchingAlgorithm.getMarkers(noType1) mustBe Nil
    }

    "return Nil for an empty type-1 marker map" in {
      val emptyType1 = makeProfile("AR-B-PED-3", geno = Map(1 -> Map.empty))
      PedigreeMatchingAlgorithm.getMarkers(emptyType1) mustBe Nil
    }
  }

  // ─── filterGeno ───────────────────────────────────────────────────────────

  "PedigreeMatchingAlgorithm.filterGeno" must {

    "keep only type-1 markers present in the provided set" in {
      val result = PedigreeMatchingAlgorithm.filterGeno(genoType1, Set("CSF1PO"))
      result(1).keySet mustBe Set("CSF1PO")
    }

    "drop all type-1 markers when the set is empty" in {
      val result = PedigreeMatchingAlgorithm.filterGeno(genoType1, Set.empty)
      result(1) mustBe empty
    }

    "leave other genotypification types untouched" in {
      val mixed: GenotypificationByType = genoType1 ++ Map(
        4 -> Map("HV1" -> List(Mitocondrial('A', 1)))
      )
      val result = PedigreeMatchingAlgorithm.filterGeno(mixed, Set("CSF1PO"))
      // type 1 filtered to CSF1PO only; type 4 preserved verbatim
      result(1).keySet mustBe Set("CSF1PO")
      result(4) mustBe mixed(4)
    }
  }

  // ─── filterGenotipification ───────────────────────────────────────────────

  "PedigreeMatchingAlgorithm.filterGenotipification" must {

    def plain(header: String): PlainCPT2 =
      PlainCPT2(Array(header, "p"), Array(Array(0.5, 0.5)))

    def pg(headers: Array[String]): PedigreeGenotypification =
      PedigreeGenotypification(
        _id = 1L,
        genotypification = headers.map(plain),
        boundary = 1.0,
        frequencyTable = "ARGENTINA",
        unknowns = Array("PI")
      )

    "keep only PlainCPTs whose header substring matches one of the marker tokens" in {
      val input = pg(Array("_CSF1PO_", "_D3S1358_", "_TH01_"))
      val result = PedigreeMatchingAlgorithm.filterGenotipification(input, List("_CSF1PO_", "_TH01_"))
      result.genotypification.map(_.header.head).toSet mustBe Set("_CSF1PO_", "_TH01_")
    }

    "drop all PlainCPTs when no marker matches" in {
      val input = pg(Array("_CSF1PO_", "_D3S1358_"))
      val result = PedigreeMatchingAlgorithm.filterGenotipification(input, List("_NOTHING_"))
      result.genotypification mustBe empty
    }

    "preserve non-genotypification fields of PedigreeGenotypification" in {
      val input = pg(Array("_CSF1PO_"))
      val result = PedigreeMatchingAlgorithm.filterGenotipification(input, List("_CSF1PO_"))
      result.boundary mustBe input.boundary
      result.frequencyTable mustBe input.frequencyTable
      result.unknowns.toList mustBe input.unknowns.toList
      result._id mustBe input._id
    }
  }

  // ─── matchToPedigreeMatch ─────────────────────────────────────────────────

  "PedigreeMatchingAlgorithm.matchToPedigreeMatch" must {

    val profileP = makeProfile("AR-B-PED-100")
    val profileQ = makeProfile("AR-B-PED-200")

    def mr(): MatchResult = MatchResult(
      _id = MongoId("5e8f8f8f8f8f8f8f8f8f8f99"),
      matchingDate = MongoDate(new Date()),
      `type` = 1,
      leftProfile = MatchingProfile(profileP.globalCode, profileP.assignee, MatchStatus.pending, None, catIR),
      rightProfile = MatchingProfile(profileQ.globalCode, profileQ.assignee, MatchStatus.pending, None, catIR),
      result = NewMatchingResult(
        stringency = Stringency.ModerateStringency,
        matchingAlleles = Map.empty,
        totalAlleles = 0,
        categoryId = catIR,
        leftPonderation = 1.0,
        rightPonderation = 1.0,
        algorithm = Algorithm.ENFSI
      ),
      n = 0L,
      superiorProfileInfo = None,
      idCourtCase = Some(42L),
      lr = 0.0,
      mismatches = 0
    )

    "return None when the input MatchResult is None" in {
      val out = PedigreeMatchingAlgorithm.matchToPedigreeMatch(
        matchResult = None,
        p = profileP,
        q = profileQ,
        idPedigree = 7L,
        assignee = "ana",
        alias = "PI",
        locusRangeMap = Map.empty
      )
      out mustBe None
    }

    "return Some(PedigreeDirectLinkMatch) carrying the input result and the profile/pedigree wrappers" in {
      val out = PedigreeMatchingAlgorithm.matchToPedigreeMatch(
        matchResult = Some(mr()),
        p = profileP,
        q = profileQ,
        idPedigree = 7L,
        assignee = "ana",
        alias = "PI",
        locusRangeMap = Map.empty
      )
      out mustBe defined
      val link = out.get.asInstanceOf[PedigreeDirectLinkMatch]
      link.profile.globalCode mustBe profileP.globalCode
      link.pedigree.idPedigree mustBe 7L
      link.pedigree.assignee mustBe "ana"
      link.pedigree.unknown mustBe NodeAlias("PI")
      link.pedigree.globalCode mustBe profileQ.globalCode
      link.pedigree.caseType mustBe "MPI"
      link.pedigree.idCourtCase mustBe 42L
      link.kind mustBe PedigreeMatchKind.DirectLink
    }
  }
