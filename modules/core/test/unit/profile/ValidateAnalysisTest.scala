package unit.profile

import configdata.*
import connections.InterconnectionService
import inbox.NotificationService
import jakarta.inject.Provider
import kits.*
import matching.{MatchingAlgorithmService, MatchingRepository, MatchingService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import pedigree.PedigreeService
import play.api.i18n.MessagesApi
import play.api.test.Helpers.stubMessagesApi
import probability.ProbabilityService
import profile.*
import profiledata.ProfileDataRepository
import services.{CacheService, UserService}
import trace.TraceService
import types.AlphanumericId

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, SECONDS}

/**
 * Fidelidad de `validateAnalysis` (admisibilidad por conteo de alelos) contra el legacy `5446c4c`
 * y la lógica documentada en `app/profile/MaxAllelesLogic.md`:
 *
 *   Máximo de alelos por marcador (`trisomyTreshold`):
 *     - referencia: multialélico 4, no multialélico 2
 *     - evidencia:  `maxAllelesPerLocus`
 *   En todos los casos se puede exceder en 1 alelo (marcador trialélico) como mucho
 *   `maxOverageDeviatedLociPerProfile` veces (E0685). Exceder en más de 1 => rechazo E0684.
 */
class ValidateAnalysisTest extends AnyWordSpec with Matchers with MockitoSugar {

  private implicit val ec: ExecutionContext = ExecutionContext.global
  private val timeout = Duration(10, SECONDS)

  private val catId = AlphanumericId("CAT")
  private val autosomal = AnalysisType(1, "Autosomal", mitochondrial = false)

  private def fullCategory(isReference: Boolean): FullCategory = FullCategory(
    id = catId,
    name = "cat",
    description = None,
    group = AlphanumericId("GRP"),
    isReference = isReference,
    filiationDataRequired = false,
    configurations = Map(1 -> CategoryConfiguration()),
    associations = Seq.empty,
    aliases = Seq.empty,
    matchingRules = Seq.empty
  )

  /** Genotipificación con `n` alelos distintos por marcador. */
  private def genotypification(sizes: (String, Int)*): Profile.Genotypification =
    sizes.toMap.map { case (marker, n) => marker -> (1 to n).toList.map(i => AlleleValue(i.toString)) }

  private def lociFor(markers: Iterable[String], minAlleles: Int = 2): Seq[Locus] =
    markers.map(m => Locus(m, m, None, minAlleles, 10, 1, required = true)).toSeq

  private def buildService(
    isReference: Boolean,
    maxAllelesPerLocus: Int,
    maxOverageDeviatedLociPerProfile: Int,
    multiallelic: Boolean,
    loci: Seq[Locus]
  ): ProfileServiceImpl = {
    val locusService = mock[LocusService]
    val kitService = mock[StrKitService]
    val categoryService = mock[CategoryService]
    val qualityParams = mock[QualityParamsProvider]

    when(locusService.list()).thenReturn(Future.successful(loci))
    when(kitService.list()).thenReturn(Future.successful(Seq.empty[StrKit]))
    when(kitService.findLociByKit(any[String]())).thenReturn(Future.successful(List.empty[StrKitLocus]))
    when(categoryService.getCategory(catId)).thenReturn(Future.successful(Some(fullCategory(isReference))))
    when(qualityParams.minLocusQuantityAllowedPerProfile(any[FullCategory](), any[StrKit]())).thenReturn(0)
    when(qualityParams.maxAllelesPerLocus(any[FullCategory](), any[StrKit]())).thenReturn(maxAllelesPerLocus)
    when(qualityParams.maxOverageDeviatedLociPerProfile(any[FullCategory](), any[StrKit]())).thenReturn(maxOverageDeviatedLociPerProfile)
    when(qualityParams.multiallelic(any[FullCategory](), any[StrKit]())).thenReturn(multiallelic)

    new ProfileServiceImpl(
      cache = mock[CacheService],
      profileRepository = mock[ProfileRepository],
      profileDataRepository = mock[ProfileDataRepository],
      matchingRepository = mock[MatchingRepository],
      kitService = kitService,
      matchingService = mock[MatchingService],
      qualityParams = qualityParams,
      categoryService = categoryService,
      notificationService = mock[NotificationService],
      probabilityService = mock[ProbabilityService],
      locusService = locusService,
      traceService = mock[TraceService],
      pedigreeServiceProvider = mock[Provider[PedigreeService]],
      analysisTypeService = mock[AnalysisTypeService],
      labelsSet = Map.empty,
      interconnectionService = mock[InterconnectionService],
      matchingRepo = mock[MatchingRepository],
      userService = mock[UserService],
      matchingAlgorithmService = mock[MatchingAlgorithmService],
      messagesApi = stubMessagesApi()
    )
  }

  private def validate(
    service: ProfileServiceImpl,
    analysis: Profile.Genotypification,
    contributors: Int
  ): Either[List[String], CategoryConfiguration] =
    Await.result(
      service.validateAnalysis(analysis, catId, kitId = None, contributors, `type` = Some(1), autosomal),
      timeout
    )

  "validateAnalysis" should {

    "rechazar un perfil de referencia (no multialélico) con un marcador de 4 alelos (E0684)" in {
      val analysis = genotypification("CSF1PO" -> 4, "TPOX" -> 2, "vWA" -> 2)
      val service = buildService(
        isReference = true, maxAllelesPerLocus = 6, maxOverageDeviatedLociPerProfile = 0,
        multiallelic = false, loci = lociFor(analysis.keys)
      )
      val result = validate(service, analysis, contributors = 1)
      result mustBe a[Left[?, ?]]
      result.left.toOption.get.exists(_.contains("E0684")) mustBe true
    }

    "rechazar un perfil de referencia multialélico con un marcador de 6 alelos (E0684)" in {
      val analysis = genotypification("CSF1PO" -> 6, "TPOX" -> 2)
      val service = buildService(
        isReference = true, maxAllelesPerLocus = 6, maxOverageDeviatedLociPerProfile = 0,
        multiallelic = true, loci = lociFor(analysis.keys)
      )
      val result = validate(service, analysis, contributors = 1)
      result mustBe a[Left[?, ?]]
      result.left.toOption.get.exists(_.contains("E0684")) mustBe true
    }

    "admitir un perfil de evidencia con un marcador un alelo por encima del cap, dentro del presupuesto trialélico" in {
      val analysis = genotypification("CSF1PO" -> 4, "TPOX" -> 2, "vWA" -> 2)
      val service = buildService(
        isReference = false, maxAllelesPerLocus = 3, maxOverageDeviatedLociPerProfile = 1,
        multiallelic = false, loci = lociFor(analysis.keys)
      )
      val result = validate(service, analysis, contributors = 1)
      result mustBe a[Right[?, ?]]
    }

    "rechazar una mezcla (contributors >= 2) que excede el presupuesto de marcadores desviados (E0685)" in {
      val analysis = genotypification("CSF1PO" -> 4, "TPOX" -> 4, "vWA" -> 2, "FGA" -> 2)
      val service = buildService(
        isReference = false, maxAllelesPerLocus = 3, maxOverageDeviatedLociPerProfile = 1,
        multiallelic = false, loci = lociFor(analysis.keys)
      )
      val result = validate(service, analysis, contributors = 3)
      result mustBe a[Left[?, ?]]
      result.left.toOption.get.exists(_.contains("E0685")) mustBe true
    }
  }
}
