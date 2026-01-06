package matching

import configdata.{Category, CategoryConfiguration, CategoryService, FullCategory}
import controllers.Matching
import laboratories.LaboratoryService
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.scalatest.mock.MockitoSugar
import org.specs2.mutable.Specification
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import probability.{CalculationTypeService, LRResult, ProbabilityModel}
import profile.Profile.LabeledGenotypification
import profile.{Profile, ProfileService}
import profiledata.ProfileDataService
import scenarios.{CalculationScenario, Hypothesis, ScenarioService}
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}
import stubs.Stubs
import types.{AlphanumericId, SampleCode, StatOption}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import profile.GenotypificationByType._
import stubs.Stubs.{analyses, genotypification}

class MatchingCalculatorServiceTest extends Specification with MockitoSugar with Results {
  val reference = Category(AlphanumericId("REF"), AlphanumericId("GRUPO_A"), "Subcategory A1", true, Option("description of subcategory A1"))
  val nonReference = Category(AlphanumericId("NONREF"), AlphanumericId("GRUPO_A"), "Subcategory A2", false, Option("description of subcategory A2"))
  val referenceFull = FullCategory(reference.id, reference.name, reference.description, reference.group, true, true, true, false,manualLoading=true,
    Map(1 -> CategoryConfiguration("", "", "K", "2", 2)), Seq.empty, Seq.empty, Seq.empty,Some(1))
  val nonReferenceFull = FullCategory(nonReference.id, nonReference.name, nonReference.description, nonReference.group, false, true, true, false,manualLoading=true,
    Map(1 -> CategoryConfiguration("", "", "K", "0", 4)), Seq.empty, Seq.empty, Seq.empty,Some(1))
  val profileRef = Profile(SampleCode("AR-B-IMBICE-500"), SampleCode("AR-B-IMBICE-500"), "", "", reference.id, genotypification, analyses, None, None, None, None, None, false, true, false)
  val profileNonRef = Profile(SampleCode("AR-B-IMBICE-500"), SampleCode("AR-B-IMBICE-500"), "", "", nonReference.id, genotypification, analyses, None, None, None, None, None, false, true, false)

  val categoryService = mock[CategoryService]
  when(categoryService.listCategories).thenReturn(Map(referenceFull.id -> referenceFull ,nonReferenceFull.id -> nonReferenceFull))

  val seqPopulation: Seq[PopulationSampleFrequency] = List(
    PopulationSampleFrequency("TPOX", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D3S1358", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("FGA", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D5S818", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("CSF1PO", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D7S520", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D8S1179", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("TH01", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("vWA", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D13S317", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("Penta E", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D16S539", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D18S51", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("PentaD", -1, BigDecimal("0.0000000001")),
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
    PopulationSampleFrequency("D21S11", -1, BigDecimal("0.0000000001")),
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

  val baseFrequency = new PopulationBaseFrequency("pop freq 1", 0, ProbabilityModel.HardyWeinberg, seqPopulation)
  val duration = Duration(100, SECONDS)

  "get lr by algorithm" should {
    "use lr-mix if both contributors = 1" in {
      val mockResult = mock[LRResult]

      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.getLRMix(any[CalculationScenario],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(Some(mockResult)))

      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, mockScenarioService, null,null,null,false,null,categoryService)

      // generate profiles with 1 contributor
      val sample = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, reference.id, Map(), None, None, Some(1), None)
      val profile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, reference.id, Map(), None, None, Some(1), None)
      val stats = StatOption("pop freq 1", "HardyWeinberg", 0.0, 0.2, Some(0.1))
      val resultFut = calculator.doGetLRByAlgorithm(sample, profile, stats)

      val lr = Await.result(resultFut, duration)

      lr must beSome
      lr.get must beEqualTo(mockResult)
      val mr = ArgumentCaptor.forClass(classOf[Option[NewMatchingResult.AlleleMatchRange]])

      val captor = ArgumentCaptor.forClass(classOf[CalculationScenario])
      verify(mockScenarioService).getLRMix(captor.capture(),mr.capture())
      val scenarioGenerated = captor.getValue()

      scenarioGenerated must beEqualTo(CalculationScenario(sample.globalCode, Hypothesis(List(profile.globalCode), List(), 0, 0.1), Hypothesis(List(), List(profile.globalCode), 1, 0.1), stats,Some(List(sample,profile))))
    }
    "determineProfiles reference vs non reference" in {
        val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null,null,null,false,null,categoryService)

      calculator.determineProfiles(profileRef,profileNonRef) must beEqualTo (profileRef,profileNonRef,false)
    }
    "determineProfiles reference vs non reference reverse" in {
      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null,null,null,false,null,categoryService)

      calculator.determineProfiles(profileNonRef,profileRef) must beEqualTo (profileRef,profileNonRef,false)
    }
    "determineProfiles reference vs non reference 2 refs" in {
      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null,null,null,false,null,categoryService)

      calculator.determineProfiles(profileRef,profileRef) must beEqualTo (profileRef,profileRef,true)
    }
    "determineProfiles reference vs non reference 2 non refs" in {
      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null,null,null,false,null,categoryService)

