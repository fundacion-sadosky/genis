package unit.probability

import java.util.Date
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import configdata.{CategoryAssociation, CategoryConfiguration, FullCategory}
import kits.{QualityParamsProvider, StrKit, StrKitService}
import probability.ProbabilityServiceImpl
import profile.{Allele, Analysis}
import profiledata.ProfileDataRepository
import services.LaboratoryService
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationBaseFrequencyNameView, PopulationSampleFrequency, ProbabilityModel}
import types.{AlphanumericId, Laboratory, MongoDate, StatOption}
import configdata.CategoryService

class ProbabilityServiceTest extends AnyWordSpec with Matchers with MockitoSugar {

  given ec: ExecutionContext = ExecutionContext.global
  val timeout: Duration = Duration(10, "seconds")

  // ── Shared frequency data (13 markers, same as legacy test) ──────

  val seqPopulation: Seq[PopulationSampleFrequency] = List(
    PopulationSampleFrequency("TPOX", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX", 5,  BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX", 6,  BigDecimal(0.00261)),
    PopulationSampleFrequency("TPOX", 7,  BigDecimal(0.00142)),
    PopulationSampleFrequency("TPOX", 8,  BigDecimal(0.48349)),
    PopulationSampleFrequency("TPOX", 9,  BigDecimal(0.0779)),
    PopulationSampleFrequency("TPOX", 10, BigDecimal(0.04761)),
    PopulationSampleFrequency("TPOX", 11, BigDecimal(0.28963)),
    PopulationSampleFrequency("TPOX", 12, BigDecimal(0.09435)),
    PopulationSampleFrequency("TPOX", 13, BigDecimal(0.00256)),
    PopulationSampleFrequency("D3S1358", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D3S1358", 10, BigDecimal(0.000273373428102788)),
    PopulationSampleFrequency("D3S1358", 11, BigDecimal(0.00055)),
    PopulationSampleFrequency("D3S1358", 12, BigDecimal(0.00186)),
    PopulationSampleFrequency("D3S1358", 13, BigDecimal(0.00399)),
    PopulationSampleFrequency("D3S1358", 14, BigDecimal(0.07485)),
    PopulationSampleFrequency("D3S1358", 15, BigDecimal(0.35112)),
    PopulationSampleFrequency("D3S1358", 16, BigDecimal(0.27983)),
    PopulationSampleFrequency("D3S1358", 17, BigDecimal(0.16096)),
    PopulationSampleFrequency("D3S1358", 18, BigDecimal(0.11706)),
    PopulationSampleFrequency("D3S1358", 19, BigDecimal(0.00891)),
    PopulationSampleFrequency("D3S1358", 20, BigDecimal(0.00071)),
    PopulationSampleFrequency("FGA", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("FGA", 16, BigDecimal(0.00038)),
    PopulationSampleFrequency("FGA", 17, BigDecimal(0.00223)),
    PopulationSampleFrequency("FGA", 18, BigDecimal(0.01084)),
    PopulationSampleFrequency("FGA", 19, BigDecimal(0.08764)),
    PopulationSampleFrequency("FGA", 20, BigDecimal(0.09248)),
    PopulationSampleFrequency("FGA", 21, BigDecimal(0.14417)),
    PopulationSampleFrequency("FGA", 22, BigDecimal(0.12609)),
    PopulationSampleFrequency("FGA", 23, BigDecimal(0.1213)),
    PopulationSampleFrequency("FGA", 24, BigDecimal(0.15153)),
    PopulationSampleFrequency("FGA", 25, BigDecimal(0.15044)),
    PopulationSampleFrequency("FGA", 26, BigDecimal(0.07375)),
    PopulationSampleFrequency("FGA", 27, BigDecimal(0.022)),
    PopulationSampleFrequency("FGA", 28, BigDecimal(0.00599)),
    PopulationSampleFrequency("D5S818", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D5S818", 7,  BigDecimal(0.0693)),
    PopulationSampleFrequency("D5S818", 8,  BigDecimal(0.00635)),
    PopulationSampleFrequency("D5S818", 9,  BigDecimal(0.0421)),
    PopulationSampleFrequency("D5S818", 10, BigDecimal(0.05326)),
    PopulationSampleFrequency("D5S818", 11, BigDecimal(0.42216)),
    PopulationSampleFrequency("D5S818", 12, BigDecimal(0.27108)),
    PopulationSampleFrequency("D5S818", 13, BigDecimal(0.12503)),
    PopulationSampleFrequency("D5S818", 14, BigDecimal(0.00843)),
    PopulationSampleFrequency("D5S818", 15, BigDecimal(0.00208)),
    PopulationSampleFrequency("CSF1PO", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("CSF1PO", 6,  BigDecimal(0.000287455444406117)),
    PopulationSampleFrequency("CSF1PO", 7,  BigDecimal(0.00167)),
    PopulationSampleFrequency("CSF1PO", 8,  BigDecimal(0.00448)),
    PopulationSampleFrequency("CSF1PO", 9,  BigDecimal(0.02185)),
    PopulationSampleFrequency("CSF1PO", 10, BigDecimal(0.26929)),
    PopulationSampleFrequency("CSF1PO", 11, BigDecimal(0.28222)),
    PopulationSampleFrequency("CSF1PO", 12, BigDecimal(0.34753)),
    PopulationSampleFrequency("CSF1PO", 13, BigDecimal(0.06209)),
    PopulationSampleFrequency("CSF1PO", 14, BigDecimal(0.00868)),
    PopulationSampleFrequency("CSF1PO", 15, BigDecimal(0.00167)),
    PopulationSampleFrequency("D7S520", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D7S520", 6,  BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520", 7,  BigDecimal(0.01408)),
    PopulationSampleFrequency("D7S520", 8,  BigDecimal(0.10337)),
    PopulationSampleFrequency("D7S520", 9,  BigDecimal(0.0882)),
    PopulationSampleFrequency("D7S520", 10, BigDecimal(0.26243)),
    PopulationSampleFrequency("D7S520", 11, BigDecimal(0.30968)),
    PopulationSampleFrequency("D7S520", 12, BigDecimal(0.1863)),
    PopulationSampleFrequency("D7S520", 13, BigDecimal(0.03094)),
    PopulationSampleFrequency("D7S520", 14, BigDecimal(0.00462)),
    PopulationSampleFrequency("D8S1179", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D8S1179", 8,  BigDecimal(0.00898)),
    PopulationSampleFrequency("D8S1179", 9,  BigDecimal(0.00762)),
    PopulationSampleFrequency("D8S1179", 10, BigDecimal(0.06872)),
    PopulationSampleFrequency("D8S1179", 11, BigDecimal(0.07247)),
    PopulationSampleFrequency("D8S1179", 12, BigDecimal(0.14353)),
    PopulationSampleFrequency("D8S1179", 13, BigDecimal(0.30446)),
    PopulationSampleFrequency("D8S1179", 14, BigDecimal(0.22236)),
    PopulationSampleFrequency("D8S1179", 15, BigDecimal(0.13732)),
    PopulationSampleFrequency("D8S1179", 16, BigDecimal(0.03047)),
    PopulationSampleFrequency("D8S1179", 17, BigDecimal(0.00337)),
    PopulationSampleFrequency("TH01", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TH01", 4,   BigDecimal(0.000290528762347472)),
    PopulationSampleFrequency("TH01", 5,   BigDecimal(0.00064)),
    PopulationSampleFrequency("TH01", 6,   BigDecimal(0.29564)),
    PopulationSampleFrequency("TH01", 7,   BigDecimal(0.26031)),
    PopulationSampleFrequency("TH01", 8,   BigDecimal(0.08001)),
    PopulationSampleFrequency("TH01", 9,   BigDecimal(0.12342)),
    PopulationSampleFrequency("TH01", 9.3, BigDecimal(0.23091)),
    PopulationSampleFrequency("TH01", 10,  BigDecimal(0.00808)),
    PopulationSampleFrequency("TH01", 11,  BigDecimal(0.0007)),
    PopulationSampleFrequency("vWA", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("vWA", 11, BigDecimal(0.00087)),
    PopulationSampleFrequency("vWA", 12, BigDecimal(0.00082)),
    PopulationSampleFrequency("vWA", 13, BigDecimal(0.00414)),
    PopulationSampleFrequency("vWA", 14, BigDecimal(0.06419)),
    PopulationSampleFrequency("vWA", 15, BigDecimal(0.0943)),
    PopulationSampleFrequency("vWA", 16, BigDecimal(0.30923)),
    PopulationSampleFrequency("vWA", 17, BigDecimal(0.28278)),
    PopulationSampleFrequency("vWA", 18, BigDecimal(0.16705)),
    PopulationSampleFrequency("vWA", 19, BigDecimal(0.06425)),
    PopulationSampleFrequency("vWA", 20, BigDecimal(0.01123)),
    PopulationSampleFrequency("D13S317", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D13S317", 7,  BigDecimal(0.000271827769924976)),
    PopulationSampleFrequency("D13S317", 8,  BigDecimal(0.09117)),
    PopulationSampleFrequency("D13S317", 9,  BigDecimal(0.15994)),
    PopulationSampleFrequency("D13S317", 10, BigDecimal(0.07562)),
    PopulationSampleFrequency("D13S317", 11, BigDecimal(0.22469)),
    PopulationSampleFrequency("D13S317", 12, BigDecimal(0.24182)),
    PopulationSampleFrequency("D13S317", 13, BigDecimal(0.12787)),
    PopulationSampleFrequency("D13S317", 14, BigDecimal(0.07606)),
    PopulationSampleFrequency("D13S317", 15, BigDecimal(0.00196)),
    PopulationSampleFrequency("D16S539", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D16S539", 7,  BigDecimal(0.000273822562979189)),
    PopulationSampleFrequency("D16S539", 8,  BigDecimal(0.01758)),
    PopulationSampleFrequency("D16S539", 9,  BigDecimal(0.16177)),
    PopulationSampleFrequency("D16S539", 10, BigDecimal(0.10915)),
    PopulationSampleFrequency("D16S539", 11, BigDecimal(0.27673)),
    PopulationSampleFrequency("D16S539", 12, BigDecimal(0.27623)),
    PopulationSampleFrequency("D16S539", 13, BigDecimal(0.1379)),
    PopulationSampleFrequency("D16S539", 14, BigDecimal(0.01917)),
    PopulationSampleFrequency("D16S539", 15, BigDecimal(0.00131)),
    PopulationSampleFrequency("D18S51", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D18S51", 9,  BigDecimal(0.00038)),
    PopulationSampleFrequency("D18S51", 10, BigDecimal(0.00728)),
    PopulationSampleFrequency("D18S51", 11, BigDecimal(0.01144)),
    PopulationSampleFrequency("D18S51", 12, BigDecimal(0.12708)),
    PopulationSampleFrequency("D18S51", 13, BigDecimal(0.11537)),
    PopulationSampleFrequency("D18S51", 14, BigDecimal(0.20496)),
    PopulationSampleFrequency("D18S51", 15, BigDecimal(0.14191)),
    PopulationSampleFrequency("D18S51", 16, BigDecimal(0.12002)),
    PopulationSampleFrequency("D18S51", 17, BigDecimal(0.12407)),
    PopulationSampleFrequency("D18S51", 18, BigDecimal(0.06715)),
    PopulationSampleFrequency("D21S11", -1, BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D21S11", 24, BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11", 25, BigDecimal(0.00033)),
    PopulationSampleFrequency("D21S11", 26, BigDecimal(0.00158)),
    PopulationSampleFrequency("D21S11", 27, BigDecimal(0.01638)),
    PopulationSampleFrequency("D21S11", 28, BigDecimal(0.09375)),
    PopulationSampleFrequency("D21S11", 29, BigDecimal(0.19643)),
    PopulationSampleFrequency("D21S11", 30, BigDecimal(0.27228)),
    PopulationSampleFrequency("D21S11", 30.2, BigDecimal(0.02536)),
    PopulationSampleFrequency("D21S11", 31, BigDecimal(0.06209)),
    PopulationSampleFrequency("D21S11", 31.2, BigDecimal(0.11312)),
    PopulationSampleFrequency("D21S11", 32, BigDecimal(0.0092)),
    PopulationSampleFrequency("D21S11", 32.2, BigDecimal(0.13843)),
    PopulationSampleFrequency("D21S11", 33.2, BigDecimal(0.0543)),
    PopulationSampleFrequency("D21S11", 34.2, BigDecimal(0.00637)),
    PopulationSampleFrequency("D21S11", 35, BigDecimal(0.00103)),
    PopulationSampleFrequency("D21S11", 35.2, BigDecimal(0.00087))
  )

  val populationBaseFrequency = PopulationBaseFrequency(
    "A", 0.0, ProbabilityModel.HardyWeinberg, seqPopulation
  )

  val strKits = Seq(
    StrKit("Identifiler", "Identifiler", 1, 16, 14),
    StrKit("Minifiler", "Minifiler", 1, 8, 8)
  )

  val fullCat = FullCategory(
    id = AlphanumericId("SOSPECHOSO"),
    name = "Sospechoso",
    description = None,
    group = AlphanumericId("INDUBITADA"),
    isReference = true,
    filiationDataRequired = false,
    replicate = true,
    configurations = Map(1 -> CategoryConfiguration("", "", "K", "2", 2)),
    associations = Seq.empty,
    aliases = Seq.empty,
    matchingRules = Seq.empty,
    tipo = Some(1)
  )

  val baseFrequencyView = PopulationBaseFrequencyNameView(
    name = "pop freq 1",
    theta = 0.0,
    model = "HardyWeinberg",
    state = true,
    default = true
  )

  val laboratory = Laboratory(
    name = "test lab",
    code = "NME",
    country = "AR",
    province = "C",
    address = "address 123",
    telephone = "54111",
    contactEmail = "lab@example.com",
    dropIn = 0.6,
    dropOut = 0.4,
    instance = Some(false)
  )

  private def buildService(
    pbfService: PopulationBaseFrequencyService = mock[PopulationBaseFrequencyService],
    pdRepo: ProfileDataRepository = mock[ProfileDataRepository],
    labService: LaboratoryService = mock[LaboratoryService],
    kitService: StrKitService = mock[StrKitService],
    paramsProvider: QualityParamsProvider = mock[QualityParamsProvider],
    catService: CategoryService = mock[CategoryService]
  ) = new ProbabilityServiceImpl(pbfService, pdRepo, labService, kitService, paramsProvider, catService)

  // ── calculateContributors ─────────────────────────────────────────

  "ProbabilityServiceImpl.calculateContributors" must {

    "detect 2 contributors" in {
      val pbfService  = mock[PopulationBaseFrequencyService]
      when(pbfService.getByName("A")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val kitService = mock[StrKitService]
      when(kitService.list()).thenReturn(Future.successful(strKits))

      val catService = mock[CategoryService]
      when(catService.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(fullCat)))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      val genotypification = Map(
        "CSF1PO"  -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51"  -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11"  -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818"  -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520"  -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA"     -> List(Allele(19), Allele(20), Allele(23), Allele(24)),
        "TH01"    -> List(Allele(6), Allele(9.3)),
        "TPOX"    -> List(Allele(12), Allele(11)),
        "vWA"     -> List(Allele(18), Allele(17), Allele(16))
      )

      val target = buildService(pbfService = pbfService, kitService = kitService,
        catService = catService, paramsProvider = paramsProvider)

      val result = Await.result(
        target.calculateContributors(
          Analysis("", MongoDate(new Date()), "Identifiler", genotypification, None),
          AlphanumericId("SOSPECHOSO"),
          StatOption("A", "1", 0, 0.4, Some(0.5))
        ),
        timeout
      )

      result mustBe 2
    }

    "detect 4 contributors" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getByName("A")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val kitService = mock[StrKitService]
      when(kitService.list()).thenReturn(Future.successful(strKits))

      val catService = mock[CategoryService]
      when(catService.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(fullCat)))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      val genotypification = Map(
        "CSF1PO"  -> List(Allele(13), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(9), Allele(12), Allele(8), Allele(13), Allele(11)),
        "D16S539" -> List(Allele(11), Allele(12), Allele(10), Allele(9)),
        "D18S51"  -> List(Allele(15), Allele(14), Allele(16), Allele(13), Allele(17)),
        "D21S11"  -> List(Allele(32.2), Allele(31), Allele(28), Allele(30), Allele(31.2)),
        "D3S1358" -> List(Allele(14), Allele(16), Allele(17), Allele(15)),
        "D5S818"  -> List(Allele(12), Allele(7), Allele(9), Allele(13), Allele(11), Allele(10)),
        "D7S520"  -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(11), Allele(14), Allele(10), Allele(8)),
        "FGA"     -> List(Allele(22), Allele(26), Allele(19), Allele(24), Allele(21), Allele(25)),
        "TH01"    -> List(Allele(7), Allele(9.3), Allele(9), Allele(6)),
        "TPOX"    -> List(Allele(12), Allele(11), Allele(8)),
        "vWA"     -> List(Allele(18), Allele(17), Allele(16), Allele(19))
      )

      val target = buildService(pbfService = pbfService, kitService = kitService,
        catService = catService, paramsProvider = paramsProvider)

      val result = Await.result(
        target.calculateContributors(
          Analysis("", MongoDate(new Date()), "Identifiler", genotypification, None),
          AlphanumericId("SOSPECHOSO"),
          StatOption("A", "1", 0, 0.4, Some(0.5))
        ),
        timeout
      )

      result mustBe 4
    }

    "count 3 contributors when there is a trisomy locus but no trisomy tolerance configured" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getByName("A")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val kitService = mock[StrKitService]
      when(kitService.list()).thenReturn(Future.successful(strKits))

      val catService = mock[CategoryService]
      when(catService.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(fullCat)))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      // FGA has 5 alleles — exceeds 2*2=4 allowed for 2 contributors
      val genotypification = Map(
        "CSF1PO"  -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51"  -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11"  -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818"  -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520"  -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA"     -> List(Allele(19), Allele(20), Allele(23), Allele(24), Allele(22)),
        "TH01"    -> List(Allele(6), Allele(9.3)),
        "TPOX"    -> List(Allele(12), Allele(11)),
        "vWA"     -> List(Allele(18), Allele(17), Allele(16))
      )

      val target = buildService(pbfService = pbfService, kitService = kitService,
        catService = catService, paramsProvider = paramsProvider)

      val result = Await.result(
        target.calculateContributors(
          Analysis("", MongoDate(new Date()), "Identifiler", genotypification, None),
          AlphanumericId("SOSPECHOSO"),
          StatOption("A", "1", 0, 0.4, Some(0.5))
        ),
        timeout
      )

      result mustBe 3
    }

    "count 2 contributors when there is a trisomy locus with 1 trisomy tolerance configured" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getByName("A")).thenReturn(Future.successful(Some(populationBaseFrequency)))

      val kitService = mock[StrKitService]
      when(kitService.list()).thenReturn(Future.successful(strKits))

      val catService = mock[CategoryService]
      when(catService.getCategory(any[AlphanumericId])).thenReturn(Future.successful(Some(fullCat)))

      val paramsProvider = mock[QualityParamsProvider]
      // 1 overage allowed → treats FGA trisomy as acceptable for 2 contributors
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(1)

      val genotypification = Map(
        "CSF1PO"  -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51"  -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11"  -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818"  -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520"  -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA"     -> List(Allele(19), Allele(20), Allele(23), Allele(24), Allele(22)),
        "TH01"    -> List(Allele(6), Allele(9.3)),
        "TPOX"    -> List(Allele(12), Allele(11)),
        "vWA"     -> List(Allele(18), Allele(17), Allele(16))
      )

      val target = buildService(pbfService = pbfService, kitService = kitService,
        catService = catService, paramsProvider = paramsProvider)

      val result = Await.result(
        target.calculateContributors(
          Analysis("", MongoDate(new Date()), "Identifiler", genotypification, None),
          AlphanumericId("SOSPECHOSO"),
          StatOption("A", "1", 0, 0.4, Some(0.5))
        ),
        timeout
      )

      result mustBe 2
    }

    "return 1 for Mitocondrial kit regardless of genotypification" in {
      val target = buildService()

      val result = Await.result(
        target.calculateContributors(
          Analysis("", MongoDate(new Date()), "Mitocondrial", Map.empty, None),
          AlphanumericId("SOSPECHOSO"),
          StatOption("A", "1", 0, 0.4, Some(0.5))
        ),
        timeout
      )

      result mustBe 1
    }
  }

  // ── getStats ──────────────────────────────────────────────────────

  "ProbabilityServiceImpl.getStats" must {

    "return StatOption with values from default population base frequency and laboratory" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getDefault()).thenReturn(Future.successful(Some(baseFrequencyView)))

      val labService = mock[LaboratoryService]
      when(labService.get("NME")).thenReturn(Future.successful(Some(laboratory)))

      val target = buildService(pbfService = pbfService, labService = labService)

      val result = Await.result(target.getStats("NME"), timeout)

      result must not be None
      result.get.frequencyTable    mustBe baseFrequencyView.name
      result.get.probabilityModel  mustBe baseFrequencyView.model
      result.get.theta             mustBe baseFrequencyView.theta
      result.get.dropIn            mustBe laboratory.dropIn
      result.get.dropOut           mustBe Some(laboratory.dropOut)
    }

    "return None when the laboratory does not exist" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getDefault()).thenReturn(Future.successful(Some(baseFrequencyView)))

      val labService = mock[LaboratoryService]
      when(labService.get("UNKNOWN")).thenReturn(Future.successful(None))

      val target = buildService(pbfService = pbfService, labService = labService)

      val result = Await.result(target.getStats("UNKNOWN"), timeout)

      result mustBe None
    }

    "return None when there is no default population base frequency" in {
      val pbfService = mock[PopulationBaseFrequencyService]
      when(pbfService.getDefault()).thenReturn(Future.successful(None))

      val labService = mock[LaboratoryService]
      when(labService.get(any[String])).thenReturn(Future.successful(Some(laboratory)))

      val target = buildService(pbfService = pbfService, labService = labService)

      val result = Await.result(target.getStats("NME"), timeout)

      result mustBe None
    }
  }
}
