package matching

import java.util.Date

import configdata.MatchingRule
import controllers.Matching
import matching.MatchingAlgorithm._
import matching.Stringency._
import org.bson.types.ObjectId
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar
import org.specs2.mutable.Specification
import pedigree._
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import probability.{CalculationTypeService, ProbabilityService}
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile._
import profile._
import stubs.Stubs
import stubs.Stubs.{analyses, catA1, genotypification}
import types._
import user.UserService

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.math.BigDecimal.int2bigDecimal

class MatchingTest extends Specification with MockitoSugar with Results {

  val duration = Duration(100, SECONDS)

  val analyses: Option[List[Analysis]] = stubs.Stubs.analyses
  "Mithocondrial " should {
    "profiles without genotipification" in {

      val profiles = List(Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "Unknown", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-2"), SampleCode("AR-C-SHDG-2"), "FatherCode", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-3"), SampleCode("AR-C-SHDG-3"), "MotherCode", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-4"), SampleCode("AR-C-SHDG-4"), "GrandmotherCode", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false)
      )
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"), None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None))

      val result = MatchingAlgorithm.getMtProfile(individuals,profiles)
      result.isDefined must beEqualTo(false)
    }
    "profiles with genotipification" in {
      val genotypificationMt: GenotypificationByType = Map( 4 -> Map(
        "HV1" -> List(Mitocondrial('A', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3))))
      val profiles = List(Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "Unknown", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-2"), SampleCode("AR-C-SHDG-2"), "FatherCode", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-3"), SampleCode("AR-C-SHDG-3"), "MotherCode", "",catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-4"), SampleCode("AR-C-SHDG-4"), "GrandmotherCode", "",catA1.id, genotypificationMt, analyses, None, None, None, None, None, false, true, false)
      )
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"), None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None))

