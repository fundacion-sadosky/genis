package matching

import play.api.libs.json._
import profile.Profile
import scenarios.Scenario
import types.SampleCode

import scala.concurrent.Future

trait MatchingService {

  def resumeFindingMatches()

  def findMatches(globalCode: SampleCode, matchType: Option[String]): Unit

  def findMatches(pedigreeId: Long): Unit

  def getByMatchedProfileId(matchingId: String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Option[JsValue]]

  //def getComparedGenotyfications(globalCode: SampleCode, matchedGlobalCode: SampleCode): Future[Option[JsValue]]
  def obtainCompareMixtureGenotypification(results:scala.Seq[Profile]):Seq[CompareMixtureGenotypification]

  def findMatchingResults(globalCode: SampleCode): Future[Option[MatchingResults]]

  def convertHit(matchId: String, firingCode: SampleCode,replicate:Boolean = true): Future[Either[String, Seq[SampleCode]]]

  def convertDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean,replicate:Boolean = true): Future[Either[String, Seq[SampleCode]]]

  def uploadStatus(matchId: String, firingCode: SampleCode, isSuperUser: Boolean): Future[String]

  def canUploadMatchStatus(matchId: String, isCollapsing:Option[Boolean] = None, isScreening:Option[Boolean] = None): Future[Boolean]

  def convertHitOrDiscard(matchId: String, firingCode: SampleCode, isSuperUser: Boolean,action:String): Future[Either[String, Seq[SampleCode]]]

  def getComparedMixtureGenotypification(globalCodes: Seq[SampleCode],matchId:String,isCollapsing:Option[Boolean] = None,isScreening:Option[Boolean] = None): Future[Seq[CompareMixtureGenotypification]]

  def deleteForce(matchId: String, globalCode: SampleCode): Future[Boolean]

  def getByFiringAndMatchingProfile(firingCode: SampleCode, matchingCode: SampleCode): Future[Option[MatchingResult]]

  def validate(scenario: Scenario) : Future[Either[String,String]]

  def validProfilesAssociated(labels: Option[Profile.LabeledGenotypification]): Seq[String]

  def matchesWithPartialHit(globalCode: SampleCode): Future[Seq[MatchResult]]

  def getMatches(search: MatchCardSearch): Future[Seq[MatchCardForense]]

  def getTotalMatches(search: MatchCardSearch): Future[Int]

  def getMatchesPaginated(search: MatchCardSearch): Future[Seq[MatchResult]]

  def getAllTotalMatches(search: MatchCardSearch): Future[Int]

  def getTotalMatchesByGroup(search: MatchGroupSearch): Future[Int]

  def getMatchesByGroup(search: MatchGroupSearch): Future[Seq[MatchingResult]]

  def getMatchResultById(matchingId: Option[String]): Future[Option[MatchResult]]

  def matchesNotDiscarded(globalCode: SampleCode): Future[Seq[MatchResult]]

  def collapse(idCourtCase:Long,user:String):Unit

  def discardCollapsingMatches(ids:List[String],courtCaseId:Long) : Future[Unit]

  def discardCollapsingByLeftProfile(id:String,courtCaseId:Long) : Future[Unit]

  def discardCollapsingByLeftAndRightProfile(id:String,courtCaseId:Long) : Future[Unit]

  def discardCollapsingByRightProfile(id:String,courtCaseId:Long) : Future[Unit]

  def findScreeningMatches(profile:Profile, queryProfiles:List[String],numberOfMismatches: Option[Int]):Future[(Set[MatchResultScreening],Set[MatchResultScreening])]

  def masiveGroupDiscardByGlobalCode(firingCode: SampleCode, isSuperUser: Boolean,replicate:Boolean = true) : Future[Either[String, Seq[SampleCode]]]

  def searchMatchesProfile(globalCode: String): Future[Seq[MatchCard]]

  def updateMatchesLR(matchingLRs: Set[(String, Double)]):Future[Unit]

  def masiveGroupDiscardByMatchesList(firingCode: types.SampleCode, matches: List[String], isSuperUser: Boolean,replicate:Boolean = true) : Future[Either[String, Seq[SampleCode]]]
}