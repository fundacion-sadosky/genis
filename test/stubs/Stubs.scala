package stubs

import java.text.SimpleDateFormat
import java.util.Date

import audit.{Key, OperationLogEntry, SignedOperationLogEntry}
import configdata._
import controllers.UserPassword
import inbox._
import kits._
import laboratories.{Geneticist, Laboratory}
import matching.Stringency.{Stringency => _, _}
import matching._
import org.bson.types.ObjectId
import pedigree.{NodeAlias, PedigreeMatchingProfile, _}
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.JsValue
import probability.{LRResult, ProbabilityModel}
import profile.GenotypificationByType.GenotypificationByType
import profile.Profile.LabeledGenotypification
import profile._
import profiledata.{DataFiliation, DataFiliationAttempt, ProfileData, ProfileDataAttempt,ProfileDataFull}
import scenarios._
import security.{AuthenticatedPair, RequestToken}
import services.{CacheKey, CacheService}
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyNameView, PopulationSampleFrequency}
import trace._
import types.{MongoId, Permission, _}
import user._

import scala.concurrent.Future
import scala.math.BigDecimal.int2bigDecimal
import scala.util.Random
import trace.ProfileDataInfo

object Stubs {

  val catA1 = Category(AlphanumericId("SOSPECHOSO"), AlphanumericId("GRUPO_A"), "Subcategory A1", true, Option("description of subcategory A1"))
  val catA2 = Category(AlphanumericId("SUBCAT_A2"), AlphanumericId("GRUPO_A"), "Subcategory A2", false, Option("description of subcategory A2"))
  val catA3 = Category(AlphanumericId("SUBCAT_A3"), AlphanumericId("GRUPO_A"), "Subcategory A3", false, Option("description of subcategory A3"))
  val catMixture = Category(AlphanumericId("MULTIPLE"), AlphanumericId("FORENSE"), "Aportantes múltiples", false, None)
  val catMixtureVictim = Category(AlphanumericId("MULTIPLE_VICTIMA"), AlphanumericId("FORENSE"), "Aportantes múltiples con victima asociada", false, None)

  val fullCatA1 = FullCategory(catA1.id, catA1.name, catA1.description, catA1.group, true, true, true, false,manualLoading=true,
    Map(1 -> CategoryConfiguration("", "", "K", "2", 2)), Seq.empty, Seq.empty, Seq.empty,Some(1))
  val fullCatA2 = FullCategory(catA2.id, catA2.name, catA2.description, catA2.group, false, true, true, false,manualLoading=true,
    Map(1 -> CategoryConfiguration("", "", "K", "0", 4)), Seq.empty, Seq.empty, Seq.empty,Some(1))

  val fullCatMixture =
    FullCategory(catMixture.id, catMixture.name, catMixture.description, catMixture.group,
      false, false, true, false,manualLoading=true,
      Map(1 -> CategoryConfiguration("", "", "K", "3", 6)), Seq.empty, Seq("mezcla", "multiples"), Seq.empty,Some(1))

  val fullCatMixtureVictim =
    FullCategory(catMixtureVictim.id, catMixtureVictim.name, catMixtureVictim.description, catMixtureVictim.group,
      false, false, true, false,manualLoading=true,
      Map(1 -> CategoryConfiguration("", "", "K", "3", 6)), Seq(CategoryAssociation(1, AlphanumericId("VICTIMA"), 0)), Seq.empty, Seq.empty,Some(1))

  val groupA = Group(
    AlphanumericId("INDUBITADA"),
    "Group A",
    Option("Description catgeroria A"))

  val catB1 = Category(AlphanumericId("SUBCAT_B1"), AlphanumericId("GRUPO_B"), "Category B1", true, Option("description of subcategory B1"))
  val catB2 = Category(AlphanumericId("SUBCAT_B2"), AlphanumericId("GRUPO_B"), "Category B2", true, Option("description of subcategory B2"))
  val catB3 = Category(AlphanumericId("SUBCAT_B3"), AlphanumericId("GRUPO_B"), "Category B3", true, Option("description of subcategory B3"))
  val groupB = Group(
    AlphanumericId("CATEGORY_B"),
    "Group B",
    Option("Description catgeroria B"))

  val analyses: Option[List[Analysis]] = Some(List[Analysis]())
  val sampleCode = new SampleCode("AR-B-LAB-1")

  val categoryList = Seq(
    (groupA, catA1),
    (groupA, catA2),
    (groupA, catA3),
    (groupB, catB1),
    (groupB, catB2),
    (groupB, catB3))

  val categoryTree: Category.CategoryTree = Map(
    groupA -> Seq(catA1, catA2, catA3),
    groupB -> Seq(catB1, catB2, catB3))

  val categoryMap = Map(fullCatA1.id -> fullCatA1, fullCatA2.id -> fullCatA2)

  val fullCategoryMap = Map(fullCatA1.id -> fullCatA1, fullCatA2.id -> fullCatA2, fullCatMixture.id -> fullCatMixture, fullCatMixtureVictim.id -> fullCatMixtureVictim)

  val fullLocus = List(FullLocus(Locus("LOCUS 1", "LOCUS 1", Some("1"), 1, 2, 1), List.empty[String], List.empty[LocusLink]),
                      FullLocus(Locus("LOCUS 2", "LOCUS 2", Some("2"), 2, 3, 1), List.empty[String], List.empty[LocusLink]))

  val locus = List(Locus("LOCUS 1", "LOCUS 1", Some("1"), 2, 3, 1),
    Locus("LOCUS 2", "LOCUS 2", Some("1"), 2, 3, 1),
    Locus("LOCUS 3", "LOCUS 3", Some("1"), 2, 3, 1),
    Locus("LOCUS 4", "LOCUS 4", Some("1"), 2, 3, 1),
    Locus("LOCUS 5", "LOCUS 5", Some("1"), 2, 3, 1),
    Locus("LOCUS 6", "LOCUS 6", Some("1"), 2, 3, 1),
    Locus("LOCUS 7", "LOCUS 7", Some("1"), 2, 3, 1))

  val analysisTypes = List(AnalysisType(1, "Autosomal"),
                          AnalysisType(2, "Cx"),
                          AnalysisType(3, "Cy"),
                          AnalysisType(4, "MT", true))

