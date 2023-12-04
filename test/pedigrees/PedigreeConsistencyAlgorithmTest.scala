package pedigrees

import org.scalatestplus.play.PlaySpec
import pedigree.{Individual, NodeAlias, PedigreeConsistencyAlgorithm, PedigreeConsistencyCheck}
import profile.GenotypificationByType.GenotypificationByType
import profile.{Allele, Mitocondrial, Profile, XY}
import stubs.Stubs.{catA1, genotypification}
import types.{SampleCode, Sex}
import matching.{CompareMixtureGenotypification, MatchingServiceSparkImpl}
import stubs.Stubs

class PedigreeConsistencyAlgorithmTest extends PlaySpec{

  def checkConsistency(
    profiles: Array[Profile],
    genogram: Array[Individual]
  ):Seq[PedigreeConsistencyCheck]={
//    val matchingService = new matching.MatchingServiceSparkImpl(
//      profileRepo = null,
//      matchingRepo = null,
//      notificationService = null,
//      categoryService = null,
//      profileDataRepo = null,
//      scenarioRepository = null,
//      spak2Matcher = null,
//      traceService = null,
//      pedigreeSparkMatcher = null,
//      pedigreeRepo = null,
//      pedigreeGenotypificationService = null,
//      interconnectionService = null,
//      spark2MatcherCollapsing = null,
//      spark2MatcherScreening = null
//    )
    PedigreeConsistencyAlgorithm.isConsistent(profiles, genogram)
  }
  def profileBuilder(globalCode:String,alleles:List[Allele],empty:Boolean =false): Profile = {
    if(empty){
      Profile(SampleCode(globalCode), SampleCode(globalCode), globalCode, "",catA1.id,
        Map.empty, None, None, None, None, None, None, false, true, false)
    }else{
      Profile(SampleCode(globalCode), SampleCode(globalCode), globalCode, "",catA1.id,
        Map( 1 -> Map("LOCUS1" -> alleles)), None, None, None, None, None, None, false, true, false)
    }
  }
  def individualBuilder(
    alias:String,
    sex: Sex.Value,
    globalCode: Option[String] = None,
    idFather: Option[String] = None,
    idMother: Option[String] = None,
    isUnknown: Boolean = false
  ): Individual = {
    Individual(
      NodeAlias(alias),
      idFather.map(NodeAlias(_)),
      idMother.map(NodeAlias(_)),
      sex,
      globalCode.map(SampleCode(_)),
      unknown = isUnknown,
      None
    )
  }
  "A Pedigree Consistency Algorithm" must {

    "pedigree1 consistent" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(12)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }

    "pedigree1 un alelo compartido entre los tres, el resto distinto" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(12))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.size mustBe profiles.length
    }

    "pedigree1 consistent. Un padre es unknown." in {

      val profiles = Array(
        profileBuilder("AR-C-SHDG-1", List(Allele(12), Allele(12))),
        profileBuilder("AR-C-SHDG-2", List(Allele(13), Allele(12))),
        profileBuilder("AR-C-SHDG-3", List(Allele(15), Allele(15))),
        profileBuilder("AR-C-SHDG-4", List(Allele(12), Allele(15))),
        profileBuilder("AR-C-SHDG-5", List(Allele(14), Allele(15)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP", Sex.Male, Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP", Sex.Female, Some("AR-C-SHDG-2")),
        individualBuilder(
          "PadrePI",
          Sex.Male,
          None,
          Some("AbueloP"),
          Some("AbuelaP"),
          isUnknown = false
        ),
        individualBuilder("Madre", Sex.Female, Some("AR-C-SHDG-3")),
        individualBuilder("Hijo1", Sex.Male, Some("AR-C-SHDG-4"), Some("PadrePI"), Some("Madre")),
        individualBuilder("Hijo2", Sex.Male, Some("AR-C-SHDG-5"), Some("PadrePI"), Some("Madre"))
      )
      val result = checkConsistency(profiles, genogram1)
      println(result)
      result mustBe Seq.empty
    }

    "pedigree1 es  inconsistent. Un padre es unknown." in {

      val profiles = Array(
        profileBuilder("AR-C-SHDG-1", List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2", List(Allele(12), Allele(11))),
        profileBuilder("AR-C-SHDG-3", List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-4", List(Allele(15), Allele(15)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP-PI", Sex.Male, Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP", Sex.Female, Some("AR-C-SHDG-2")),
        individualBuilder(
          "Padre",
          Sex.Male,
          None,
          Some("AbueloP-PI"),
          Some("AbuelaP"),
          isUnknown = true
        ),
        individualBuilder("Madre", Sex.Female, Some("AR-C-SHDG-4")),
        individualBuilder("Hijo", Sex.Male, Some("AR-C-SHDG-3"), Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles, genogram1)

      result mustBe Seq.empty
    }

    "pedigree1 consistent los padres comparten primer alelo" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(12), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(12), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }

    "pedigree1 consistent los padres comparten segundo alelo" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(13))),
        profileBuilder("AR-C-SHDG-3",List(Allele(12), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }

    "pedigree1 consistent todos alelos iguales" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-3",List(Allele(12), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }
    "pedigree1 consistent primer alelo de la madre segundo del padre" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(11), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }
    "pedigree1 consistent primer alelo del padre segundo de la madre" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(12), Allele(10)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }
    "pedigree1 inconsistent homocigota" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(11)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.sortBy(_.globalCode) mustBe Seq(PedigreeConsistencyCheck("AR-C-SHDG-1",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")))
    }
    "pedigree1 inconsistent dos alelos distintos" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
                            profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
                            profileBuilder("AR-C-SHDG-3",List(Allele(14), Allele(15))))

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.sortBy(_.globalCode) mustBe Seq(
                        PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")))
    }
    "pedigree1 inconsistent comparte solo con madre" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(15))))

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.sortBy(_.globalCode) mustBe Seq(
        PedigreeConsistencyCheck("AR-C-SHDG-1",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")))
    }
    "pedigree1 inconsistent comparte solo con padre" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(12), Allele(15))))

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.sortBy(_.globalCode) mustBe Seq(
        PedigreeConsistencyCheck("AR-C-SHDG-2",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")))
    }

    "pedigree2 inconsistent more than 4 alleles in a sibship" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-3",List(Allele(14), Allele(15))),
        profileBuilder("AR-C-SHDG-4",List(Allele(16), Allele(17))),
        profileBuilder("AR-C-SHDG-5",List(Allele(14), Allele(18))))

      val genogram2 = Array(
        individualBuilder("AbueloP",Sex.Male),
        individualBuilder("AbuelaP",Sex.Female),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tio1",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tio2",Sex.Male,Some("AR-C-SHDG-5"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram2)

      result.sortBy(_.globalCode) mustBe Seq(PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-4",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-5",List("LOCUS1")))
    }
    "pedigree2 inconsistent more than 3 alleles in a sibship with one homocygous" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-3",List(Allele(14), Allele(15))),
        profileBuilder("AR-C-SHDG-4",List(Allele(16))),
        profileBuilder("AR-C-SHDG-5",List(Allele(13), Allele(16))))

      val genogram2 = Array(
        individualBuilder("AbueloP",Sex.Male),
        individualBuilder("AbuelaP",Sex.Female),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tio1",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tio2",Sex.Male,Some("AR-C-SHDG-5"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram2)

      result.sortBy(_.globalCode) mustBe Seq(PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-4",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-5",List("LOCUS1")))
    }

    "pedigree3 consistent " in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11)))
      )

      val genogram1 = Array(
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-1")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-2"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("AbueloP",Sex.Male,None),
        individualBuilder("Padre",Sex.Male,None,Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }

    "pedigree3 inconsistent " in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-1")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-2"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("AbueloP",Sex.Male,None),
        individualBuilder("Padre",Sex.Male,None,Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.sortBy(_.globalCode) mustBe Seq(PedigreeConsistencyCheck("AR-C-SHDG-1",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-2",List("LOCUS1")))
    }

    "pedigree4 inconsistent (2 inconsistencias, rama paterna, rama materna) " in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(13))),
        profileBuilder("AR-C-SHDG-3",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-4",List(Allele(10), Allele(13)))
      )

      val genogram1 = Array(
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-1")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-2"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("AbuelaM",Sex.Female,Some("AR-C-SHDG-3")),
        individualBuilder("TioM",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloM"), Some("AbuelaM")),
        individualBuilder("AbueloP",Sex.Male,None),
        individualBuilder("AbueloM",Sex.Male,None),
        individualBuilder("Padre",Sex.Male,None,Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female,None,Some("AbueloM"), Some("AbuelaM")),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.size>1 mustBe true
      result.size mustBe profiles.size
    }
    "pedigree5 consistent contra abuelos 1" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(1), Allele(2))),
        profileBuilder("AR-C-SHDG-2",List(Allele(3), Allele(4))),
        profileBuilder("AR-C-SHDG-3",List(Allele(1), Allele(5))),
        profileBuilder("AR-C-SHDG-4",List(Allele(5), Allele(6))),
        profileBuilder("AR-C-SHDG-5",List(Allele(7), Allele(8))))

      val genogram1 = Array(
        individualBuilder("BisAbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("BisAbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("BisAbueloP1",Sex.Male,Some("AR-C-SHDG-4")),
        individualBuilder("BisAbuelaP2",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("AbueloP",Sex.Male,None,Some("BisAbueloP"), Some("BisAbuelaP")),
        individualBuilder("AbuelaP",Sex.Female,None,Some("BisAbueloP1"), Some("BisAbuelaP2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }

    "pedigree5 consistent contra abuelos 2" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(1), Allele(2))),
        profileBuilder("AR-C-SHDG-2",List(Allele(3), Allele(4))),
        profileBuilder("AR-C-SHDG-3",List(Allele(7), Allele(4))),
        profileBuilder("AR-C-SHDG-4",List(Allele(5), Allele(6))),
        profileBuilder("AR-C-SHDG-5",List(Allele(7), Allele(8))))

      val genogram1 = Array(
        individualBuilder("BisAbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("BisAbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("BisAbueloP1",Sex.Male,Some("AR-C-SHDG-4")),
        individualBuilder("BisAbuelaP2",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("AbueloP",Sex.Male,None,Some("BisAbueloP"), Some("BisAbuelaP")),
        individualBuilder("AbuelaP",Sex.Female,None,Some("BisAbueloP1"), Some("BisAbuelaP2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }
    "pedigree5 consistent contra abuelos 3" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(1), Allele(2))),
        profileBuilder("AR-C-SHDG-2",List(Allele(3), Allele(4))),
        profileBuilder("AR-C-SHDG-3",List(Allele(1), Allele(4))),
        profileBuilder("AR-C-SHDG-4",List(Allele(5), Allele(6))),
        profileBuilder("AR-C-SHDG-5",List(Allele(1), Allele(7))))

      val genogram1 = Array(
        individualBuilder("BisAbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("BisAbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("BisAbueloP1",Sex.Male,Some("AR-C-SHDG-4")),
        individualBuilder("BisAbuelaP2",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("AbueloP",Sex.Male,None,Some("BisAbueloP"), Some("BisAbuelaP")),
        individualBuilder("AbuelaP",Sex.Female,None,Some("BisAbueloP1"), Some("BisAbuelaP2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }

    "pedigree5 inconsistent contra abuelos 1" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(1), Allele(2))),
        profileBuilder("AR-C-SHDG-2",List(Allele(3), Allele(4))),
        profileBuilder("AR-C-SHDG-3",List(Allele(1), Allele(9))),
        profileBuilder("AR-C-SHDG-4",List(Allele(5), Allele(6))),
        profileBuilder("AR-C-SHDG-5",List(Allele(7), Allele(8))))

      val genogram1 = Array(
        individualBuilder("BisAbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("BisAbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("BisAbueloP1",Sex.Male,Some("AR-C-SHDG-4")),
        individualBuilder("BisAbuelaP2",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("AbueloP",Sex.Male,None,Some("BisAbueloP"), Some("BisAbuelaP")),
        individualBuilder("AbuelaP",Sex.Female,None,Some("BisAbueloP1"), Some("BisAbuelaP2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.size>0 mustBe true
      result.size mustBe 3
      result.sortBy(_.globalCode) mustBe Seq(PedigreeConsistencyCheck("AR-C-SHDG-3",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-4",List("LOCUS1")),
        PedigreeConsistencyCheck("AR-C-SHDG-5",List("LOCUS1")))

    }
    "pedigree5 inconsistent contra abuelos 2" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(1), Allele(2))),
        profileBuilder("AR-C-SHDG-2",List(Allele(3), Allele(4))),
        profileBuilder("AR-C-SHDG-3",List(Allele(5), Allele(9))),
        profileBuilder("AR-C-SHDG-4",List(Allele(5), Allele(6))),
        profileBuilder("AR-C-SHDG-5",List(Allele(7), Allele(8))))

      val genogram1 = Array(
        individualBuilder("BisAbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("BisAbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("BisAbueloP1",Sex.Male,Some("AR-C-SHDG-4")),
        individualBuilder("BisAbuelaP2",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("AbueloP",Sex.Male,None,Some("BisAbueloP"), Some("BisAbuelaP")),
        individualBuilder("AbuelaP",Sex.Female,None,Some("BisAbueloP1"), Some("BisAbuelaP2")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.size>0 mustBe true
      result.size mustBe 3
    }

    "pedigree6 consistent muchos tios consistente" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(12))),
        profileBuilder("AR-C-SHDG-4",List(Allele(11), Allele(13))),
        profileBuilder("AR-C-SHDG-6",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-7",List(Allele(13), Allele(10))),
        profileBuilder("AR-C-SHDG-5",List(Allele(12), Allele(10)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Tia2P",Sex.Female,Some("AR-C-SHDG-6"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tia3P",Sex.Female,Some("AR-C-SHDG-7"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TiaP",Sex.Female,Some("AR-C-SHDG-5"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty
    }
    "pedigree6 consistent muchos tios uno inconsistente" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(12))),
        profileBuilder("AR-C-SHDG-4",List(Allele(11), Allele(13))),
        profileBuilder("AR-C-SHDG-6",List(Allele(11), Allele(12))),
        profileBuilder("AR-C-SHDG-7",List(Allele(13), Allele(15))),
        profileBuilder("AR-C-SHDG-5",List(Allele(12), Allele(10)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Tia2P",Sex.Female,Some("AR-C-SHDG-6"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tia3P",Sex.Female,Some("AR-C-SHDG-7"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TiaP",Sex.Female,Some("AR-C-SHDG-5"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)
      result.size>0 mustBe true
      result.size mustBe 1
    }
    "pedigree6 consistent muchos tios dos inconsistente" in {

      val profiles = Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(13))),
        profileBuilder("AR-C-SHDG-2",List(Allele(10), Allele(11))),
        profileBuilder("AR-C-SHDG-3",List(Allele(10), Allele(12))),
        profileBuilder("AR-C-SHDG-4",List(Allele(11), Allele(13))),
        profileBuilder("AR-C-SHDG-6",List(Allele(11), Allele(15))),
        profileBuilder("AR-C-SHDG-7",List(Allele(13), Allele(15))),
        profileBuilder("AR-C-SHDG-5",List(Allele(12), Allele(10)))
      )

      val genogram1 = Array(
        individualBuilder("AbueloP",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("AbuelaP",Sex.Female,Some("AR-C-SHDG-2")),
        individualBuilder("Tia2P",Sex.Female,Some("AR-C-SHDG-6"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Tia3P",Sex.Female,Some("AR-C-SHDG-7"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TiaP",Sex.Female,Some("AR-C-SHDG-5"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("TioP",Sex.Male,Some("AR-C-SHDG-4"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Padre",Sex.Male,Some("AR-C-SHDG-3"),Some("AbueloP"), Some("AbuelaP")),
        individualBuilder("Madre",Sex.Female),
        individualBuilder("PI",Sex.Male,None,Some("Padre"), Some("Madre"))
      )

      val result = checkConsistency(profiles,genogram1)

      result.size>0 mustBe true
      result.size mustBe profiles.size
    }

    "pedigree7 consistent caso Pedigree5Mariana.png" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(12), Allele(12))),
        profileBuilder("AR-C-SHDG-4",List(Allele(12), Allele(11))),
        profileBuilder("AR-C-SHDG-5",List(Allele(12), Allele(12))),
        profileBuilder("AR-C-SHDG-7",List(Allele(11), Allele(12))))

      val genogram1 = Array(
        individualBuilder("1",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("2",Sex.Female),
        individualBuilder("3",Sex.Male,None,Some("1"), Some("2")),
        individualBuilder("4",Sex.Female,Some("AR-C-SHDG-4"),Some("1"), Some("2")),
        individualBuilder("5",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("7",Sex.Male,Some("AR-C-SHDG-7"),Some("3"), Some("5")),
        individualBuilder("6",Sex.Male,None,Some("3"), Some("5")),
        individualBuilder("8",Sex.Female),
        individualBuilder("9",Sex.Male,None,Some("6"), Some("8"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }
    "pedigree7 consistent caso Pedigree5Mariana.png 2" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(8), Allele(8))),
        profileBuilder("AR-C-SHDG-4",List(Allele(8), Allele(12))),
        profileBuilder("AR-C-SHDG-5",List(Allele(11), Allele(9))),
        profileBuilder("AR-C-SHDG-7",List(Allele(11), Allele(12))))

      val genogram1 = Array(
        individualBuilder("1",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("2",Sex.Female),
        individualBuilder("3",Sex.Male,None,Some("1"), Some("2")),
        individualBuilder("4",Sex.Female,Some("AR-C-SHDG-4"),Some("1"), Some("2")),
        individualBuilder("5",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("7",Sex.Male,Some("AR-C-SHDG-7"),Some("3"), Some("5")),
        individualBuilder("6",Sex.Male,None,Some("3"), Some("5")),
        individualBuilder("8",Sex.Female),
        individualBuilder("9",Sex.Male,None,Some("6"), Some("8"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }
    "pedigree7 consistent caso Pedigree5Mariana.png 2 - caso perfil sin marcador" in {

      val profiles =  Array(profileBuilder("AR-C-SHDG-1",List(Allele(8), Allele(8))),
        profileBuilder("AR-C-SHDG-3",Nil,true),
        profileBuilder("AR-C-SHDG-4",List(Allele(8), Allele(12))),
        profileBuilder("AR-C-SHDG-5",List(Allele(11), Allele(9))),
        profileBuilder("AR-C-SHDG-7",List(Allele(11), Allele(12))))

      val genogram1 = Array(
        individualBuilder("1",Sex.Male,Some("AR-C-SHDG-1")),
        individualBuilder("2",Sex.Female),
        individualBuilder("3",Sex.Male,Some("AR-C-SHDG-3"),Some("1"), Some("2")),
        individualBuilder("4",Sex.Female,Some("AR-C-SHDG-4"),Some("1"), Some("2")),
        individualBuilder("5",Sex.Female,Some("AR-C-SHDG-5")),
        individualBuilder("7",Sex.Male,Some("AR-C-SHDG-7"),Some("3"), Some("5")),
        individualBuilder("6",Sex.Male,None,Some("3"), Some("5")),
        individualBuilder("8",Sex.Female),
        individualBuilder("9",Sex.Male,None,Some("6"), Some("8"))
      )

      val result = checkConsistency(profiles,genogram1)

      result mustBe Seq.empty

    }
  }
}