      val result = MatchingAlgorithm.getMtProfile(individuals,profiles)
      result.isDefined must beEqualTo(true)
      result.get.internalSampleCode must beEqualTo("GrandmotherCode")
    }
    "brother" in {
      val genotypificationMt: GenotypificationByType = Map( 4 -> Map(
        "HV1" -> List(Mitocondrial('A', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3))))
      val profiles = List(
        Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "Unknown", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-2"), SampleCode("AR-C-SHDG-2"), "FatherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-3"), SampleCode("AR-C-SHDG-3"), "MotherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-4"), SampleCode("AR-C-SHDG-4"), "GrandmotherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-5"), SampleCode("AR-C-SHDG-5"), "Brother", "",catA1.id,
          genotypificationMt, analyses, None, None, None, None, None, false, true, false)
      )
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"), None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None),
        Individual(NodeAlias("Brother"), None, Some(NodeAlias("Mother")), Sex.Female, Some(SampleCode("AR-C-SHDG-5")), true, None)
        )

      val result = MatchingAlgorithm.getMtProfile(individuals,profiles)
      result.isDefined must beEqualTo(true)
      result.get.internalSampleCode must beEqualTo("Brother")
    }
    "uncle" in {
      val genotypificationMt: GenotypificationByType = Map( 4 -> Map(
        "HV1" -> List(Mitocondrial('A', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3))))
      val profiles = List(
        Profile(SampleCode("AR-C-SHDG-1"), SampleCode("AR-C-SHDG-1"), "Unknown", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-2"), SampleCode("AR-C-SHDG-2"), "FatherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-3"), SampleCode("AR-C-SHDG-3"), "MotherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-4"), SampleCode("AR-C-SHDG-4"), "GrandmotherCode", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-5"), SampleCode("AR-C-SHDG-5"), "Brother", "",catA1.id,
          genotypification, analyses, None, None, None, None, None, false, true, false),
        Profile(SampleCode("AR-C-SHDG-6"), SampleCode("AR-C-SHDG-6"), "Uncle", "",catA1.id,
          genotypificationMt, analyses, None, None, None, None, None, false, true, false)
      )
      val individuals: Seq[Individual] = Seq(Individual(NodeAlias("PI"), Some(NodeAlias("Father")), Some(NodeAlias("Mother")), Sex.Unknown, Some(SampleCode("AR-C-SHDG-1")), true, None),
        Individual(NodeAlias("Mother"), None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-3")), true, None),
        Individual(NodeAlias("Father"), None, None, Sex.Male, Some(SampleCode("AR-C-SHDG-2")), true, None),
        Individual(NodeAlias("Grandmother"), None, None, Sex.Female, Some(SampleCode("AR-C-SHDG-4")), true, None),
        Individual(NodeAlias("Brother"), None, Some(NodeAlias("Mother")), Sex.Female, Some(SampleCode("AR-C-SHDG-5")), true, None),
        Individual(NodeAlias("Uncle"), None, Some(NodeAlias("Grandmother")), Sex.Female, Some(SampleCode("AR-C-SHDG-6")), true, None)
      )

      val result = MatchingAlgorithm.getMtProfile(individuals,profiles)
      result.isDefined must beEqualTo(true)
      result.get.internalSampleCode must beEqualTo("Uncle")
    }
  }
  "Java Mongo API  ObjectId " should {
    "be created from string withuot changing UUID" in {
      val srcOid = new ObjectId()
      val tgtOid = new ObjectId(srcOid.toString())
      tgtOid must beEqualTo(srcOid)

    }
  }

  "Matching ids" should {
    "be transformed from string to MongoId and ObjectId" in {
      val srcOid = new ObjectId()
      val mongoId = MongoId(srcOid.toString)
      val tgtOid = new ObjectId(mongoId.id)
      tgtOid must beEqualTo(srcOid)

    }
  }
  "convert alelles" should {
    "should convert" in {
     val converted = convertAleles(List(Allele(10),Allele(20)),"TH01", Map("TH01" -> AleleRange(11,15)))
     converted.toList.toString() must beEqualTo(List(OutOfLadderAllele(11,"<").toString,OutOfLadderAllele(15,">")).toString)

      val converted2 = convertAleles(List(Allele(12),Allele(14)),"TH01", Map("TH01" -> AleleRange(11,15)))
      converted2.toString must beEqualTo(Seq(Allele(12),Allele(14)).toString)

      val converted3 = convertAleles(List(Allele(8),Allele(14)),"TH01", Map("TH01" -> AleleRange(11,15)))
      converted3.toString must beEqualTo(Seq(OutOfLadderAllele(11,"<"),Allele(14)).toString)

      val converted4 = convertAleles(List(Allele(12),Allele(16)),"TH01", Map("TH01" -> AleleRange(11,15)))
      converted4.toString must beEqualTo(Seq(Allele(12),OutOfLadderAllele(15,">")).toString)

      val converted5 = convertAleles(List(Allele(11),Allele(15)),"TH01", Map("TH01" -> AleleRange(11,15)))
      converted5.toString must beEqualTo(Seq(Allele(11),Allele(15)).toString)
    }
    "should single allele convert" in {
      convertSingleAlele(OutOfLadderAllele(11,"<"),AleleRange(12,15)).toString must beEqualTo(OutOfLadderAllele(12,"<").toString)
      convertSingleAlele(OutOfLadderAllele(16,">"),AleleRange(12,15)).toString must beEqualTo(OutOfLadderAllele(15,">").toString)
      convertSingleAlele(MicroVariant(11),AleleRange(12,15)).toString must beEqualTo(OutOfLadderAllele(12,"<").toString)
      convertSingleAlele(MicroVariant(12),AleleRange(12,15)).toString must beEqualTo(MicroVariant(12).toString)
      convertSingleAlele(MicroVariant(16),AleleRange(12,15)).toString must beEqualTo(OutOfLadderAllele(15,">").toString)

    }
  }
  "matchLevel" should {
    "match high" in {
      val level = matchLevel(Allele(1), Allele(1))
      level must beEqualTo(Stringency.HighStringency)

    }
    "match with new alelles values" in {
      matchLevel(OutOfLadderAllele(12,"<"), Allele(11)) must beEqualTo(Stringency.HighStringency)
      matchLevel(OutOfLadderAllele(12,"<"), Allele(12)) must beEqualTo(Stringency.NoMatch)
      matchLevel(OutOfLadderAllele(12,"<"), OutOfLadderAllele(11,"<")) must beEqualTo(Stringency.HighStringency)
      matchLevel(OutOfLadderAllele(12,"<"), Allele(11.9)) must beEqualTo(Stringency.HighStringency)
      matchLevel(OutOfLadderAllele(12,"<"), OutOfLadderAllele(10,">")) must beEqualTo(Stringency.NoMatch)
      matchLevel(OutOfLadderAllele(12,"<"), MicroVariant(11)) must beEqualTo(Stringency.HighStringency)

      matchLevel(MicroVariant(12), Allele(11)) must beEqualTo(Stringency.NoMatch)
      matchLevel(MicroVariant(12),OutOfLadderAllele(12,">")) must beEqualTo(Stringency.HighStringency)
      matchLevel(MicroVariant(12), Allele(12.1)) must beEqualTo(Stringency.HighStringency)
      matchLevel(MicroVariant(12), Allele(13)) must beEqualTo(Stringency.NoMatch)


      matchLevel(OutOfLadderAllele(12,">"), OutOfLadderAllele(11,">")) must beEqualTo(Stringency.HighStringency)
      matchLevel(MicroVariant(11), MicroVariant(11)) must beEqualTo(Stringency.HighStringency)
      matchLevel(MicroVariant(11), MicroVariant(12)) must beEqualTo(Stringency.NoMatch)
      matchLevel(MicroVariant(12), MicroVariant(11)) must beEqualTo(Stringency.NoMatch)
      matchLevel(OutOfLadderAllele(12,"<"), OutOfLadderAllele(11,">")) must beEqualTo(Stringency.NoMatch)
      matchLevel(OutOfLadderAllele(12,">"), OutOfLadderAllele(11,"<")) must beEqualTo(Stringency.NoMatch)
      matchLevel(OutOfLadderAllele(12,">"), MicroVariant(12)) must beEqualTo(Stringency.HighStringency)
      matchLevel(OutOfLadderAllele(12,">"), Allele(12.5)) must beEqualTo(Stringency.HighStringency)
      matchLevel(OutOfLadderAllele(12,">"), Allele(12)) must beEqualTo(Stringency.NoMatch)


    }
  }

  "matchLevel" should {
    "reject non-matches" in {
      val level = matchLevel(Allele(1), Allele(2))
      level must beEqualTo(Stringency.NoMatch)
    }
  }

  "worstMatchLevel" should {
    "match high" in {
      val level = worstMatchLevel(List(Allele(1), Allele(2)), List(Allele(1), Allele(2)))
      level must beEqualTo(Stringency.HighStringency)
    }
  }

  "worstMatchLevel" should {
    "reject a non match" in {
      val level = worstMatchLevel(List(Allele(1), Allele(2)), List(Allele(3), Allele(4)))
      level must beEqualTo(Stringency.NoMatch)
    }
  }
  "genotypeMatch" should {
    "accept a permutation" in {
      val level = genotypeMatch(List(Allele(1), Allele(2)), List(Allele(2), Allele(1)))
      level must beEqualTo(Stringency.HighStringency)
    }
  }

  "genotypeMatch" should {
    "match moderate if different lenght" in {
      val level = genotypeMatch(List(Allele(1), Allele(2)), List(Allele(2)))
      level must beEqualTo(Stringency.ModerateStringency)

    }
    "Microvariantes Low UBA Test1" in {
      val level = genotypeMatch(List(MicroVariant(80), MicroVariant(81)), List(MicroVariant(80), MicroVariant(80)))
      level must beEqualTo(Stringency.LowStringency)
    }
    "Microvariantes Low UBA Test2" in {
      val level = genotypeMatch(List(MicroVariant(80), MicroVariant(80)), List(Allele(80.2),OutOfLadderAllele(BigDecimal("80.5"),">")))
      level must beEqualTo(Stringency.LowStringency)
    }
    "Varios test iba 1" in {
      //      10.0 12.0 [X 11,Q 11.3] [Q 11.9,Q 11.6,Q 11.5,Q 11.1] Baja
      val level1 = genotypeMatch(List(MicroVariant(11), Allele(11.3)),
                                 List(Allele(11.9),Allele(11.6),Allele(11.5),Allele(11.1)))
      level1 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 2" in {
      //      10.0 12.0 [X 10,Q 10.7,Q 10.2] [X 10,Q 10.9,Q 10.8,Q 10.5] Baja
      val level2 = genotypeMatch(List(MicroVariant(10), Allele(10.7), Allele(10.2)),
        List(MicroVariant(10),Allele(10.9),Allele(10.8),Allele(10.5)))
      level2 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 3" in {
      //      10.0 12.0 [Q 10.5,Q 10.6,Q 10.3,Q 10.2,X 10] [X 10,Q 10.4,Q 10.8] Baja
      val level3 = genotypeMatch(List(Allele(10.5), Allele(10.6), Allele(10.3), Allele(10.2),MicroVariant(10)),
        List(MicroVariant(10),Allele(10.4),Allele(10.8)))
      level3 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 4" in {
      //      10.0 12.0 [Q 10.1,X 10,Q 10.9] [Q 10.4,Q 10.8] Baja
      val level4 = genotypeMatch(List(Allele(10.1), MicroVariant(10), Allele(10.9)),
        List(Allele(10.4),Allele(10.8)))
      level4 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 5" in {
      //      10.0 12.0 [Q 11.8,X 11,Q 11.6,Q 11.5,Q 11.6] [Q 11.2,Q 11.7,X 11] Baja
      val level5 = genotypeMatch(List(Allele(11.8), MicroVariant(11), Allele(11.6), Allele(11.5), Allele(11.6)),
        List(Allele(11.2),Allele(11.7),MicroVariant(11)))
      level5 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 6" in {
      //      10.0 12.0 [X 10,Q 10.6] [Q 10.2,Q 10.5] Baja
      val level6 = genotypeMatch(List(MicroVariant(10),Allele(10.6)),
        List(Allele(10.2),Allele(10.5)))
      level6 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 7" in {
      //      10.0 12.0 [Q 11.3,X 11,Q 11.9] [Q 11.6,Q 11.7,X 11] Baja
      val level7 = genotypeMatch(List(Allele(11.3),MicroVariant(11),Allele(11.9)),
        List(Allele(11.6),Allele(11.7),MicroVariant(11)))
      level7 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 8" in {
      //      10.0 12.0 [Q 10.8,X 10] [Q 10.3,Q 10.7,Q 10.1] Baja
      val level8 = genotypeMatch(List(Allele(10.8),MicroVariant(10)),
        List(Allele(10.3),Allele(10.7),Allele(10.1)))
      level8 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 9" in {
      //      10.0 12.0 [X 11,Q 11.6] [Q 11.4,Q 11.8] Baja
      val level9 = genotypeMatch(List(MicroVariant(11),Allele(11.6)),
        List(Allele(11.4),Allele(11.8)))
      level9 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 10" in {
      //      10.0 12.0 [Q 11.4,X 11,Q 11.9,Q 11.7] [X 11,Q 11.6,Q 11.5] Baja
      val level10 = genotypeMatch(List(Allele(10.0),MicroVariant(11),Allele(11.9),Allele(11.7)),
        List(MicroVariant(11),Allele(11.6),Allele(11.5)))
      level10 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 11" in {
      //      10.0 12.0 [Q 11.6,Q 11.7,X 11] [Q 11.1,Q 11.3,Q 11.4,Q 11.8] Baja
      val level11 = genotypeMatch(List(Allele(10.0),Allele(11.6),Allele(11.7),MicroVariant(11)),
        List(Allele(11.1),Allele(11.3),Allele(11.4),Allele(11.8)))
      level11 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 12" in {
      //      10.0 12.0 [Q 11.5,X 11,Q 11.2] [Q 11.3,Q 11.7] Baja
      val level12 = genotypeMatch(List(Allele(11.5),MicroVariant(11),Allele(11.2)),
        List(Allele(11.3),Allele(11.7)))
      level12 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 13" in {
      //      10.0 12.0 [Q 10.6,Q 10.1,X 10,Q 10.5,Q 10.2] [Q 10.8,Q 10.7] Baja
      val level13 = genotypeMatch(List(Allele(10.6),Allele(10.1),MicroVariant(10),Allele(10.5),Allele(10.2)),
        List(Allele(10.8),Allele(10.7)))
      level13 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 14" in {
      //      10.0 12.0 [X 11,Q 11.7,Q 11.6,Q 11.1] [Q 11.8,Q 11.3] Baja
      val level14 = genotypeMatch(List(MicroVariant(11),Allele(11.7),Allele(11.6),Allele(11.1)),
        List(Allele(11.8),Allele(11.3)))
      level14 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 15" in {
      //      10.0 12.0 [Q 10.4,Q 10.8,X 10] [Q 10.6,Q 10.3,Q 10.3] Baja
      val level15 = genotypeMatch(List(Allele(10.4),Allele(10.8),MicroVariant(10)),
        List(Allele(10.6),Allele(10.3),Allele(10.3)))
      level15 must beEqualTo(Stringency.LowStringency)
    }
    "Varios test UBA 16" in {
      //      10.0 12.0 [X 10,Q 10.2,Q 10.9] [X 10,Q 10.7,Q 10.6] Baja
      val level16 = genotypeMatch(List(MicroVariant(10),Allele(10.2),Allele(10.9)),
        List(MicroVariant(10),Allele(10.7),Allele(10.6)))
      level16 must beEqualTo(Stringency.LowStringency)
    }
  }

  "genotypeMatch" should {
    "reject non matches" in {
      val level = genotypeMatch(List(Allele(1)), List(Allele(2)))
      level must beEqualTo(Stringency.NoMatch)
    }
  }

  "genotypeMatch" should {
    "match moderate if one of alleles list has an homocygote - case 1" in {
      val level = genotypeMatch(List(Allele(1), Allele(2), Allele(3)), List(Allele(1), Allele(2), Allele(2)))
      level must beEqualTo(Stringency.ModerateStringency)
    }

    "match moderate if one of alleles list has an homocygote - case 2" in {
      val level = genotypeMatch(List(Allele(3), Allele(2), Allele(1)), List(Allele(1), Allele(2), Allele(2)))
      level must beEqualTo(Stringency.ModerateStringency)
    }

    "match moderate if one of alleles list has an homocygote - case 3" in {
      val level = genotypeMatch(List(Allele(1), Allele(2), Allele(2)), List(Allele(1), Allele(2), Allele(3)))
      level must beEqualTo(Stringency.ModerateStringency)
    }

    "match moderate if one of alleles list has an homocygote - case 4" in {
      val level = genotypeMatch(List(Allele(1), Allele(2), Allele(2)), List(Allele(3), Allele(2), Allele(1)))
      level must beEqualTo(Stringency.ModerateStringency)
    }

    "match high if lists are 122 12" in {
      val level = genotypeMatch(List(Allele(1), Allele(2), Allele(2)), List(Allele(1), Allele(2)))
      level must beEqualTo(Stringency.HighStringency)
    }

    "match high if lists are 12 122" in {
      val level = genotypeMatch(List(Allele(1), Allele(2)), List(Allele(1), Allele(2), Allele(2)))
      level must beEqualTo(Stringency.HighStringency)
    }
  }

  "Profile Match" should {
    "filter profile required locus" in {
      val p1 = Stubs.mixtureProfile
      val markers = p1.genotypification(1).map(x => x._1.toString()).toSet

      markers.contains("FGA") must beEqualTo(true)
      markers.contains("D7S820") must beEqualTo(true)
      val markersSize = markers.size

      val p2 = convertProfileWithoutAcceptedLocus(p1,List("FGA","D8S1179"))
      val markers2 = p2.genotypification(1).map(x => x._1.toString()).toSet

      markers2.contains("D7S820") must beEqualTo(true)

      markers2.contains("FGA") must beEqualTo(false)

      markers2.size must beEqualTo(markersSize-2)
    }
    "convert profile with outofladder allelles" in {
      val p1 = Stubs.mixtureProfile
      val p2 = convertProfileWithConvertedOutOfLadderAlleles(p1,Map("TPOX" -> AleleRange(11,88)))

      p1.genotypification(1).get("TPOX").mkString must beEqualTo(Some(List(Allele(8), Allele(11), Allele(10))).mkString)
      p2.genotypification(1).get("TPOX").mkString must beEqualTo(Some(List(OutOfLadderAllele(11,"<"), Allele(11), OutOfLadderAllele(11,"<"))).mkString)
    }
    "match high" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(2), Allele(1)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.HighStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.HighStringency))
    }
  }

  "profileMatch" should {
    "match moderate" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(2)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.ModerateStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.ModerateStringency))
    }
  }

  "profileMatch" should {
    "not match unrelated" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(5), Allele(6)), "M2" -> List(Allele(7)))), analyses, None, None, None, None, None,false, true)

      val level = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      level must beNone
    }
  }

  "profileMatch" should {
    "NOT accept one mismatch" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(7)))), analyses, None, None, None, None, None,false, true)

      val level = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      level must beNone
    }
  }

  "profileMatch" should {
    "reject two mismatches" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)), "M3" -> List(Allele(5), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(7)), "M3" -> List(Allele(3), Allele(6)))), analyses, None, None, None, None, None,false, true)

      val level = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      level must beNone
    }
  }

  "profileMatch" should {
    "match in moderate for mixture and suspect" in {

      val pMixture = Stubs.mixtureProfile
      val p2 = Stubs.mixtureP2
      val mapOfMinimumStringencies = Stubs.listOfMinimumStringencies

      val res2 = profileMatch(pMixture, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)

      res2 must beSome
      res2.get.result.stringency must beEqualTo(Stringency.ModerateStringency)

    }
  }

  "profileMatch" should {
    "accept up to specified mismatches" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(7)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 1, true), None, 1)
      result must beSome

    }
  }

  "profileMatch" should {
    "NOT accept more mismatches than specified" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(7)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beNone

    }
  }

  "profileMatch" should {
    "count and report as mismatches the matches of lower stringency" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 1, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.HighStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.Mismatch))

    }
  }

  "profileMatch" should {
    "report High matches when High is requested" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(2), Allele(1)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.HighStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.HighStringency))
    }
  }

  "profileMatch" should {
    "NOT report Moderate matches when High is requested" in {

      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beNone
    }
  }

  "profileMatch" should {
    "NOT report Low matches when High is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(5)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.HighStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beNone
    }
  }

  "profileMatch" should {
    "report High matches when Moderate is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(2), Allele(1)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.HighStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.HighStringency))
    }
  }

  "profileMatch" should {
    "report Moderate matches when Moderate is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.ModerateStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.ModerateStringency))
    }
  }

  "profileMatch" should {
    "NOT report Low matches when Moderate is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(4)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(3), Allele(5)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beNone
    }
  }

  "profileMatch" should {
    "report High matches when Low is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.HighStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.HighStringency))
    }
  }

  "profileMatch" should {
    "report Moderate matches when Low is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(5), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(5)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.ModerateStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.HighStringency, "M2" -> Stringency.ModerateStringency))
    }
  }

  "profileMatch" should {
    "report Low matches when Low is requested" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, None, None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(5)), "M2" -> List(Allele(1), Allele(3)))), analyses, None, None, None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.stringency must beEqualTo(Stringency.LowStringency)
      result.get.result.matchingAlleles must beEqualTo(Map("M1" -> Stringency.LowStringency, "M2" -> Stringency.LowStringency))
    }
  }

  "profileMatch" should {
    "match Mix Mix" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, Some(2), None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(5)), "M2" -> List(Allele(1), Allele(3)))), analyses, None, Some(2), None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.ModerateStringency, true, true, Algorithm.GENIS_MM, 0, 0, true), None, 1)
      result must beSome
      result.get.result.algorithm must beEqualTo(Algorithm.GENIS_MM)
    }
  }

  "profileMatch" should {
    "match ENFSI" in {
      val p1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))), analyses, None, Some(1), None, None, None,false, true)
      val p2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("CAT_1"),
        Map(1 -> Map("M1" -> List(Allele(1), Allele(5)), "M2" -> List(Allele(1), Allele(3)))), analyses, None, Some(2), None, None, None,false, true)

      val result = profileMatch(p1, p2, MatchingRule(1, AlphanumericId("CAT_1"), Stringency.LowStringency, true, true, Algorithm.ENFSI, 0, 0, true), None, 1)
      result must beSome
      result.get.result.algorithm must beEqualTo(Algorithm.ENFSI)
    }
  }

  "sharedAllelePonderation" should {
    "be 100% for the same profile" in {
      val leftGenotype = Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))
      val rightGenotype = Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))

      val (leftPonderation, rightPonderation) = sharedAllelePonderation(leftGenotype, rightGenotype)

      leftPonderation must beEqualTo(1)
      rightPonderation must beEqualTo(1)
    }
  }

  "sharedAllelePonderation" should {
    "be different for each profile" in {
      val leftGenotype = Map("M1" -> List(Allele(1), Allele(2)), "M2" -> List(Allele(1), Allele(2)))
      val rightGenotype = Map("M1" -> List(Allele(1)), "M2" -> List(Allele(2)))

      val (leftPonderation, rightPonderation) = sharedAllelePonderation(leftGenotype, rightGenotype)

      leftPonderation must beEqualTo(0.5)
      rightPonderation must beEqualTo(1)
    }
  }

  "sharedAllelePonderation" should {
    "be 0% for empty profiles" in {
      val leftGenotype: Genotypification = Map()
      val rightGenotype: Genotypification = Map()

      val (leftPonderation, rightPonderation) = sharedAllelePonderation(leftGenotype, rightGenotype)

      leftPonderation must beEqualTo(0)
      rightPonderation must beEqualTo(0)
    }

  }

  "sharedAllelePonderation" should {
    "be 100% for same homocygote values" in {
      val leftGenotype = Map("M1" -> List(Allele(1), Allele(1)))
      val rightGenotype = Map("M1" -> List(Allele(1)))

      val (leftPonderation, rightPonderation) = sharedAllelePonderation(leftGenotype, rightGenotype)

      leftPonderation must beEqualTo(1)
      rightPonderation must beEqualTo(1)
    }

  }

  "uniquePonderation" should {
    "be right when firing=evidence and matching=reference" in {
      val leftProfile = Profile(null, SampleCode("AR-C-HIBA-500"), null, null, null, Map(), None, None, None, None, None, None, false, false, false)
      val rightProfile = Profile(null, SampleCode("AR-B-IMBICE-5000"), null, null, null, Map(), None, None, None, None, None, None, false, false, true)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.5,1.0,Algorithm.ENFSI), 1)

      val result = uniquePonderation(matchResult, leftProfile, rightProfile)

      result must beEqualTo(1.0)
    }
    "be left when firing=reference and matching=reference" in {
      val leftProfile = Profile(null, SampleCode("AR-C-HIBA-500"), null, null, null, Map(), None, None, None, None, None, None, false, false, true)
      val rightProfile = Profile(null, SampleCode("AR-B-IMBICE-5000"), null, null, null, Map(), None, None, None, None, None, None, false, false, true)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.5,1.0,Algorithm.ENFSI), 1)

      val result = uniquePonderation(matchResult, leftProfile, rightProfile)

      result must beEqualTo(0.5)
    }
    "be left when firing=evidence and matching=evidence" in {
      val leftProfile = Profile(null, SampleCode("AR-C-HIBA-500"), null, null, null, Map(), None, None, None, None, None, None, false, false, true)
      val rightProfile = Profile(null, SampleCode("AR-B-IMBICE-5000"), null, null, null, Map(), None, None, None, None, None, None, false, false, true)

      val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
        MongoDate(new Date()), 1,
        MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None),
        MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None),
        NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.5,1.0,Algorithm.ENFSI), 1)

      val result = uniquePonderation(matchResult, leftProfile, rightProfile)

      result must beEqualTo(0.5)
    }
  }


  "getLR" should {
    "use default stats if parameters are not specified" in {

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val mockCalculatorService = mock[MatchingCalculatorService]
      when(mockCalculatorService.getLRByAlgorithm(any[Profile], any[Profile], any[StatOption],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(None))

      val mockProbabilityService = mock[ProbabilityService]
      when(mockProbabilityService.getStats(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val mockMatchingService= mock[MatchingService]
      when(mockMatchingService.getMatchResultById(any[Option[String]])).thenReturn(Future.successful(None))

      val matching = new Matching(mockMatchingService, mockProfileService, mockCalculatorService, mockProbabilityService, null, null)

      val lrRequest = LRRequest(SampleCode("AR-C-SHDG-2"), SampleCode("AR-B-IMBICE-1002"), None)
      val request = FakeRequest().withBody(Json.toJson(lrRequest))
      val result: Future[Result] = matching.getLR().apply(request)

      verify(mockProbabilityService).getStats(any[SampleCode])
      status(result) must beEqualTo(OK)
    }

    "use method parameters if stats are specified" in {

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val mockCalculatorService = mock[MatchingCalculatorService]
      when(mockCalculatorService.getLRByAlgorithm(any[Profile], any[Profile], any[StatOption],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(None))

      val mockProbabilityService = mock[ProbabilityService]
      when(mockProbabilityService.getStats(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.statOption)))

      val mockMatchingService= mock[MatchingService]
      when(mockMatchingService.getMatchResultById(any[Option[String]])).thenReturn(Future.successful(None))

      val matching = new Matching(mockMatchingService, mockProfileService, mockCalculatorService, mockProbabilityService, null, null)

      val lrRequest = LRRequest(SampleCode("AR-C-SHDG-2"), SampleCode("AR-B-IMBICE-1002"), Some(Stubs.statOption))
      val request = FakeRequest().withBody(Json.toJson(lrRequest))
      val result: Future[Result] = matching.getLR().apply(request)

      verify(mockProbabilityService, times(0)).getStats(any[SampleCode])
      status(result) must beEqualTo(OK)
    }

    "return bad request if request body is invalid" in {
      val mockMatchingService= mock[MatchingService]
      when(mockMatchingService.getMatchResultById(any[Option[String]])).thenReturn(Future.successful(None))

      val matching = new Matching(mockMatchingService, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("sample" -> "AR- C-SHDG-2"))
      val result: Future[Result] = matching.getLR().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "return bad request if there are no default stats configured" in {

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.findByCode(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.newProfile)))

      val mockCalculatorService = mock[MatchingCalculatorService]
      when(mockCalculatorService.getLRByAlgorithm(any[Profile], any[Profile], any[StatOption],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(None))

      val mockProbabilityService = mock[ProbabilityService]
      when(mockProbabilityService.getStats(any[SampleCode])).thenReturn(Future.successful(None))

      val mockMatchingService= mock[MatchingService]
      when(mockMatchingService.getMatchResultById(any[Option[String]])).thenReturn(Future.successful(None))

      val matching = new Matching(mockMatchingService, mockProfileService, mockCalculatorService, mockProbabilityService, null, null)

      val lrRequest = LRRequest(SampleCode("AR-C-SHDG-2"), SampleCode("AR-B-IMBICE-1002"), None)
      val request = FakeRequest().withBody(Json.toJson(lrRequest))
      val result: Future[Result] = matching.getLR().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }
  }

  "convert discard" should {
    "return bad request if service returns left" in {
      val matchingService = mock[MatchingService]
      when(matchingService.convertDiscard(any[String], any[SampleCode], any[Boolean], any[Boolean])).thenReturn(Future.successful(Left("error message")))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val matching = new Matching(matchingService, null, null, null, null, userService )

      val request = FakeRequest().withHeaders("X-USER" -> "pdg")
      val result: Future[Result] = matching.convertDiscard("5343243u23d1kl", SampleCode("AR-B-IMBICE-501")).apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "be ok if service returns right" in {
      val matchingService = mock[MatchingService]
      when(matchingService.convertDiscard(any[String], any[SampleCode], any[Boolean], any[Boolean])).thenReturn(Future.successful(Right(Seq(Stubs.sampleCode))))
      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val matching = new Matching(matchingService, null, null, null, null, userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg")
      val result: Future[Result] = matching.convertDiscard("5343243u23d1kl", SampleCode("AR-B-IMBICE-501")).apply(request)

      status(result) must beEqualTo(OK)
    }
  }


  "A MatchingController" should {

    "return a bad request getting matches" in {
      val controller = new Matching(null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = controller.getMatches().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "return ok getting matches" in {
      val mejorLR =MatchCardMejorLr( "InternalSampleCode",
        AlphanumericId("IR"),
        1,
        MatchStatus.hit,
        MatchStatus.hit,
        0.0,
        1,
        0.0,
        SampleCode("AR-C-SHDG-1"),
        1)
      val matchesCards = Seq(
        MatchCardForense(MatchCard(SampleCode("AR-C-SHDG-1"), 1, 0, 0, 0, 2, "InternalSampleCode", AlphanumericId("IR"), new Date(), "SHDG", "assignee"),
          mejorLR ),
        MatchCardForense(MatchCard(SampleCode("AR-C-SHDG-2"), 0, 1, 1, 1, 2, "InternalSampleCode2", AlphanumericId("IR"), new Date(), "SHDG", "assignee2"),
          mejorLR)
      )

      val matchingService = mock[MatchingService]
      when(matchingService.getMatches(any[MatchCardSearch])).thenReturn(Future.successful(matchesCards))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val search = MatchCardSearch("pdg", false)

      val controller = new Matching(matchingService, null, null, null, null, userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.toJson(search))
      val result: Future[Result] = controller.getMatches().apply(request)

      status(result) must beEqualTo(OK)
    }


    "return a bad request getting total matches" in {
      val controller = new Matching(null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = controller.getTotalMatches().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "return ok getting total matches" in {
      val matchesCards = Seq(
        MatchCard(SampleCode("AR-C-SHDG-1"), 1, 0, 0, 0, 2, "InternalSampleCode", AlphanumericId("IR"), new Date(), "SHDG", "assignee"),
        MatchCard(SampleCode("AR-C-SHDG-2"), 0, 1, 1, 1, 2, "InternalSampleCode2", AlphanumericId("IR"), new Date(), "SHDG", "assignee2")
      )

      val matchingService = mock[MatchingService]
      when(matchingService.getTotalMatches(any[MatchCardSearch])).thenReturn(Future.successful(2))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((false)))

      val search = MatchCardSearch("pdg", false)

      val controller = new Matching(matchingService, null, null, null, null, userService)

      val request = FakeRequest().withHeaders("X-USER" -> "pdg").withHeaders("X-SUPERUSER" -> "false").withBody(Json.toJson(search))
      val result: Future[Result] = controller.getTotalMatches().apply(request)

      status(result) must beEqualTo(OK)
    }

    "return a bad request getting matches by group" in {
      val controller = new Matching(null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = controller.getMatchesByGroup().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "return ok getting matches by group" in {
      val matchesResults = Seq(
        MatchingResult(
          "59108248e351ef09a333a3c4",
          SampleCode("AR-C-SHDG-1384"),
          "internal",
          Stringency.ModerateStringency,
          Map("D3S1358" -> Stringency.ModerateStringency, "FGA" -> Stringency.ModerateStringency, "vWA" -> Stringency.ModerateStringency),
          3,
          AlphanumericId("CA"),
          MatchStatus.pending,
          MatchStatus.pending,
          MatchGlobalStatus.pending,
          1.0,
          2,
          false,
          Algorithm.ENFSI,
          1
        ),
        MatchingResult(
          "59108248e351ef09a333a3c57",
          SampleCode("AR-C-SHDG-1385"),
          "internal",
          Stringency.ModerateStringency,
          Map("D3S1358" -> Stringency.ModerateStringency, "FGA" -> Stringency.ModerateStringency, "vWA" -> Stringency.ModerateStringency),
          3,
          AlphanumericId("CA"),
          MatchStatus.pending,
          MatchStatus.pending,
          MatchGlobalStatus.pending,
          1.0,
          2,
          false,
          Algorithm.ENFSI,
          1
        )
      )

      val matchingService = mock[MatchingService]
      when(matchingService.getMatchesByGroup(any[MatchGroupSearch])).thenReturn(Future.successful(matchesResults))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val search = MatchGroupSearch("pdg", true, SampleCode("AR-C-SHDG-1"), MatchKind.Normal, 1, 30, "id", true)

      val controller = new Matching(matchingService, null, null, null, null, userService)

      val request = FakeRequest().withBody(Json.toJson(search))
      val result: Future[Result] = controller.getMatchesByGroup().apply(request)

      status(result) must beEqualTo(OK)
    }


    "return a bad request getting total matches by group" in {
      val controller = new Matching(null, null, null, null, null, null)

      val request = FakeRequest().withBody(Json.obj("user" -> "pdg"))
      val result: Future[Result] = controller.getTotalMatchesByGroup().apply(request)

      status(result) must beEqualTo(BAD_REQUEST)
    }

    "return ok getting total matches by group" in {
      val matchesResults = Seq(
        MatchingResult(
          "59108248e351ef09a333a3c4",
          SampleCode("AR-C-SHDG-1384"),
          "internal",
          Stringency.ModerateStringency,
          Map("D3S1358" -> Stringency.ModerateStringency, "FGA" -> Stringency.ModerateStringency, "vWA" -> Stringency.ModerateStringency),
          3,
          AlphanumericId("CA"),
          MatchStatus.pending,
          MatchStatus.pending,
          MatchGlobalStatus.pending,
          1.0,
          2,
          false,
          Algorithm.ENFSI,
          1
        ),
        MatchingResult(
          "59108248e351ef09a333a3c57",
          SampleCode("AR-C-SHDG-1385"),
          "internal",
          Stringency.ModerateStringency,
          Map("D3S1358" -> Stringency.ModerateStringency, "FGA" -> Stringency.ModerateStringency, "vWA" -> Stringency.ModerateStringency),
          3,
          AlphanumericId("CA"),
          MatchStatus.pending,
          MatchStatus.pending,
          MatchGlobalStatus.pending,
          1.0,
          2,
          false,
          Algorithm.ENFSI,
          1
        )
      )

      val matchingService = mock[MatchingService]
      when(matchingService.getTotalMatchesByGroup(any[MatchGroupSearch])).thenReturn(Future.successful(2))

      val userService = mock[UserService]
      when(userService.isSuperUser("pdg")).thenReturn(Future.successful((true)))

      val search = MatchGroupSearch("pdg", true, SampleCode("AR-C-SHDG-1"), MatchKind.Normal, 1, 30, "id", true)

      val controller = new Matching(matchingService, null, null, null, null, userService)

      val request = FakeRequest().withBody(Json.toJson(search))
      val result: Future[Result] = controller.getTotalMatchesByGroup().apply(request)

      status(result) must beEqualTo(OK)
    }
  }
}