  val opLogRecord = new OperationLogEntry(1, "user1", None, new Date(), "GET", "/some", "controller.Action()", "developbuild", Some("Ok"), 200, 1L, "Obtener algo")
  val signedOpLogRecord = new SignedOperationLogEntry(1, "user1", None, new Date(), "GET", "/some", "controller.Action()", "developbuild", Some("Ok"), 200, 1L, Key("03d98620-eacc4bbd"), "ObtenerAlgo")

  val ldapUser = LdapUser("user", "username", "lastname",
    "usl@example.com", Seq("clerk", "geneticist"),
    "genId", "41188080", None, UserStatus.active,
    Array.emptyByteArray, Array.emptyByteArray,
    Array.emptyByteArray)

  val requestToken = new RequestToken("123")
  val totpToken = new TotpToken("123456")
  val ivAp = "e932d6e8f6920f1efa4103226391a570"
  val keyAp = "9e3ba370183beccf7df540ca812b3985"
  val verifierAp = "d59b81ea658b20e4d3a1712b75bb21d5"

  val laboratory = new Laboratory("nombre", "NME", "AR", "C", "address 123", "541112345678", Email("mail@example.com"), 0.6, 0.4, Some(false))
  val instanceLaboratory = new Laboratory("nombre", "SHDG", "AR", "C", "address 123", "541112345678", Email("mail@example.com"), 0.6, 0.4)

  val geneticist = new Geneticist("Siebert", "SHDG", "Caroline", Email("mail@example.com"), "123412345", None)

  val authPair = new AuthenticatedPair(verifierAp, keyAp, ivAp)

  val userPassword = UserPassword("user", "pass", totpToken)

  val pmdf = new DataFiliation(Some("Juan Perez"), Some("Don Juan"), Some(new Date(0)), Some("Morón"), Some("Argentina"), Some("DNI XX.XXX.XXX"), Some("R.C.P."), Some("direccion"), List(), List(), List())
  val pmdfa = new DataFiliationAttempt(pmdf.fullName, pmdf.nickname, pmdf.birthday, pmdf.birthPlace, pmdf.nationality, pmdf.identificationIssuingAuthority, pmdf.identification, pmdf.address, "inprints", "pictures", "signatures")

  val userPermissions: Set[Permission] = Set(Permission.DNA_PROFILE_CRUD, Permission.MATCHES_MANAGER)

  val user = new User("user", "user", "lastname", "mail@example.com", "user", Seq("geneticist"), userPermissions, UserStatus.active, "41188080")

  val userCredentials = UserCredentials(Array.emptyByteArray, Array.emptyByteArray, "CRII5DCIVF4WPP2R")

  val fullUser = FullUser(user, userCredentials, authPair)

  val sdf = new SimpleDateFormat("yyyy-MM-dd")

  val onlyDate = sdf.parse("2015-01-01")

  val mismatches: Profile.Mismatch = Map("MULTIPLE" -> 0, "SOSPECHOSO" -> 0)

  val matchGroup = MatchCard(SampleCode("AR-B-SHDG-500"), 2, 0, 0, 1, 2, "muestra", AlphanumericId("SOSPECHOSO"), new Date(), "SHDG", "jerkovicm")

  val matchResult = MatchResult(MongoId("54eb50cc2cdc8a94c6ee794b"),
    MongoDate(new Date()), 1,
    MatchingProfile(SampleCode("AR-C-HIBA-500"),"tst-admintist",MatchStatus.pending,None,catA1.id),
    MatchingProfile(SampleCode("AR-B-IMBICE-500"),"tst-genetist",MatchStatus.pending,None,catA1.id),
    NewMatchingResult(ModerateStringency,
      Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI), 1)

  val matchingResult = MatchingResult("54eb50cc2cdc8a94c6ee794b",SampleCode("AR-B-IMBICE-500"),"BI400",ModerateStringency,Map("TH01" -> ModerateStringency, "CSF1PO" -> ModerateStringency, "D18S51" -> ModerateStringency, "AMEL" -> HighStringency, "D8S1179" -> ModerateStringency, "D3S1358" -> ModerateStringency, "D7S820" -> ModerateStringency, "FGA" -> ModerateStringency, "D16S539" -> HighStringency, "D13S317" -> ModerateStringency, "vWA" -> ModerateStringency, "D5S818" -> ModerateStringency, "TPOX" -> ModerateStringency, "D21S11" -> ModerateStringency),14,AlphanumericId("SOSPECHOSO"), MatchStatus.hit, MatchStatus.pending, MatchGlobalStatus.pending,0.7023809523809523,1, true, Algorithm.ENFSI, 1)

