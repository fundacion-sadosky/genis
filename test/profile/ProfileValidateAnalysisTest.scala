package profile

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration.SECONDS
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import specs.PdgSpec
import stubs.Stubs
import configdata._
import kits.{Locus, LocusService, StrKit, StrKitService}

class ProfileValidateAnalysisTest extends PdgSpec with MockitoSugar {

  "ProfileService" must {
    val duration = Duration(10, SECONDS)

    val qualityParamsMockProvider = mock[QualityParamsProvider]
    when(qualityParamsMockProvider.minLocusQuantityAllowedPerProfile(any[FullCategory], any[StrKit])).thenReturn(5)
    when(qualityParamsMockProvider.maxOverageDeviatedLociPerProfile(any[FullCategory], any[StrKit])).thenReturn(1)
    when(qualityParamsMockProvider.maxAllelesPerLocus(any[FullCategory], any[StrKit])).thenReturn(6)

    val categoryServiceMock = mock[CategoryService]
    when(categoryServiceMock.listCategories).thenReturn(Stubs.categoryMap)

    val kitServiceMock = mock[StrKitService]
    when(kitServiceMock.list()).thenReturn(Future(Stubs.strKits))
    when(kitServiceMock.findLociByKit(any[String])).thenReturn(Future(Stubs.loci))

    val locusServiceMock = mock[LocusService]
    when(locusServiceMock.list).thenReturn(Future.successful(Stubs.locus))

    val target = new ProfileServiceImpl(null, null, null, kitServiceMock, null, qualityParamsMockProvider, categoryServiceMock,null, null, locusServiceMock, null, null, null, null)

    "take for valid a new analysis with less than minimun allowed markers" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2)),
        "LOCUS 2" -> List(Allele(1), Allele(2)),
        "LOCUS 3" -> List(Allele(1), Allele(2)),
        "LOCUS 4" -> List(Allele(1), Allele(2)),
        "LOCUS 5" -> List(Allele(1), Allele(1)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 1, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isRight must be (true)
    }

    "take for valid a new analysis with less than minimun allowed markers required locus" in {

      val locusServiceMock = mock[LocusService]
      val target = new ProfileServiceImpl(null, null, null, kitServiceMock, null, qualityParamsMockProvider, categoryServiceMock,null, null, locusServiceMock, null, null, null, null)
      when(locusServiceMock.list).thenReturn(Future.successful(List(Locus("LOCUS 1", "LOCUS 1", Some("1"), 2, 3, 1,false),
        Locus("LOCUS 2", "LOCUS 2", Some("1"), 2, 3, 1,true),
        Locus("LOCUS 3", "LOCUS 3", Some("1"), 2, 3, 1,true),
        Locus("LOCUS 4", "LOCUS 4", Some("1"), 2, 3, 1,false),
        Locus("LOCUS 5", "LOCUS 5", Some("1"), 2, 3, 1,false),
        Locus("LOCUS 6", "LOCUS 6", Some("1"), 2, 3, 1,false),
        Locus("LOCUS 7", "LOCUS 7", Some("1"), 2, 3, 1,false))))

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2)),
        "LOCUS 2" -> List(Allele(1), Allele(2)),
        "LOCUS 7" -> List(Allele(1), Allele(2)),
        "LOCUS 4" -> List(Allele(1), Allele(2)),
        "LOCUS 5" -> List(Allele(1), Allele(1)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 1, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

//      result.isRight must be (false)
      result.isRight must be (true) //con la nueva definicion no toma solo los requeridos
    }

    "fail adding a new analysis with less than minimun allowed markers" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2)),
        "LOCUS 2" -> List(Allele(1), Allele(2)),
        "LOCUS 3" -> List(Allele(1), Allele(2)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 1, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isLeft must be (true)
      result.left.get.size must be (1)
      result.left.get(0) mustBe "E0683: El análisis debe contener al menos 5 marcadores."
    }

    "not validate too many alleles for references" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5), Allele(6), Allele(7)),
        "LOCUS 2" -> List(Allele(1), Allele(2), Allele(3)),
        "LOCUS 3" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5)),
        "LOCUS 5" -> List(Allele(1)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 3, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isRight must be (true)
    }

    "fail adding a new analysis with too many alleles" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5), Allele(6), Allele(7)),
        "LOCUS 2" -> List(Allele(1), Allele(2), Allele(3)),
        "LOCUS 3" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5)),
        "LOCUS 5" -> List(Allele(1)))

      val future = target.validateAnalysis(g, Stubs.categoryList(1)._2.id, Some("Identifiler"), 3, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isLeft must be (true)
      result.left.get.size must be (1)
      result.left.get(0) mustBe "E0684: El análisis no puede contener marcadores con más de 6 alelos."
    }

    "fail adding a new analysis with to many markers with trisomies" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2)),
        "LOCUS 2" -> List(Allele(1), Allele(2)),
        "LOCUS 3" -> List(Allele(1), Allele(2)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3)),
        "LOCUS 5" -> List(Allele(1), Allele(1), Allele(3)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 1, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isLeft must be (true)
      result.left.get.size must be (1)
      result.left.get(0) mustBe "E0685: El análisis no puede contener más de 1 marcadores con trisomías."
    }

    "take for valid a new analysis with less than minimun allowed markers (mixture)" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 2" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 3" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 5" -> List(Allele(1), Allele(1), Allele(3), Allele(4)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 2, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isRight must be (true)
    }

    "fail adding a new analysis with to many markers with trisomies (mixture)" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 2" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 3" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3), Allele(4), Allele(5), Allele(6)),
        "LOCUS 5" -> List(Allele(1), Allele(1), Allele(3), Allele(4), Allele(5), Allele(6)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 1, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isLeft must be (true)
      result.left.get.size must be (1)
      result.left.get(0) mustBe "E0685: El análisis no puede contener más de 1 marcadores con trisomías."
    }

    "return category configuration when right" in {

      val g: Map[String, List[AlleleValue]] = Map(
        "LOCUS 1" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 2" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 3" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 4" -> List(Allele(1), Allele(2), Allele(3), Allele(4)),
        "LOCUS 5" -> List(Allele(1), Allele(1), Allele(3), Allele(4)))

      val future = target.validateAnalysis(g, Stubs.categoryList(0)._2.id, Some("Identifiler"), 2, None, Stubs.analysisTypes.head)
      val result = Await.result(future, duration)

      result.isRight must be (true)
      result.right.get must be (Stubs.fullCatA1.configurations.get(1).get)
    }
  }

}