      calculator.determineProfiles(profileNonRef,profileNonRef) must beEqualTo (profileNonRef,profileNonRef,true)
    }
    "avglr" in {
      val a: Option[LRResult] = Some(LRResult(4.0, Map("A"->Some(2.0), "B"->Some(10.0), "C"->Some(20.0))))
      val b: Option[LRResult] = Some(LRResult(6.0, Map("A"->Some(5.0), "B"->None, "C"->Some(10.0))))
      val calculator = new MatchingCalculatorServiceImpl(
        matchingService = null,
        profileService = null,
        populationBaseFrequencyService = null,
        profileDataService = null,
        laboratoryService = null,
        scenarioService = null,
        calculationTypeService = null,
        probabilityService = null,
        currentInstanceLabCode = null,
        updateLr = false,
        locusService = null,
        categoryService = categoryService
      )
      val actual: Option[LRResult] = calculator.avgLr(a, b)
      val expected: Option[LRResult] = Some(LRResult(5.0, Map("A"->Some(3.5), "B"->Some(5.0), "C"->Some(15.0))))
      actual mustEqual expected
    }
    "avglr when a profile has total lr of 0.0" in {
      val a: Option[LRResult] = Some(
        LRResult(
          4.0,
          Map("A" -> Some(2.0), "B" -> Some(10.0), "C" -> Some(20.0))
        )
      )
      val b: Option[LRResult] = Some(
        LRResult(
          0.0,
          Map("A" -> Some(5.0), "B" -> Some(5.0), "C" -> Some(10.0))
        )
      )
      val calculator = new MatchingCalculatorServiceImpl(
        matchingService = null,
        profileService = null,
        populationBaseFrequencyService = null,
        profileDataService = null,
        laboratoryService = null,
        scenarioService = null,
        calculationTypeService = null,
        probabilityService = null,
        currentInstanceLabCode = null,
        updateLr = false,
        locusService = null,
        categoryService = categoryService
      )
      val actual: Option[LRResult] = calculator.avgLr(a, b)
      actual mustEqual a
    }
    /*   "use mix-mix if both contributors = 2" in {
      val mockPopulationService = mock[PopulationBaseFrequencyService]
      when(mockPopulationService.getByName(any[String])).thenReturn(Future.successful(Some(baseFrequency)))

      // generate profiles with 2 contributors
      val profile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Stubs.mixtureGenotypification, None, None, Some(2), None)
      val stats = StatOption("pop freq 1", "HardyWeinberg", 0.0, 0.2, Some(0.1))

      val mockProfileService = mock[ProfileService]
      when(mockProfileService.getAssociatedProfiles(any[Profile])).thenReturn(Future.successful(Nil))
      when(mockProfileService.findByCodes(any[List[SampleCode]])).thenReturn(Future.successful(List(profile, profile)))

      val mockCalculationTypeService = mock[CalculationTypeService]
      when(mockCalculationTypeService.filterCodes(any[List[SampleCode]], any[String],any[List[Profile]])).thenReturn(Future.successful(List(profile.genotypification(1), profile.genotypification(1))))
      when(mockCalculationTypeService.filterProfiles(any[List[Profile]], any[String])).thenReturn(Future.successful(List(profile.genotypification(1), profile.genotypification(1))))
      when(mockCalculationTypeService.getAnalysisTypeByCalculation(any[String])).thenReturn(Future.successful(Stubs.analysisTypes.head))

      val calculator = new MatchingCalculatorServiceImpl(null, mockProfileService, mockPopulationService, null, null, null, mockCalculationTypeService)

      val resultFut = calculator.doGetLRByAlgorithm(profile, profile, stats)
      val lr = Await.result(resultFut, duration)

      lr must beSome
      lr.get.total must beGreaterThan(0.0)
      lr.get.detailed.isEmpty must beFalse
    }*/

    "use lr-mix if q(n=1) and p(n>1)" in {
      val mockResult = mock[LRResult]

      val mockScenarioService = mock[ScenarioService]
      when(mockScenarioService.getLRMix(any[CalculationScenario],any[Option[NewMatchingResult.AlleleMatchRange]])).thenReturn(Future.successful(Some(mockResult)))

      val profileService = mock[ProfileService]
      when(profileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val calculator = new MatchingCalculatorServiceImpl(null, profileService, null, null, null, mockScenarioService, null)

      // generate a profile with 1 contributor and another one with >1 contributors
      val sample = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      val profile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(2), None)
      val stats = StatOption("pop freq 1", "HardyWeinberg", 0.0, 0.2, Some(0.1))
      val resultFut = calculator.doGetLRByAlgorithm(sample, profile, stats)

      val lr = Await.result(resultFut, duration)

      lr must beSome
      lr.get must beEqualTo(mockResult)

      val captor = ArgumentCaptor.forClass(classOf[CalculationScenario])
      val mr = ArgumentCaptor.forClass(classOf[Option[NewMatchingResult.AlleleMatchRange]])

      verify(mockScenarioService).getLRMix(captor.capture(),mr.capture())
      val scenarioGenerated = captor.getValue()

      scenarioGenerated must beEqualTo(CalculationScenario(profile.globalCode, Hypothesis(List(sample.globalCode), List(), 1, 0.1), Hypothesis(List(), List(sample.globalCode), 2, 0.1), stats,Some(List(sample,profile))))
    }

   /* "use no algorithm if both contributors > 1" in {
      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null)

      // generate profiles with >2 contributors
      val sample = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(3), None)
      val profile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(2), None)
      val stats = StatOption("pop freq 1", "HardyWeinberg", 0.0, 0.2, Some(0.1))
      val resultFut = calculator.doGetLRByAlgorithm(sample, profile, stats)

      val lr = Await.result(resultFut, duration)

      lr must beNone
    }*/

    "create default scenario with 1 and 1 contributors" in {
      val stats = Stubs.statOption
      val firingProfile = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, reference.id, Map(), None, None, Some(1), None)
      val matchingProfile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, reference.id, Map(), None, None, Some(1), None)

      val calculator = new MatchingCalculatorServiceImpl(null, null, null, null, null, null, null,null,null,false,null,categoryService)

      val scenario = calculator.createDefaultScenario(firingProfile, matchingProfile, stats)

      scenario.sample must beEqualTo(firingProfile.globalCode)
      scenario.prosecutor.selected.head must beEqualTo(matchingProfile.globalCode)
      scenario.prosecutor.unknowns must beEqualTo(firingProfile.contributors.get - 1)
      scenario.defense.unselected.head must beEqualTo(matchingProfile.globalCode)
      scenario.defense.unknowns must beEqualTo(firingProfile.contributors.get)
      scenario.stats must beEqualTo(stats)
    }

    "create default scenario with 1 and 2 contributors" in {
      val stats = Stubs.statOption
      val firingProfile = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(1), None)
      val matchingProfile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(2), None)

      val profileService = mock[ProfileService]
      when(profileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val calculator = new MatchingCalculatorServiceImpl(null, profileService, null, null, null, null, null)

      val scenario = calculator.createDefaultScenario(firingProfile, matchingProfile, stats)

      scenario.sample must beEqualTo(matchingProfile.globalCode)
      scenario.prosecutor.selected.head must beEqualTo(firingProfile.globalCode)
      scenario.prosecutor.unknowns must beEqualTo(matchingProfile.contributors.get - 1)
      scenario.defense.unselected.head must beEqualTo(firingProfile.globalCode)
      scenario.defense.unknowns must beEqualTo(matchingProfile.contributors.get)
      scenario.stats must beEqualTo(stats)
    }

    "create default scenario with 2 and 1 contributors" in {
      val stats = Stubs.statOption
      val firingProfile = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(2), None)
      val matchingProfile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(1), None)

      val profileService = mock[ProfileService]
      when(profileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Nil)

      val calculator = new MatchingCalculatorServiceImpl(null, profileService, null, null, null, null, null)

      val scenario = calculator.createDefaultScenario(firingProfile, matchingProfile, stats)

      scenario.sample must beEqualTo(firingProfile.globalCode)
      scenario.prosecutor.selected.head must beEqualTo(matchingProfile.globalCode)
      scenario.prosecutor.unknowns must beEqualTo(firingProfile.contributors.get - 1)
      scenario.defense.unselected.head must beEqualTo(matchingProfile.globalCode)
      scenario.defense.unknowns must beEqualTo(firingProfile.contributors.get)
      scenario.stats must beEqualTo(stats)
    }

    "create default scenario with 2 and 1 contributors and profile associated" in {
      val stats = Stubs.statOption
      val firingProfile = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, null, Map(), None, None, Some(2), None)
      val matchingProfile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, null, Map(), None, None, Some(1), None)

      val associated = Stubs.sampleCode
      val profileService = mock[ProfileService]
      when(profileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq(associated.text))

      val calculator = new MatchingCalculatorServiceImpl(null, profileService, null, null, null, null, null)

      val scenario = calculator.createDefaultScenario(firingProfile, matchingProfile, stats)

      scenario.sample must beEqualTo(firingProfile.globalCode)
      scenario.prosecutor.selected must beEqualTo(Seq(associated, matchingProfile.globalCode))
      scenario.prosecutor.unknowns must beEqualTo(firingProfile.contributors.get - 2)
      scenario.defense.selected must beEqualTo(Seq(associated))
      scenario.defense.unselected must beEqualTo(Seq(matchingProfile.globalCode))
      scenario.defense.unknowns must beEqualTo(firingProfile.contributors.get - 1)
      scenario.stats must beEqualTo(stats)
    }

    "create default scenario with 1 and 1 contributors and profile associated" in {
      val stats = Stubs.statOption
      val firingProfile = Profile(null, SampleCode("AR-B-IMBICE-400"), null, null, reference.id, Map(), None, None, Some(1), None)
      val matchingProfile = Profile(null, SampleCode("AR-B-IMBICE-500"), null, null, reference.id, Map(), None, None, Some(1), None)

      val associated = Stubs.sampleCode
      val profileService = mock[ProfileService]
      when(profileService.validProfilesAssociated(any[Option[LabeledGenotypification]])).thenReturn(Seq(associated.text))

      val calculator = new MatchingCalculatorServiceImpl(null, profileService, null, null, null, null, null,null,null,false,null,categoryService)

      val scenario = calculator.createDefaultScenario(firingProfile, matchingProfile, stats)

      scenario.sample must beEqualTo(firingProfile.globalCode)
      scenario.prosecutor.selected.head must beEqualTo(matchingProfile.globalCode)
      scenario.prosecutor.unknowns must beEqualTo(firingProfile.contributors.get - 1)
      scenario.defense.unselected.head must beEqualTo(matchingProfile.globalCode)
      scenario.defense.unknowns must beEqualTo(firingProfile.contributors.get)
      scenario.stats must beEqualTo(stats)
    }

  }

}
