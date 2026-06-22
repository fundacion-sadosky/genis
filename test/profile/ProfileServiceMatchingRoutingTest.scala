package profile

import configdata.{CategoryService, FullCategory}
import matching._
import org.mockito.Matchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.iteratee.Enumerator
import stubs.Stubs
import types.SampleCode

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, SECONDS}

/**
 * Regresión #202/eeace925: al aceptar un perfil, el matching debe rutearse según
 * el tipo de categoría (MPI/DVI → PedigreeSparkMatcher), y la espera bloqueante
 * debe completarse con los estados del pedigree matcher (PedigreeMatchJobEnded),
 * no sólo con los del matcher directo (MatchJobEndend).
 */
class ProfileServiceMatchingRoutingTest extends PlaySpec with MockitoSugar {

  val duration = Duration(8, SECONDS)

  private def buildTarget(matchingService: MatchingService,
                          categoryService: CategoryService,
                          matchingProcessStatus: MatchingProcessStatus): ProfileServiceImpl =
    new ProfileServiceImpl(
      null, null, null, null, null,
      matchingService, null, categoryService, null, null,
      null, null, null, null, null,
      null, null, null, matchingProcessStatus, null)

  "triggerMatchingAndAwait" should {

    "route an MPI category to the pedigree matcher and complete on PedigreeMatchJobEnded" in {
      val profile = Stubs.newProfile
      val category = Stubs.fullCatA1

      val matchingService = mock[MatchingService]
      val categoryService = mock[CategoryService]
      when(categoryService.getCategoryTypeFromFullCategory(any[FullCategory])).thenReturn(Some("MPI"))

      val matchingProcessStatus = mock[MatchingProcessStatus]
      when(matchingProcessStatus.getJobStatus()).thenReturn(Enumerator[MatchJobStatus](PedigreeMatchJobEnded))

      val target = buildTarget(matchingService, categoryService, matchingProcessStatus)

      val result = Await.result(target.triggerMatchingAndAwait(profile, category), duration)

      result mustBe PedigreeMatchJobEnded
      verify(matchingService).findMatches(profile.globalCode, Some("MPI"))
    }

    "route a non-pedigree (type 1) category to the direct matcher and complete on MatchJobEndend" in {
      val profile = Stubs.newProfile
      val category = Stubs.fullCatA1

      val matchingService = mock[MatchingService]
      val categoryService = mock[CategoryService]
      when(categoryService.getCategoryTypeFromFullCategory(any[FullCategory])).thenReturn(None)

      val matchingProcessStatus = mock[MatchingProcessStatus]
      when(matchingProcessStatus.getJobStatus()).thenReturn(Enumerator[MatchJobStatus](MatchJobEndend))

      val target = buildTarget(matchingService, categoryService, matchingProcessStatus)

      val result = Await.result(target.triggerMatchingAndAwait(profile, category), duration)

      result mustBe MatchJobEndend
      verify(matchingService).findMatches(profile.globalCode, None)
    }
  }
}
