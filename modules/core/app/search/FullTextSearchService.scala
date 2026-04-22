package search

import profiledata.{ProfileData, ProfileDataWithBatch, ProfileDataView}
import types.SampleCode

import scala.concurrent.Future

// ---------------------------------------------------------------------------
// FullTextSearchService — stub, full migration pending.
// Used by PedigreeService for profile filtering by free-text input.
// ---------------------------------------------------------------------------

trait FullTextSearchService:
  def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[profiledata.ProfileData]]
  def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[profiledata.ProfileData]]
  def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[profiledata.ProfileData]]

@jakarta.inject.Singleton
class FullTextSearchServiceStub extends FullTextSearchService:
  override def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] =
    Future.successful(Seq.empty)
  override def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] =
    Future.successful(Seq.empty)
  override def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileData]] =
    Future.successful(Seq.empty)