  val scenarioOption = ScenarioOption(SampleCode("AR-C-HIBA-500"), "SAMPLE-CODE-500", AlphanumericId("SOSPECHOSO"), 0.7023809523809523, 1, 5, false)

//  val profileDataAttempt = ProfileDataAttempt(catA1.id, Option("attorney"), Option("SANGRE"), Option("court"), Option("ASESINATO"), Option("PERSONAS"), Option("criminalCase"), "internalSampleCode", "assignee", Some("SHDG"), Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate), Option(onlyDate), Some(pmdfa))
  val profileDataAttempt = ProfileDataAttempt(catA1.id, Option("attorney"), Option("SANGRE"), Option("court"), Option("ASESINATO"), Option("PERSONAS"), Option("criminalCase"), "internalSampleCode", "assignee", Some("SHDG"), Some(1l), Some(onlyDate), Some(onlyDate), Option(onlyDate), Some(pmdfa))
  val profileData = ProfileData(catA1.id, SampleCode("AR-C-SHDG-1100"),
    Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
    Option("PERSONAS"), Option("case number"), "internalSampleCode", "assignee", "SHDG",
    false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
    Option(onlyDate), Some(pmdf),false)
  val profileDataFull = ProfileDataFull(catA1.id, SampleCode("AR-C-SHDG-1100"),
    Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
    Option("PERSONAS"), Option("case number"), "internalSampleCode", "assignee", "SHDG",
    false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
    Option(onlyDate), Some(pmdf),false,false)
  val profileData2nd = ProfileData(catA1.id, SampleCode("AR-C-SHDG-1101"),
    Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
    Option("PERSONAS"), Option("case number"), "internalSampleCode", "assignee", "SHDG",
    false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
    Option(onlyDate), Some(pmdf),false)
  val profileData2ndFull = ProfileDataFull(catA1.id, SampleCode("AR-C-SHDG-1101"),
    Option("attorney"), Option("SANGRE"), Option("court desc"), Option("ASESINATO"),
    Option("PERSONAS"), Option("case number"), "internalSampleCode", "assignee", "SHDG",
    false, None, Some("responsibleGeneticist"), Some(onlyDate), Some(onlyDate),
    Option(onlyDate), Some(pmdf),false,false)
  val seqPopulation: Seq[PopulationSampleFrequency] = List(
    PopulationSampleFrequency("TPOX", 5, BigDecimal("0.00016000")),
    PopulationSampleFrequency("TPOX", 6, BigDecimal("0.00261000")),
    PopulationSampleFrequency("TPOX", 7, BigDecimal("0.00142000")),
    PopulationSampleFrequency("TPOX", 8, BigDecimal("0.48349000")),
    PopulationSampleFrequency("TPOX", 9, BigDecimal("0.07790000")),
    PopulationSampleFrequency("TPOX", 10, BigDecimal("0.04761000")),
    PopulationSampleFrequency("TPOX", 11, BigDecimal("0.28963000")),
    PopulationSampleFrequency("TPOX", 12, BigDecimal("0.09435000")),
    PopulationSampleFrequency("TPOX", 13, BigDecimal("0.00256000")),
    PopulationSampleFrequency("TPOX", 14, BigDecimal("0.00011000")),
    PopulationSampleFrequency("TPOX", 16, BigDecimal("0.00005000")),
    PopulationSampleFrequency("TPOX", 21, BigDecimal("0.00005000")),
    PopulationSampleFrequency("TPOX", 21.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D3S1358", 10, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D3S1358", 11, BigDecimal("0.00055000")),
    PopulationSampleFrequency("D3S1358", 12, BigDecimal("0.00186000")),
    PopulationSampleFrequency("D3S1358", 13, BigDecimal("0.00399000")),
    PopulationSampleFrequency("D3S1358", 14, BigDecimal("0.07485000")),
    PopulationSampleFrequency("D3S1358", 15, BigDecimal("0.35112000")),
    PopulationSampleFrequency("D3S1358", 16, BigDecimal("0.27983000")),
    PopulationSampleFrequency("D3S1358", 17, BigDecimal("0.16096000")),
    PopulationSampleFrequency("D3S1358", 18, BigDecimal("0.11706000")),
    PopulationSampleFrequency("D3S1358", 19, BigDecimal("0.00891000")),
    PopulationSampleFrequency("D3S1358", 20, BigDecimal("0.00071000")),
    PopulationSampleFrequency("D3S1358", 21, BigDecimal("0.00011000")),
    PopulationSampleFrequency("FGA", 16, BigDecimal("0.00038000")),
    PopulationSampleFrequency("FGA", 17, BigDecimal("0.00223000")),
    PopulationSampleFrequency("FGA", 18, BigDecimal("0.01084000")),
    PopulationSampleFrequency("FGA", 18.2, BigDecimal("0.00038000")),
    PopulationSampleFrequency("FGA", 19, BigDecimal("0.08764000")),
    PopulationSampleFrequency("FGA", 19.2, BigDecimal("0.00016000")),
    PopulationSampleFrequency("FGA", 20, BigDecimal("0.09248000")),
    PopulationSampleFrequency("FGA", 20.2, BigDecimal("0.00022000")),
    PopulationSampleFrequency("FGA", 21, BigDecimal("0.14417000")),
    PopulationSampleFrequency("FGA", 21.2, BigDecimal("0.00202000")),
    PopulationSampleFrequency("FGA", 22, BigDecimal("0.12609000")),
    PopulationSampleFrequency("FGA", 22.2, BigDecimal("0.00398000")),
    PopulationSampleFrequency("FGA", 22.3, BigDecimal("0.00005000")),
    PopulationSampleFrequency("FGA", 23, BigDecimal("0.12130000")),
    PopulationSampleFrequency("FGA", 23.2, BigDecimal("0.00245000")),
    PopulationSampleFrequency("FGA", 24, BigDecimal("0.15153000")),
    PopulationSampleFrequency("FGA", 24.2, BigDecimal("0.00071000")),
    PopulationSampleFrequency("FGA", 25, BigDecimal("0.15044000")),
    PopulationSampleFrequency("FGA", 25.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("FGA", 25.3, BigDecimal("0.00005000")),
    PopulationSampleFrequency("FGA", 26, BigDecimal("0.07375000")),
    PopulationSampleFrequency("FGA", 26.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("FGA", 27, BigDecimal("0.02200000")),
    PopulationSampleFrequency("FGA", 28, BigDecimal("0.00599000")),
    PopulationSampleFrequency("FGA", 29, BigDecimal("0.00054000")),
    PopulationSampleFrequency("FGA", 30, BigDecimal("0.00016000")),
    PopulationSampleFrequency("FGA", 31, BigDecimal("0.00022000")),
    PopulationSampleFrequency("D5S818", 7, BigDecimal("0.06930000")),
    PopulationSampleFrequency("D5S818", 8, BigDecimal("0.00635000")),
    PopulationSampleFrequency("D5S818", 9, BigDecimal("0.04210000")),
    PopulationSampleFrequency("D5S818", 10, BigDecimal("0.05326000")),
    PopulationSampleFrequency("D5S818", 11, BigDecimal("0.42216000")),
    PopulationSampleFrequency("D5S818", 12, BigDecimal("0.27108000")),
    PopulationSampleFrequency("D5S818", 13, BigDecimal("0.12503000")),
    PopulationSampleFrequency("D5S818", 14, BigDecimal("0.00843000")),
    PopulationSampleFrequency("D5S818", 15, BigDecimal("0.00208000")),
    PopulationSampleFrequency("D5S818", 16, BigDecimal("0.00022000")),
    PopulationSampleFrequency("CSF1PO", 0, BigDecimal("0.00011000")),
    PopulationSampleFrequency("CSF1PO", 6, BigDecimal("0.00006000")),
    PopulationSampleFrequency("CSF1PO", 7, BigDecimal("0.00167000")),
    PopulationSampleFrequency("CSF1PO", 8, BigDecimal("0.00448000")),
    PopulationSampleFrequency("CSF1PO", 8.3, BigDecimal("0.00006000")),
    PopulationSampleFrequency("CSF1PO", 9, BigDecimal("0.02185000")),
    PopulationSampleFrequency("CSF1PO", 10, BigDecimal("0.26929000")),
    PopulationSampleFrequency("CSF1PO", 10.3, BigDecimal("0.00006000")),
    PopulationSampleFrequency("CSF1PO", 11, BigDecimal("0.28222000")),
    PopulationSampleFrequency("CSF1PO", 12, BigDecimal("0.34753000")),
    PopulationSampleFrequency("CSF1PO", 13, BigDecimal("0.06209000")),
    PopulationSampleFrequency("CSF1PO", 14, BigDecimal("0.00868000")),
    PopulationSampleFrequency("CSF1PO", 15, BigDecimal("0.00167000")),
    PopulationSampleFrequency("D7S520", 6, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D7S520", 7, BigDecimal("0.01408000")),
    PopulationSampleFrequency("D7S520", 8, BigDecimal("0.10337000")),
    PopulationSampleFrequency("D7S520", 9, BigDecimal("0.08820000")),
    PopulationSampleFrequency("D7S520", 9.1, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D7S520", 10, BigDecimal("0.26243000")),
    PopulationSampleFrequency("D7S520", 11, BigDecimal("0.30968000")),
    PopulationSampleFrequency("D7S520", 12, BigDecimal("0.18630000")),
    PopulationSampleFrequency("D7S520", 12.1, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D7S520", 12.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D7S520", 12.3, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D7S520", 13, BigDecimal("0.03094000")),
    PopulationSampleFrequency("D7S520", 14, BigDecimal("0.00462000")),
    PopulationSampleFrequency("D7S520", 15, BigDecimal("0.00016000")),
    PopulationSampleFrequency("D8S1179", 8, BigDecimal("0.00898000")),
    PopulationSampleFrequency("D8S1179", 9, BigDecimal("0.00762000")),
    PopulationSampleFrequency("D8S1179", 10, BigDecimal("0.06872000")),
    PopulationSampleFrequency("D8S1179", 11, BigDecimal("0.07247000")),
    PopulationSampleFrequency("D8S1179", 12, BigDecimal("0.14353000")),
    PopulationSampleFrequency("D8S1179", 13, BigDecimal("0.30446000")),
    PopulationSampleFrequency("D8S1179", 14, BigDecimal("0.22236000")),
    PopulationSampleFrequency("D8S1179", 15, BigDecimal("0.13732000")),
    PopulationSampleFrequency("D8S1179", 16, BigDecimal("0.03047000")),
    PopulationSampleFrequency("D8S1179", 17, BigDecimal("0.00337000")),
    PopulationSampleFrequency("D8S1179", 18, BigDecimal("0.00065000")),
    PopulationSampleFrequency("D8S1179", 19, BigDecimal("0.00005000")),
    PopulationSampleFrequency("TH01", 4, BigDecimal("0.00006000")),
    PopulationSampleFrequency("TH01", 5, BigDecimal("0.00064000")),
    PopulationSampleFrequency("TH01", 6, BigDecimal("0.29564000")),
    PopulationSampleFrequency("TH01", 7, BigDecimal("0.26031000")),
    PopulationSampleFrequency("TH01", 8, BigDecimal("0.08001000")),
    PopulationSampleFrequency("TH01", 9, BigDecimal("0.12342000")),
    PopulationSampleFrequency("TH01", 9.3, BigDecimal("0.23091000")),
    PopulationSampleFrequency("TH01", 10, BigDecimal("0.00808000")),
    PopulationSampleFrequency("TH01", 11, BigDecimal("0.00070000")),
    PopulationSampleFrequency("TH01", 13.3, BigDecimal("0.00006000")),
    PopulationSampleFrequency("vWA", 11, BigDecimal("0.00087000")),
    PopulationSampleFrequency("vWA", 12, BigDecimal("0.00082000")),
    PopulationSampleFrequency("vWA", 13, BigDecimal("0.00414000")),
    PopulationSampleFrequency("vWA", 14, BigDecimal("0.06419000")),
    PopulationSampleFrequency("vWA", 15, BigDecimal("0.09430000")),
    PopulationSampleFrequency("vWA", 16, BigDecimal("0.30923000")),
    PopulationSampleFrequency("vWA", 17, BigDecimal("0.28278000")),
    PopulationSampleFrequency("vWA", 18, BigDecimal("0.16705000")),
    PopulationSampleFrequency("vWA", 19, BigDecimal("0.06425000")),
    PopulationSampleFrequency("vWA", 20, BigDecimal("0.01123000")),
    PopulationSampleFrequency("vWA", 21, BigDecimal("0.00093000")),
    PopulationSampleFrequency("vWA", 22, BigDecimal("0.00005000")),
    PopulationSampleFrequency("vWA", 23, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D13S317", 6, BigDecimal("0.00054000")),
    PopulationSampleFrequency("D13S317", 7, BigDecimal("0.00027000")),
    PopulationSampleFrequency("D13S317", 8, BigDecimal("0.09117000")),
    PopulationSampleFrequency("D13S317", 9, BigDecimal("0.15994000")),
    PopulationSampleFrequency("D13S317", 10, BigDecimal("0.07562000")),
    PopulationSampleFrequency("D13S317", 11, BigDecimal("0.22469000")),
    PopulationSampleFrequency("D13S317", 12, BigDecimal("0.24182000")),
    PopulationSampleFrequency("D13S317", 13, BigDecimal("0.12787000")),
    PopulationSampleFrequency("D13S317", 14, BigDecimal("0.07606000")),
    PopulationSampleFrequency("D13S317", 15, BigDecimal("0.00196000")),
    PopulationSampleFrequency("D13S317", 16, BigDecimal("0.00005000")),
    PopulationSampleFrequency("Penta E", 5, BigDecimal("0.03706000")),
    PopulationSampleFrequency("Penta E", 6, BigDecimal("0.00051000")),
    PopulationSampleFrequency("Penta E", 7, BigDecimal("0.09435000")),
    PopulationSampleFrequency("Penta E", 8, BigDecimal("0.02785000")),
    PopulationSampleFrequency("Penta E", 9, BigDecimal("0.00695000")),
    PopulationSampleFrequency("Penta E", 10, BigDecimal("0.06017000")),
    PopulationSampleFrequency("Penta E", 11, BigDecimal("0.08780000")),
    PopulationSampleFrequency("Penta E", 12, BigDecimal("0.18220000")),
    PopulationSampleFrequency("Penta E", 13, BigDecimal("0.09463000")),
    PopulationSampleFrequency("Penta E", 13.2, BigDecimal("0.00011000")),
    PopulationSampleFrequency("Penta E", 14, BigDecimal("0.07299000")),
    PopulationSampleFrequency("Penta E", 15, BigDecimal("0.09814000")),
    PopulationSampleFrequency("Penta E", 16, BigDecimal("0.05927000")),
    PopulationSampleFrequency("Penta E", 16.3, BigDecimal("0.00090000")),
    PopulationSampleFrequency("Penta E", 16.4, BigDecimal("0.00006000")),
    PopulationSampleFrequency("Penta E", 17, BigDecimal("0.05322000")),
    PopulationSampleFrequency("Penta E", 18, BigDecimal("0.03859000")),
    PopulationSampleFrequency("Penta E", 19, BigDecimal("0.02616000")),
    PopulationSampleFrequency("Penta E", 20, BigDecimal("0.02537000")),
    PopulationSampleFrequency("Penta E", 21, BigDecimal("0.02107000")),
    PopulationSampleFrequency("Penta E", 22, BigDecimal("0.00757000")),
    PopulationSampleFrequency("Penta E", 23, BigDecimal("0.00345000")),
    PopulationSampleFrequency("Penta E", 24, BigDecimal("0.00113000")),
    PopulationSampleFrequency("Penta E", 25, BigDecimal("0.00028000")),
    PopulationSampleFrequency("Penta E", 26, BigDecimal("0.00006000")),
    PopulationSampleFrequency("D16S539", 5, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D16S539", 7, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D16S539", 8, BigDecimal("0.01758000")),
    PopulationSampleFrequency("D16S539", 9, BigDecimal("0.16177000")),
    PopulationSampleFrequency("D16S539", 10, BigDecimal("0.10915000")),
    PopulationSampleFrequency("D16S539", 11, BigDecimal("0.27673000")),
    PopulationSampleFrequency("D16S539", 12, BigDecimal("0.27623000")),
    PopulationSampleFrequency("D16S539", 13, BigDecimal("0.13790000")),
    PopulationSampleFrequency("D16S539", 14, BigDecimal("0.01917000")),
    PopulationSampleFrequency("D16S539", 15, BigDecimal("0.00131000")),
    PopulationSampleFrequency("D18S51", 9, BigDecimal("0.00038000")),
    PopulationSampleFrequency("D18S51", 10, BigDecimal("0.00728000")),
    PopulationSampleFrequency("D18S51", 10.2, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D18S51", 11, BigDecimal("0.01144000")),
    PopulationSampleFrequency("D18S51", 12, BigDecimal("0.12708000")),
    PopulationSampleFrequency("D18S51", 13, BigDecimal("0.11537000")),
    PopulationSampleFrequency("D18S51", 13.2, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D18S51", 14, BigDecimal("0.20496000")),
    PopulationSampleFrequency("D18S51", 14.2, BigDecimal("0.00016000")),
    PopulationSampleFrequency("D18S51", 15, BigDecimal("0.14191000")),
    PopulationSampleFrequency("D18S51", 15.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D18S51", 16, BigDecimal("0.12002000")),
    PopulationSampleFrequency("D18S51", 16.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D18S51", 17, BigDecimal("0.12407000")),
    PopulationSampleFrequency("D18S51", 18, BigDecimal("0.06715000")),
    PopulationSampleFrequency("D18S51", 19, BigDecimal("0.03639000")),
    PopulationSampleFrequency("D18S51", 20, BigDecimal("0.01954000")),
    PopulationSampleFrequency("D18S51", 21, BigDecimal("0.01144000")),
    PopulationSampleFrequency("D18S51", 22, BigDecimal("0.00859000")),
    PopulationSampleFrequency("D18S51", 22.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D18S51", 23, BigDecimal("0.00235000")),
    PopulationSampleFrequency("D18S51", 24, BigDecimal("0.00104000")),
    PopulationSampleFrequency("D18S51", 25, BigDecimal("0.00016000")),
    PopulationSampleFrequency("D18S51", 26, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D18S51", 27, BigDecimal("0.00016000")),
    PopulationSampleFrequency("PentaD", 2.2, BigDecimal("0.00358000")),
    PopulationSampleFrequency("PentaD", 3.2, BigDecimal("0.00011000")),
    PopulationSampleFrequency("PentaD", 5, BigDecimal("0.00146000")),
    PopulationSampleFrequency("PentaD", 6, BigDecimal("0.00039000")),
    PopulationSampleFrequency("PentaD", 7, BigDecimal("0.01014000")),
    PopulationSampleFrequency("PentaD", 8, BigDecimal("0.01366000")),
    PopulationSampleFrequency("PentaD", 9, BigDecimal("0.19036000")),
    PopulationSampleFrequency("PentaD", 9.2, BigDecimal("0.00056000")),
    PopulationSampleFrequency("PentaD", 10, BigDecimal("0.20671000")),
    PopulationSampleFrequency("PentaD", 11, BigDecimal("0.16045000")),
    PopulationSampleFrequency("PentaD", 12, BigDecimal("0.17008000")),
    PopulationSampleFrequency("PentaD", 13, BigDecimal("0.16840000")),
    PopulationSampleFrequency("PentaD", 14, BigDecimal("0.05410000")),
    PopulationSampleFrequency("PentaD", 15, BigDecimal("0.01512000")),
    PopulationSampleFrequency("PentaD", 16, BigDecimal("0.00364000")),
    PopulationSampleFrequency("PentaD", 17, BigDecimal("0.00090000")),
    PopulationSampleFrequency("PentaD", 18, BigDecimal("0.00017000")),
    PopulationSampleFrequency("PentaD", 19, BigDecimal("0.00006000")),
    PopulationSampleFrequency("D21S11", 19, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D21S11", 24, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D21S11", 24.2, BigDecimal("0.00087000")),
    PopulationSampleFrequency("D21S11", 25, BigDecimal("0.00033000")),
    PopulationSampleFrequency("D21S11", 25.2, BigDecimal("0.00054000")),
    PopulationSampleFrequency("D21S11", 26, BigDecimal("0.00158000")),
    PopulationSampleFrequency("D21S11", 26.2, BigDecimal("0.00016000")),
    PopulationSampleFrequency("D21S11", 27, BigDecimal("0.01638000")),
    PopulationSampleFrequency("D21S11", 27.2, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D21S11", 28, BigDecimal("0.09375000")),
    PopulationSampleFrequency("D21S11", 28.2, BigDecimal("0.00076000")),
    PopulationSampleFrequency("D21S11", 29, BigDecimal("0.19643000")),
    PopulationSampleFrequency("D21S11", 29.2, BigDecimal("0.00207000")),
    PopulationSampleFrequency("D21S11", 30, BigDecimal("0.27228000")),
    PopulationSampleFrequency("D21S11", 30.2, BigDecimal("0.02536000")),
    PopulationSampleFrequency("D21S11", 31, BigDecimal("0.06209000")),
    PopulationSampleFrequency("D21S11", 31.2, BigDecimal("0.11312000")),
    PopulationSampleFrequency("D21S11", 32, BigDecimal("0.00920000")),
    PopulationSampleFrequency("D21S11", 32.1, BigDecimal("0.00016000")),
    PopulationSampleFrequency("D21S11", 32.2, BigDecimal("0.13843000")),
    PopulationSampleFrequency("D21S11", 33, BigDecimal("0.00212000")),
    PopulationSampleFrequency("D21S11", 33.1, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D21S11", 33.2, BigDecimal("0.05430000")),
    PopulationSampleFrequency("D21S11", 33.3, BigDecimal("0.00011000")),
    PopulationSampleFrequency("D21S11", 34, BigDecimal("0.00044000")),
    PopulationSampleFrequency("D21S11", 34.2, BigDecimal("0.00637000")),
    PopulationSampleFrequency("D21S11", 35, BigDecimal("0.00103000")),
    PopulationSampleFrequency("D21S11", 35.1, BigDecimal("0.00027000")),
    PopulationSampleFrequency("D21S11", 35.2, BigDecimal("0.00087000")),
    PopulationSampleFrequency("D21S11", 36, BigDecimal("0.00027000")),
    PopulationSampleFrequency("D21S11", 36.2, BigDecimal("0.00005000")),
    PopulationSampleFrequency("D21S11", 37, BigDecimal("0.00005000")))

  val populationBaseFrequency = new PopulationBaseFrequency("pop freq 1", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

  val baseFrequency = PopulationBaseFrequencyNameView("pop freq 1", 0, "HardyWeinberg", true, true)

  val genotypification: GenotypificationByType = Map( 1 -> Map(
    "LOCUS 1" -> List(Allele(2.1), Allele(3)),
    "LOCUS 2" -> List(Allele(2), Allele(3)),
    "LOCUS 3" -> List(Allele(1), XY('X')),
    "LOCUS 4" -> List(Mitocondrial('A', 1), Mitocondrial('A', 2.1), Mitocondrial('-', 3)),
    "LOCUS 5" -> List(Allele(1), Allele(3))))

  def newProfile = {
    val random = new Random()
    val nextRanInt = Math.abs(random.nextInt)

    val id: String = "AR-B-LAB-" + nextRanInt.toString //UUID.randomUUID().toString() 
    Profile(SampleCode(id), SampleCode(id), "", "", catA1.id, genotypification, analyses, None, None, None, None, None, false, true, false)
  }

  val newAnalysis = NewAnalysis(
    sampleCode,
    "pdg",
    "token",
    Some("Identifiler"),
    None,
    genotypification(1),
    None,
    Some(1),
    None)

  val labeledGenotypification: Profile.LabeledGenotypification = Map(
    "label1" ->
      Map("LOCUS 1" -> List(Allele(1)),
        "LOCUS 2" -> List(Allele(1))),
    "label2" ->
      Map("LOCUS 1" -> List(Allele(2.1)),
        "LOCUS 2" -> List(Allele(5))))

  val profileAssocGenotypification: Profile.LabeledGenotypification = Map(
    "XX-X-XXXX-1" ->
      Map("LOCUS 1" -> List(Allele(1)),
        "LOCUS 2" -> List(Allele(1))))

  val newAnalysisLabeledGenotypification = NewAnalysis(
    sampleCode,
    "pdg",
    "token",
    Some("Identifiler"),
    None,
    genotypification(1),
    Some(labeledGenotypification),
    Some(2),
    None)

  def newProfileLabeledGenotypification = {
    val random = new Random()
    val nextRanInt = Math.abs(random.nextInt)

    val id: String = "AR-B-LAB-" + nextRanInt.toString //UUID.randomUUID().toString() 
    Profile(SampleCode(id), SampleCode(id), "", "", catA1.id, genotypification, analyses, Some(labeledGenotypification), Some(2), None, None, None, false, true)
  }

  def mixtureGenotypification = Map(1 -> Map("LOCUS 1" -> List(Allele(11), Allele(12), Allele(13), Allele(14)),
    "LOCUS 2" -> List(Allele(21), Allele(22), Allele(23), Allele(24))))

  def mix1Genotypification = Map(1 -> Map("LOCUS 1" -> List(Allele(11), Allele(12)),
    "LOCUS 2" -> List(Allele(21), Allele(22))))

  def mix2Genotypification = Map(1 -> Map("LOCUS 1" -> List(Allele(13), Allele(14)),
    "LOCUS 2" -> List(Allele(23), Allele(24))))

  def newProfile(subcatId: AlphanumericId, genot: GenotypificationByType) = {
    val random = new Random()
    val nextRanInt = Math.abs(random.nextInt)

    val id: String = "AR-B-LAB-" + nextRanInt.toString
    Profile(SampleCode(id), SampleCode(id), "", "", subcatId, genot, analyses, None, None, None, None, None, false, true, true)
  }

  val mixtureProfile = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("MULTIPLE"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(11), Allele(10)),
      "D3S1358" -> List(Allele(18), Allele(15), Allele(16)),
      "FGA" -> List(Allele(23), Allele(24), Allele(22)),
      "D5S818" -> List(Allele(12), Allele(11), Allele(13)),
      "CSF1PO" -> List(Allele(12), Allele(9)),
      "D7S820" -> List(Allele(11), Allele(10)),
      "D8S1179" -> List(Allele(13), Allele(11), Allele(12)),
      "TH01" -> List(Allele(6), Allele(7), Allele(8)),
      "vWA" -> List(Allele(12), Allele(17), Allele(16)),
      "D13S317" -> List(Allele(14), Allele(9), Allele(10), Allele(11)),
      "D16S539" -> List(Allele(9), Allele(13), Allele(12)),
      "D18S51" -> List(Allele(12), Allele(15), Allele(14)),
      "D21S11" -> List(Allele(28), Allele(29), Allele(31.2)))), analyses, None, Some(2), None, None, None, false, true, false)

  val mixtureP1 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("SOSPECHOSO"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(11)),
      "D3S1358" -> List(Allele(18)),
      "FGA" -> List(Allele(23), Allele(24)),
      "D5S818" -> List(Allele(12), Allele(11)),
      "CSF1PO" -> List(Allele(12)),
      "D7S820" -> List(Allele(11), Allele(10)),
      "D8S1179" -> List(Allele(13)),
      "TH01" -> List(Allele(6), Allele(7)),
      "vWA" -> List(Allele(12), Allele(17)),
      "D13S317" -> List(Allele(14), Allele(9)),
      "D16S539" -> List(Allele(9), Allele(13)),
      "D18S51" -> List(Allele(12), Allele(15)),
      "D21S11" -> List(Allele(28), Allele(29)),
      "AMEL" -> List(XY('X'), XY('Y')))), analyses, None, None, None, None, None, false, true, false)

  val mixtureP2 = Profile(Stubs.sampleCode, Stubs.sampleCode, "", "", AlphanumericId("SOSPECHOSO"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(10)),
      "D3S1358" -> List(Allele(15), Allele(16)),
      "FGA" -> List(Allele(23), Allele(22)),
      "D5S818" -> List(Allele(11), Allele(13)),
      "CSF1PO" -> List(Allele(9)),
      "D7S820" -> List(Allele(11), Allele(10)),
      "D8S1179" -> List(Allele(11), Allele(12)),
      "TH01" -> List(Allele(6), Allele(8)),
      "vWA" -> List(Allele(17), Allele(16)),
      "D13S317" -> List(Allele(10), Allele(11)),
      "D16S539" -> List(Allele(9), Allele(12)),
      "D18S51" -> List(Allele(14), Allele(14)),
      "D21S11" -> List(Allele(28), Allele(31.2)))), analyses, None, None, None, None, None, false, true, false)

  val mixtureVictimProfileNonAssociated = Profile(SampleCode("XX-X-ANY-1"), SampleCode("XX-X-ANY-1"), "", "",
    AlphanumericId("MULTIPLE_VICTIMA"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(11), Allele(10)))),
    None,
    None,
    Some(2),
    None, None, None, false, true, false)

  val mixtureVictimLabeled = Profile(SampleCode("XX-X-ANY-2"), SampleCode("XX-X-ANY-2"), "", "",
    AlphanumericId("MULTIPLE_VICTIMA"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(11), Allele(10)))),
    None,
    Some(labeledGenotypification),
    Some(2),
    None, None, None, false, true, false)

  val mixtureVictimProfileAssociated = Profile(SampleCode("XX-X-ANY-3"), SampleCode("XX-X-ANY-3"), "", "",
    AlphanumericId("MULTIPLE_VICTIMA"),
    Map( 1 -> Map("TPOX" -> List(Allele(8), Allele(11), Allele(10)))),
    None,
    Some(profileAssocGenotypification),
    Some(2),
    None, None, None, false, true, false)

  val listOfMinimumStringencies: Seq[MatchingRule] = Seq(
    MatchingRule(1, AlphanumericId("MULTIPLE"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 10, 0, true),
    MatchingRule(1, AlphanumericId("SOSPECHOSO"), Stringency.ModerateStringency, true, true, Algorithm.ENFSI, 10, 0, true))

  val fullKits = List(FullStrKit("KIT 1", "KIT 1", 1, 16, 14, Nil, Nil),
    FullStrKit("KIT 2", "KIT 2", 1, 21, 16, Nil, Nil))

  val strKits = List(StrKit("Identifiler", "Identifiler", 1, 16, 14),
    StrKit("Powerplex21", "Powerplex 21", 1, 21, 16))

  val loci = List(StrKitLocus("LOCUS 1", "LOCUS 1", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 2", "LOCUS 2", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 3", "LOCUS 3", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 4", "LOCUS 4", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 5", "LOCUS 5", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 6", "LOCUS 6", Some("1"), 2, 3, None, 1),
    StrKitLocus("LOCUS 7", "LOCUS 7", Some("1"), 2, 3, None, 1))

  class NotificationServiceMock(repository: NotificationRepository) extends NotificationService {
    override def search(notiSearch: NotificationSearch): Future[Seq[Notification]] = Future.successful(Seq.empty)
    override def count(notiSearch: NotificationSearch): Future[Int] = Future.successful(0)
    override def push(userId: String, info: NotificationInfo) = {}
    override def solve(userId: String, info: NotificationInfo) = {}
    override def delete(id: Long) = Future.successful(Right(id))
    override def changeFlag(id: Long, flag: Boolean) = Future.successful(Right(id))
    def getNotifications(userId: String): Enumerator[Notification] = Enumerator.empty
  }

  val notificationServiceMock = new NotificationServiceMock(null)

  val labelsSets: Profile.LabelSets = Map("set1" -> Map("1" -> Label("1", "Víctima"), "2" -> Label("2", "Obligado")),
    "set2" -> Map("3" -> Label("3", "Individuo 1"), "4" -> Label("4", "Individuo 1")))

  val seqSelectedProfiles: List[SampleCode] = List(
    Stubs.newProfile.globalCode,
    Stubs.newProfile.globalCode
  )

  val seqUnselectedProfiles: List[SampleCode] = List(
    Stubs.newProfile.globalCode,
    Stubs.newProfile.globalCode
  )
  val hypothesisProsecutor = Hypothesis(seqSelectedProfiles, seqUnselectedProfiles, 3, 0.4)

  val hypothesisDefense = Hypothesis(seqSelectedProfiles, seqUnselectedProfiles, 1, 0.5)
  val statOption = StatOption("A", "1", 1, 0.4, Some(0.5))

  val lrResult = LRResult(0.0, Map())
  val calculationScenario = CalculationScenario(Stubs.newProfile.globalCode, hypothesisProsecutor, hypothesisDefense, statOption)

  def newScenario = {
    Scenario(MongoId(new ObjectId().toString), "Scenario A", ScenarioStatus.Pending, "Genetista", calculationScenario, MongoDate(new Date()), false, Some(lrResult), Some("Description A"))
  }

  class MockCacheService extends CacheService {
    override def get[T](key: CacheKey[T])(implicit ct: ClassManifest[T]): Option[T] = ???
    override def set[T](key: CacheKey[T], value: T): Unit = ???
    override def getOrElse[T](key: CacheKey[T])(orElse: => T)(implicit ct: ClassManifest[T]): T = orElse
    override def asyncGetOrElse[T](key: CacheKey[T])(orElse: => Future[T])(implicit ct: ClassManifest[T]): Future[T] = orElse
    override def pop[T](key: CacheKey[T])(implicit ct: ClassManifest[T]): Option[T] = None
  }

  val cacheServiceMock = new MockCacheService()

  val courtCase = CourtCase(456l, "PEDIGREE", None, None, "pdg", None, None, None, PedigreeStatus.Open,
                            List(PersonData(None, None, None, None, None, None, None, None, None, None, None, None, None, None,"Pepe",None)), "MPI")

  val courtCaseFull = CourtCaseFull(123l, "internalSampleCode", None, None, "assignee", None, None, None,
    PedigreeStatus.Open, List(PersonData(None, None, None, None, None, None, None, None, None, None, None, None, None, None, "Pedro", None)), "MPI")

  val courtCaseModelView = CourtCaseModelView(456l, "PEDIGREE", None, None, "pdg", None, None, None, PedigreeStatus.UnderConstruction,"MPI")
  val courtCaseModelView2 = CourtCaseModelView(457l, "CASEDVI", None, None, "pdg", None, None, None, PedigreeStatus.UnderConstruction,"DVI")

  val pedigree = PedigreeDataCreation(PedigreeMetaData(456, 456, "Pedigree", new Date(), PedigreeStatus.UnderConstruction, "pdg"), None)

  class MockTraceService extends TraceService {
    override def add(trace: Trace): Future[Either[String, Long]] = Future.successful(Right(1l))
    override def count(traceSearch: TraceSearch): Future[Int] = Future.successful(0)
    override def search(traceSearch: TraceSearch): Future[Seq[Trace]] = Future.successful(Seq())
    override def getFullDescription(id: Long): Future[String] = Future.successful("Description")
    override def searchPedigree(traceSearch: TraceSearchPedigree): Future[Seq[TracePedigree]] = Future.successful(Nil)
    override def countPedigree(traceSearch: TraceSearchPedigree): Future[Int] =  Future.successful(0)
    override def addTracePedigree(trace: TracePedigree): Future[Either[String, Long]]  = Future.successful(Right(0l))
  }

  val traceServiceMock = new MockTraceService()

  val trace: Trace = Trace(SampleCode("AR-B-SHDG-1234"), "userId", new Date(), ProfileDataInfo)

  class MockMatchingService extends MatchingService {
    override def resumeFindingMatches(): Unit = {}
    override def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]] = ???
    override def getTotalMatches(search: MatchCardSearch): Future[Int] = ???
    override def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]] = ???
    override def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int] = ???
    override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] = ???
    override def findMatches(globalCode: SampleCode, matchType: Option[String]) = None
    override def findMatches(pedigreeId: Long): Unit = {}
    override def getComparedMixtureGenotypification(globalCodes: Seq[SampleCode],matchId:String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean]): Future[Seq[CompareMixtureGenotypification]] = ???
    override def getByMatchedProfileId(matchingId: String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Option[JsValue]] = ???
    override def convertHit(matchId: String, firingCode: SampleCode,replicate:Boolean = true): Future[Either[String, Seq[SampleCode]]] = ???
    override def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]] = ???
    override def validate(scenario: Scenario): Future[Either[String, String]] = ???
    override def convertDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean,replicate:Boolean = true): Future[Either[String, Seq[SampleCode]]] = ???
    override def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchingResult]] = ???
    override def deleteForce(matchId: String, globalCode: SampleCode): Future[Boolean] = ???
    override def validProfilesAssociated(labels: Option[LabeledGenotypification]): Seq[String] = ???
    override def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]] = Future.successful(None)
    override def convertHitOrDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean,action:String): Future[Either[String, Seq[SampleCode]]] = ???
    override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] = ???
    override def collapse(idCourtCase:Long,user:String):Unit = ()
    override def discardCollapsingMatches(ids:List[String],courtCaseId:Long) : Future[Unit] = Future.successful(())
    override def discardCollapsingByLeftProfile(id:String,courtCaseId:Long) : Future[Unit] = Future.successful(())
    override def discardCollapsingByLeftAndRightProfile(id:String,courtCaseId:Long) : Future[Unit] = Future.successful(())
    override def discardCollapsingByRightProfile(id:String,courtCaseId:Long) : Future[Unit] = Future.successful(())
    override def findScreeningMatches(profile:Profile, queryProfiles:List[String],numberOfMismatches: Option[Int]):Future[(Set[MatchResultScreening],Set[MatchResultScreening])] =  Future.successful((Set.empty,Set.empty))
    override def obtainCompareMixtureGenotypification(results:scala.Seq[Profile]):Seq[CompareMixtureGenotypification] = Nil
    override def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit] = Future.successful(())
    override def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]] = Future.successful(Seq.empty)
    override def getMatchesPaginated(search: MatchCardSearch): Future[Seq[MatchResult]] = Future.successful(Nil)
    override def getAllTotalMatches(search: MatchCardSearch): Future[Int] = Future.successful(1)

    override def masiveGroupDiscardByGlobalCode(firingCode: SampleCode, isSuperUser: Boolean, replicate: Boolean): Future[Either[String, Seq[SampleCode]]] = ???

    override def masiveGroupDiscardByMatchesList(firingCode: SampleCode, matches: List[String], isSuperUser: Boolean, replicate: Boolean): Future[Either[String, Seq[SampleCode]]] = ???
    override def uploadStatus(matchId: String, firingCode: SampleCode, isSuperUser: Boolean): Future[String] = ???
    override def canUploadMatchStatus(matchId: String, isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Boolean] = ???
  }

  val matchingServiceMock = new MockMatchingService()

  val pedigreeMatch = PedigreeDirectLinkMatch(MongoId("58ac62e6ebb12c3bfcc1afaa"),
    MongoDate(new Date()), 1,
    MatchingProfile(SampleCode("AR-C-SHDG-1102"), "jerkovicm", MatchStatus.pending, None,catA1.id),
    PedigreeMatchingProfile(12, NodeAlias("PI1"), SampleCode("AR-C-SHDG-1101"), "tst-admintist", MatchStatus.hit,"caseType",7l),
    NewMatchingResult(ModerateStringency, Map(),14,AlphanumericId("SOSPECHOSO"),0.7023809523809523,1.0,Algorithm.ENFSI))

}
