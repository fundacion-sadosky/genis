package probability

import configdata._
import kits.{StrKit, StrKitService}
import laboratories.LaboratoryService

import scala.concurrent.ExecutionContext.Implicits.global
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import profile.{Allele, Analysis}
import profiledata.ProfileDataService
import specs.PdgSpec
import stats.{PopulationBaseFrequency, PopulationBaseFrequencyService, PopulationSampleFrequency}
import stubs.Stubs
import types.{AlphanumericId, SampleCode, StatOption}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class ProbabilityServiceTest extends PdgSpec with MockitoSugar {
  val duration = Duration(10, SECONDS)

  val seqPopulation = List(
    PopulationSampleFrequency("TPOX",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX",5,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX",6,BigDecimal(0.00261)),
    PopulationSampleFrequency("TPOX",7,BigDecimal(0.00142)),
    PopulationSampleFrequency("TPOX",8,BigDecimal(0.48349)),
    PopulationSampleFrequency("TPOX",9,BigDecimal(0.0779)),
    PopulationSampleFrequency("TPOX",10,BigDecimal(0.04761)),
    PopulationSampleFrequency("TPOX",11,BigDecimal(0.28963)),
    PopulationSampleFrequency("TPOX",12,BigDecimal(0.09435)),
    PopulationSampleFrequency("TPOX",13,BigDecimal(0.00256)),
    PopulationSampleFrequency("TPOX",14,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX",16,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX",21,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TPOX",21.2,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D3S1358",10,BigDecimal(0.000273373428102788)),
    PopulationSampleFrequency("D3S1358",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D3S1358",11,BigDecimal(0.00055)),
    PopulationSampleFrequency("D3S1358",12,BigDecimal(0.00186)),
    PopulationSampleFrequency("D3S1358",13,BigDecimal(0.00399)),
    PopulationSampleFrequency("D3S1358",14,BigDecimal(0.07485)),
    PopulationSampleFrequency("D3S1358",15,BigDecimal(0.35112)),
    PopulationSampleFrequency("D3S1358",16,BigDecimal(0.27983)),
    PopulationSampleFrequency("D3S1358",17,BigDecimal(0.16096)),
    PopulationSampleFrequency("D3S1358",18,BigDecimal(0.11706)),
    PopulationSampleFrequency("D3S1358",19,BigDecimal(0.00891)),
    PopulationSampleFrequency("D3S1358",20,BigDecimal(0.00071)),
    PopulationSampleFrequency("D3S1358",21,BigDecimal(0.000273373428102788)),
    PopulationSampleFrequency("FGA",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("FGA",16,BigDecimal(0.00038)),
    PopulationSampleFrequency("FGA",17,BigDecimal(0.00223)),
    PopulationSampleFrequency("FGA",18,BigDecimal(0.01084)),
    PopulationSampleFrequency("FGA",18.2,BigDecimal(0.00038)),
    PopulationSampleFrequency("FGA",19,BigDecimal(0.08764)),
    PopulationSampleFrequency("FGA",19.2,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",20,BigDecimal(0.09248)),
    PopulationSampleFrequency("FGA",20.2,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",21,BigDecimal(0.14417)),
    PopulationSampleFrequency("FGA",21.2,BigDecimal(0.00202)),
    PopulationSampleFrequency("FGA",22,BigDecimal(0.12609)),
    PopulationSampleFrequency("FGA",22.2,BigDecimal(0.00398)),
    PopulationSampleFrequency("FGA",22.3,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",23,BigDecimal(0.1213)),
    PopulationSampleFrequency("FGA",23.2,BigDecimal(0.00245)),
    PopulationSampleFrequency("FGA",24,BigDecimal(0.15153)),
    PopulationSampleFrequency("FGA",24.2,BigDecimal(0.00071)),
    PopulationSampleFrequency("FGA",25,BigDecimal(0.15044)),
    PopulationSampleFrequency("FGA",25.2,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",25.3,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",26,BigDecimal(0.07375)),
    PopulationSampleFrequency("FGA",26.2,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",27,BigDecimal(0.022)),
    PopulationSampleFrequency("FGA",28,BigDecimal(0.00599)),
    PopulationSampleFrequency("FGA",29,BigDecimal(0.00054)),
    PopulationSampleFrequency("FGA",30,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("FGA",31,BigDecimal(0.000272331154684096)),
    PopulationSampleFrequency("D5S818",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D5S818",7,BigDecimal(0.0693)),
    PopulationSampleFrequency("D5S818",8,BigDecimal(0.00635)),
    PopulationSampleFrequency("D5S818",9,BigDecimal(0.0421)),
    PopulationSampleFrequency("D5S818",10,BigDecimal(0.05326)),
    PopulationSampleFrequency("D5S818",11,BigDecimal(0.42216)),
    PopulationSampleFrequency("D5S818",12,BigDecimal(0.27108)),
    PopulationSampleFrequency("D5S818",13,BigDecimal(0.12503)),
    PopulationSampleFrequency("D5S818",14,BigDecimal(0.00843)),
    PopulationSampleFrequency("D5S818",15,BigDecimal(0.00208)),
    PopulationSampleFrequency("D5S818",16,BigDecimal(0.000273702649441647)),
    PopulationSampleFrequency("CSF1PO",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("CSF1PO",0,BigDecimal(0.000287455444406117)),
    PopulationSampleFrequency("CSF1PO",6,BigDecimal(0.000287455444406117)),
    PopulationSampleFrequency("CSF1PO",7,BigDecimal(0.00167)),
    PopulationSampleFrequency("CSF1PO",8,BigDecimal(0.00448)),
    PopulationSampleFrequency("CSF1PO",8.3,BigDecimal(0.000287455444406117)),
    PopulationSampleFrequency("CSF1PO",9,BigDecimal(0.02185)),
    PopulationSampleFrequency("CSF1PO",10,BigDecimal(0.26929)),
    PopulationSampleFrequency("CSF1PO",10.3,BigDecimal(0.000287455444406117)),
    PopulationSampleFrequency("CSF1PO",11,BigDecimal(0.28222)),
    PopulationSampleFrequency("CSF1PO",12,BigDecimal(0.34753)),
    PopulationSampleFrequency("CSF1PO",13,BigDecimal(0.06209)),
    PopulationSampleFrequency("CSF1PO",14,BigDecimal(0.00868)),
    PopulationSampleFrequency("CSF1PO",15,BigDecimal(0.00167)),
    PopulationSampleFrequency("D7S520",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D7S520",6,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520",7,BigDecimal(0.01408)),
    PopulationSampleFrequency("D7S520",8,BigDecimal(0.10337)),
    PopulationSampleFrequency("D7S520",9,BigDecimal(0.0882)),
    PopulationSampleFrequency("D7S520",9.1,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520",10,BigDecimal(0.26243)),
    PopulationSampleFrequency("D7S520",11,BigDecimal(0.30968)),
    PopulationSampleFrequency("D7S520",12,BigDecimal(0.1863)),
    PopulationSampleFrequency("D7S520",12.1,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520",12.2,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520",12.3,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D7S520",13,BigDecimal(0.03094)),
    PopulationSampleFrequency("D7S520",14,BigDecimal(0.00462)),
    PopulationSampleFrequency("D7S520",15,BigDecimal(0.000271886895051658)),
    PopulationSampleFrequency("D8S1179",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D8S1179",8,BigDecimal(0.00898)),
    PopulationSampleFrequency("D8S1179",9,BigDecimal(0.00762)),
    PopulationSampleFrequency("D8S1179",10,BigDecimal(0.06872)),
    PopulationSampleFrequency("D8S1179",11,BigDecimal(0.07247)),
    PopulationSampleFrequency("D8S1179",12,BigDecimal(0.14353)),
    PopulationSampleFrequency("D8S1179",13,BigDecimal(0.30446)),
    PopulationSampleFrequency("D8S1179",14,BigDecimal(0.22236)),
    PopulationSampleFrequency("D8S1179",15,BigDecimal(0.13732)),
    PopulationSampleFrequency("D8S1179",16,BigDecimal(0.03047)),
    PopulationSampleFrequency("D8S1179",17,BigDecimal(0.00337)),
    PopulationSampleFrequency("D8S1179",18,BigDecimal(0.00065)),
    PopulationSampleFrequency("D8S1179",19,BigDecimal(0.000272034820457019)),
    PopulationSampleFrequency("TH01",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("TH01",4,BigDecimal(0.000290528762347472)),
    PopulationSampleFrequency("TH01",5,BigDecimal(0.00064)),
    PopulationSampleFrequency("TH01",6,BigDecimal(0.29564)),
    PopulationSampleFrequency("TH01",7,BigDecimal(0.26031)),
    PopulationSampleFrequency("TH01",8,BigDecimal(0.08001)),
    PopulationSampleFrequency("TH01",9,BigDecimal(0.12342)),
    PopulationSampleFrequency("TH01",9.3,BigDecimal(0.23091)),
    PopulationSampleFrequency("TH01",10,BigDecimal(0.00808)),
    PopulationSampleFrequency("TH01",11,BigDecimal(0.0007)),
    PopulationSampleFrequency("TH01",13.3,BigDecimal(0.000290528762347472)),
    PopulationSampleFrequency("vWA",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("vWA",11,BigDecimal(0.00087)),
    PopulationSampleFrequency("vWA",12,BigDecimal(0.00082)),
    PopulationSampleFrequency("vWA",13,BigDecimal(0.00414)),
    PopulationSampleFrequency("vWA",14,BigDecimal(0.06419)),
    PopulationSampleFrequency("vWA",15,BigDecimal(0.0943)),
    PopulationSampleFrequency("vWA",16,BigDecimal(0.30923)),
    PopulationSampleFrequency("vWA",17,BigDecimal(0.28278)),
    PopulationSampleFrequency("vWA",18,BigDecimal(0.16705)),
    PopulationSampleFrequency("vWA",19,BigDecimal(0.06425)),
    PopulationSampleFrequency("vWA",20,BigDecimal(0.01123)),
    PopulationSampleFrequency("vWA",21,BigDecimal(0.00093)),
    PopulationSampleFrequency("vWA",22,BigDecimal(0.000272687609075044)),
    PopulationSampleFrequency("vWA",23,BigDecimal(0.000272687609075044)),
    PopulationSampleFrequency("D13S317",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D13S317",6,BigDecimal(0.00054)),
    PopulationSampleFrequency("D13S317",7,BigDecimal(0.000271827769924976)),
    PopulationSampleFrequency("D13S317",8,BigDecimal(0.09117)),
    PopulationSampleFrequency("D13S317",9,BigDecimal(0.15994)),
    PopulationSampleFrequency("D13S317",10,BigDecimal(0.07562)),
    PopulationSampleFrequency("D13S317",11,BigDecimal(0.22469)),
    PopulationSampleFrequency("D13S317",12,BigDecimal(0.24182)),
    PopulationSampleFrequency("D13S317",13,BigDecimal(0.12787)),
    PopulationSampleFrequency("D13S317",14,BigDecimal(0.07606)),
    PopulationSampleFrequency("D13S317",15,BigDecimal(0.00196)),
    PopulationSampleFrequency("D13S317",16,BigDecimal(0.000271827769924976)),
    PopulationSampleFrequency("PentaE",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("PentaE",5,BigDecimal(0.03706)),
    PopulationSampleFrequency("PentaE",6,BigDecimal(0.00051)),
    PopulationSampleFrequency("PentaE",7,BigDecimal(0.09435)),
    PopulationSampleFrequency("PentaE",8,BigDecimal(0.02785)),
    PopulationSampleFrequency("PentaE",9,BigDecimal(0.00695)),
    PopulationSampleFrequency("PentaE",10,BigDecimal(0.06017)),
    PopulationSampleFrequency("PentaE",11,BigDecimal(0.0878)),
    PopulationSampleFrequency("PentaE",12,BigDecimal(0.1822)),
    PopulationSampleFrequency("PentaE",13,BigDecimal(0.09463)),
    PopulationSampleFrequency("PentaE",13.2,BigDecimal(0.000282485875706215)),
    PopulationSampleFrequency("PentaE",14,BigDecimal(0.07299)),
    PopulationSampleFrequency("PentaE",15,BigDecimal(0.09814)),
    PopulationSampleFrequency("PentaE",16,BigDecimal(0.05927)),
    PopulationSampleFrequency("PentaE",16.3,BigDecimal(0.0009)),
    PopulationSampleFrequency("PentaE",16.4,BigDecimal(0.000282485875706215)),
    PopulationSampleFrequency("PentaE",17,BigDecimal(0.05322)),
    PopulationSampleFrequency("PentaE",18,BigDecimal(0.03859)),
    PopulationSampleFrequency("PentaE",19,BigDecimal(0.02616)),
    PopulationSampleFrequency("PentaE",20,BigDecimal(0.02537)),
    PopulationSampleFrequency("PentaE",21,BigDecimal(0.02107)),
    PopulationSampleFrequency("PentaE",22,BigDecimal(0.00757)),
    PopulationSampleFrequency("PentaE",23,BigDecimal(0.00345)),
    PopulationSampleFrequency("PentaE",24,BigDecimal(0.00113)),
    PopulationSampleFrequency("PentaE",25,BigDecimal(0.000282485875706215)),
    PopulationSampleFrequency("PentaE",26,BigDecimal(0.000282485875706215)),
    PopulationSampleFrequency("D16S539",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D16S539",5,BigDecimal(0.000273822562979189)),
    PopulationSampleFrequency("D16S539",7,BigDecimal(0.000273822562979189)),
    PopulationSampleFrequency("D16S539",8,BigDecimal(0.01758)),
    PopulationSampleFrequency("D16S539",9,BigDecimal(0.16177)),
    PopulationSampleFrequency("D16S539",10,BigDecimal(0.10915)),
    PopulationSampleFrequency("D16S539",11,BigDecimal(0.27673)),
    PopulationSampleFrequency("D16S539",12,BigDecimal(0.27623)),
    PopulationSampleFrequency("D16S539",13,BigDecimal(0.1379)),
    PopulationSampleFrequency("D16S539",14,BigDecimal(0.01917)),
    PopulationSampleFrequency("D16S539",15,BigDecimal(0.00131)),
    PopulationSampleFrequency("D18S51",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D18S51",9,BigDecimal(0.00038)),
    PopulationSampleFrequency("D18S51",10,BigDecimal(0.00728)),
    PopulationSampleFrequency("D18S51",10.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",11,BigDecimal(0.01144)),
    PopulationSampleFrequency("D18S51",12,BigDecimal(0.12708)),
    PopulationSampleFrequency("D18S51",13,BigDecimal(0.11537)),
    PopulationSampleFrequency("D18S51",13.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",14,BigDecimal(0.20496)),
    PopulationSampleFrequency("D18S51",14.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",15,BigDecimal(0.14191)),
    PopulationSampleFrequency("D18S51",15.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",16,BigDecimal(0.12002)),
    PopulationSampleFrequency("D18S51",16.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",17,BigDecimal(0.12407)),
    PopulationSampleFrequency("D18S51",18,BigDecimal(0.06715)),
    PopulationSampleFrequency("D18S51",19,BigDecimal(0.03639)),
    PopulationSampleFrequency("D18S51",20,BigDecimal(0.01954)),
    PopulationSampleFrequency("D18S51",21,BigDecimal(0.01144)),
    PopulationSampleFrequency("D18S51",22,BigDecimal(0.00859)),
    PopulationSampleFrequency("D18S51",22.2,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",23,BigDecimal(0.00235)),
    PopulationSampleFrequency("D18S51",24,BigDecimal(0.00104)),
    PopulationSampleFrequency("D18S51",25,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",26,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("D18S51",27,BigDecimal(0.000273642732049037)),
    PopulationSampleFrequency("PentaD",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("PentaD",2.2,BigDecimal(0.00358)),
    PopulationSampleFrequency("PentaD",3.2,BigDecimal(0.000280017921146953)),
    PopulationSampleFrequency("PentaD",5,BigDecimal(0.00146)),
    PopulationSampleFrequency("PentaD",6,BigDecimal(0.00039)),
    PopulationSampleFrequency("PentaD",7,BigDecimal(0.01014)),
    PopulationSampleFrequency("PentaD",8,BigDecimal(0.01366)),
    PopulationSampleFrequency("PentaD",9,BigDecimal(0.19036)),
    PopulationSampleFrequency("PentaD",9.2,BigDecimal(0.00056)),
    PopulationSampleFrequency("PentaD",10,BigDecimal(0.20671)),
    PopulationSampleFrequency("PentaD",11,BigDecimal(0.16045)),
    PopulationSampleFrequency("PentaD",12,BigDecimal(0.17008)),
    PopulationSampleFrequency("PentaD",13,BigDecimal(0.1684)),
    PopulationSampleFrequency("PentaD",14,BigDecimal(0.0541)),
    PopulationSampleFrequency("PentaD",15,BigDecimal(0.01512)),
    PopulationSampleFrequency("PentaD",16,BigDecimal(0.00364)),
    PopulationSampleFrequency("PentaD",17,BigDecimal(0.0009)),
    PopulationSampleFrequency("PentaD",18,BigDecimal(0.000280017921146953)),
    PopulationSampleFrequency("PentaD",19,BigDecimal(0.000280017921146953)),
    PopulationSampleFrequency("D21S11",-1,BigDecimal(0.000272360823619131)),
    PopulationSampleFrequency("D21S11",19,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",24,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",24.2,BigDecimal(0.00087)),
    PopulationSampleFrequency("D21S11",25,BigDecimal(0.00033)),
    PopulationSampleFrequency("D21S11",25.2,BigDecimal(0.00054)),
    PopulationSampleFrequency("D21S11",26,BigDecimal(0.00158)),
    PopulationSampleFrequency("D21S11",26.2,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",27,BigDecimal(0.01638)),
    PopulationSampleFrequency("D21S11",27.2,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",28,BigDecimal(0.09375)),
    PopulationSampleFrequency("D21S11",28.2,BigDecimal(0.00076)),
    PopulationSampleFrequency("D21S11",29,BigDecimal(0.19643)),
    PopulationSampleFrequency("D21S11",29.2,BigDecimal(0.00207)),
    PopulationSampleFrequency("D21S11",30,BigDecimal(0.27228)),
    PopulationSampleFrequency("D21S11",30.2,BigDecimal(0.02536)),
    PopulationSampleFrequency("D21S11",31,BigDecimal(0.06209)),
    PopulationSampleFrequency("D21S11",31.2,BigDecimal(0.11312)),
    PopulationSampleFrequency("D21S11",32,BigDecimal(0.0092)),
    PopulationSampleFrequency("D21S11",32.1,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",32.2,BigDecimal(0.13843)),
    PopulationSampleFrequency("D21S11",33,BigDecimal(0.00212)),
    PopulationSampleFrequency("D21S11",33.1,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",33.2,BigDecimal(0.0543)),
    PopulationSampleFrequency("D21S11",33.3,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",34,BigDecimal(0.00044)),
    PopulationSampleFrequency("D21S11",34.2,BigDecimal(0.00637)),
    PopulationSampleFrequency("D21S11",35,BigDecimal(0.00103)),
    PopulationSampleFrequency("D21S11",35.1,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",35.2,BigDecimal(0.00087)),
    PopulationSampleFrequency("D21S11",36,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",36.2,BigDecimal(0.000272064424855806)),
    PopulationSampleFrequency("D21S11",37,BigDecimal(0.000272064424855806))
  )

  val populationBaseFrequency = new PopulationBaseFrequency("A", 0, ProbabilityModel.HardyWeinberg, seqPopulation)

  "calculate contributors" should {
    "work with 2 contributors" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("A")).thenReturn(Future(Some(populationBaseFrequency)))

      val kitsService = mock[StrKitService]
      when(kitsService.list()).thenReturn(Future.successful(Stubs.strKits))

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      val genotypification = Map("CSF1PO" -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51" -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11" -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818" -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520" -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA" -> List(Allele(19), Allele(20), Allele(23), Allele(24)),
        "TH01" -> List(Allele(6), Allele(9.3)),
        "TPOX" -> List(Allele(12), Allele(11)),
        "vWA" -> List(Allele(18), Allele(17), Allele(16)))
      val target = new ProbabilityServiceImpl(populationBaseFrequencyService, null, null, kitsService, paramsProvider, categoryService)

      val result: Int = Await.result(target.calculateContributors(Analysis("", null, "", genotypification, None),
        AlphanumericId("SOSPECHOSO"), StatOption("A", "1", 0, 0.4, Some(0.5))), duration)
      result mustBe 2

    }

    "work with 4 contributors" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("A")).thenReturn(Future(Some(populationBaseFrequency)))

      val kitsService = mock[StrKitService]
      when(kitsService.list()).thenReturn(Future.successful(Stubs.strKits))

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      val genotypification = Map("CSF1PO" -> List(Allele(13), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(9), Allele(12), Allele(8), Allele(13), Allele(11)),
        "D16S539" -> List(Allele(11), Allele(12), Allele(10), Allele(9)),
        "D18S51" -> List(Allele(15), Allele(14), Allele(16), Allele(13), Allele(17)),
        "D21S11" -> List(Allele(32.2), Allele(31), Allele(28), Allele(30), Allele(31.2)),
        "D3S1358" -> List(Allele(14), Allele(16), Allele(17), Allele(15)),
        "D5S818" -> List(Allele(12), Allele(7), Allele(9), Allele(13), Allele(11), Allele(10)),
        "D7S520" -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(11), Allele(14), Allele(10), Allele(8)),
        "FGA" -> List(Allele(22), Allele(26), Allele(19), Allele(24), Allele(21), Allele(25)),
        "TH01" -> List(Allele(7), Allele(9.3), Allele(9), Allele(6)),
        "TPOX" -> List(Allele(12), Allele(11), Allele(8)),
        "vWA" -> List(Allele(18), Allele(17), Allele(16), Allele(19)))
      val target = new ProbabilityServiceImpl(populationBaseFrequencyService, null, null, kitsService, paramsProvider, categoryService)

      val result: Int = Await.result(target.calculateContributors(Analysis("", null, "", genotypification, None),
        AlphanumericId("SOSPECHOSO"), StatOption("A", "1", 0, 0.4, Some(0.5))), duration)
      result mustBe 4
    }
    "not work with 2 contributors and 1 trisomy with no trisomies configured" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("A")).thenReturn(Future(Some(populationBaseFrequency)))

      val kitsService = mock[StrKitService]
      when(kitsService.list()).thenReturn(Future.successful(Stubs.strKits))

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(0)

      val genotypification = Map("CSF1PO" -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51" -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11" -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818" -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520" -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA" -> List(Allele(19), Allele(20), Allele(23), Allele(24), Allele(22)),
        "TH01" -> List(Allele(6), Allele(9.3)),
        "TPOX" -> List(Allele(12), Allele(11)),
        "vWA" -> List(Allele(18), Allele(17), Allele(16)))
      val target = new ProbabilityServiceImpl(populationBaseFrequencyService, null, null, kitsService, paramsProvider, categoryService)

      val result: Int = Await.result(target.calculateContributors(Analysis("", null, "Identifiler", genotypification, None),
        AlphanumericId("SOSPECHOSO"), StatOption("A", "1", 0, 0.4, Some(0.5))), duration)
      result mustBe 3
    }
    "work with 2 contributors and 1 trisomy with 1 trisomy configured" in {
      val populationBaseFrequencyService = mock[PopulationBaseFrequencyService]
      when(populationBaseFrequencyService.getByName("A")).thenReturn(Future(Some(populationBaseFrequency)))

      val kitsService = mock[StrKitService]
      when(kitsService.list()).thenReturn(Future.successful(Stubs.strKits))

      val categoryService = mock[CategoryService]
      when(categoryService.getCategory(any[AlphanumericId])).thenReturn(Some(Stubs.fullCatA1))

      val paramsProvider = mock[QualityParamsProvider]
      when(paramsProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(1)

      val genotypification = Map("CSF1PO" -> List(Allele(11), Allele(10), Allele(12)),
        "D13S317" -> List(Allele(11), Allele(14), Allele(10), Allele(13)),
        "D16S539" -> List(Allele(12), Allele(13), Allele(10)),
        "D18S51" -> List(Allele(15), Allele(14), Allele(16)),
        "D21S11" -> List(Allele(28), Allele(30), Allele(29), Allele(31)),
        "D3S1358" -> List(Allele(15), Allele(18), Allele(16)),
        "D5S818" -> List(Allele(11), Allele(10), Allele(12), Allele(13)),
        "D7S520" -> List(Allele(11), Allele(12), Allele(10)),
        "D8S1179" -> List(Allele(13), Allele(15), Allele(12)),
        "FGA" -> List(Allele(19), Allele(20), Allele(23), Allele(24), Allele(22)),
        "TH01" -> List(Allele(6), Allele(9.3)),
        "TPOX" -> List(Allele(12), Allele(11)),
        "vWA" -> List(Allele(18), Allele(17), Allele(16)))
      val target = new ProbabilityServiceImpl(populationBaseFrequencyService, null, null, kitsService, paramsProvider, categoryService)

      val result: Int = Await.result(target.calculateContributors(Analysis("", null, "Identifiler", genotypification, None),
        AlphanumericId("SOSPECHOSO"), StatOption("A", "1", 0, 0.4, Some(0.5))), duration)
      result mustBe 2
    }
  }

  "get stats" should {
    "get default parameters for LR" in {
      val mockPopulationService = mock[PopulationBaseFrequencyService]
      when(mockPopulationService.getDefault()).thenReturn(Future.successful(Some(Stubs.baseFrequency)))

      val mockProfileDataService = mock[ProfileDataService]
      when(mockProfileDataService.get(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(mockProfileDataService.findProfileDataLocalOrSuperior(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mockLaboratoryService = mock[LaboratoryService]
      when(mockLaboratoryService.get(any[String])).thenReturn(Future.successful(Some(Stubs.laboratory)))

      val calculator = new ProbabilityServiceImpl(mockPopulationService, mockProfileDataService, mockLaboratoryService, null, null, null)

      val stats = Await.result(calculator.getStats(SampleCode("AR-C-SHDG-12345")), duration)

      stats.isDefined mustBe true
      stats.get.frequencyTable mustBe Stubs.baseFrequency.name
      stats.get.probabilityModel mustBe Stubs.baseFrequency.model
      stats.get.theta mustBe Stubs.baseFrequency.theta
      stats.get.dropIn mustBe Stubs.laboratory.dropIn
      stats.get.dropOut.get mustBe Stubs.laboratory.dropOut

    }

    "be none when there is no laboratory configured" in {
      val mockPopulationService = mock[PopulationBaseFrequencyService]
      when(mockPopulationService.getDefault()).thenReturn(Future.successful(Some(Stubs.baseFrequency)))

      val mockProfileDataService = mock[ProfileDataService]
      when(mockProfileDataService.get(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))
      when(mockProfileDataService.findProfileDataLocalOrSuperior(any[SampleCode])).thenReturn(Future.successful(Some(Stubs.profileData)))

      val mockLaboratoryService = mock[LaboratoryService]
      when(mockLaboratoryService.get(any[String])).thenReturn(Future.successful(None))

      val calculator = new ProbabilityServiceImpl(mockPopulationService, mockProfileDataService, mockLaboratoryService, null, null, null)

      val stats = Await.result(calculator.getStats(SampleCode("AR-C-SHDG-12345")), duration)

      stats mustBe None
    }
  }
}
