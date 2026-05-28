package matching

import javax.inject.{Inject, Singleton}
import profile.Profile
import types.SampleCode
import scala.concurrent.Future

@Singleton
class MatchingServiceImpl @Inject()(
  matchingRepo: MatchingRepository,
  profileMatcher: ProfileMatcher
) extends MatchingService {

  override def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit =
    // matchType "MPI" is pedigree — not yet migrated; skip silently
    matchType match {
      case Some("MPI") => () // pedigree matching not yet implemented in core
      case _           => profileMatcher.findMatchesInBackground(globalCode)
    }

  override def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]] =
    matchingRepo.matchesNotDiscarded(globalCode)

  override def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]] =
    matchingRepo.matchesWithPartialHit(globalCode)

  override def validProfilesAssociated(
    labels: Option[Profile.LabeledGenotypification]
  ): Seq[String] =
    labels.getOrElse(Map.empty).keySet
      .filter(code => code.matches(SampleCode.validationRe.toString()))
      .toSeq

  // TODO: migrar lógica real desde legacy app/matching. new-dev sólo los tenía
  // stubbeados (MatchingServiceStub); se replica ese comportamiento no-op.
  override def findScreeningMatches(
    profile: Profile,
    queryProfiles: List[String],
    numberOfMismatches: Option[Int]
  ): Future[(Set[MatchResultScreening], Set[MatchResultScreening])] =
    Future.successful((Set.empty, Set.empty))

  override def collapse(idCourtCase: Long, user: String): Unit = ()

  override def discardCollapsingByLeftAndRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] =
    Future.successful(())

  override def discardCollapsingByRightProfile(globalCode: String, courtCaseId: Long): Future[Unit] =
    Future.successful(())
}