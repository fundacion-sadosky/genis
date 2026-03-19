package search

import javax.inject.Singleton
import profiledata.{ProfileData, ProfileDataSearch, ProfileDataWithBatch, ProfileDataFull}
import scala.concurrent.Future

@Singleton
class FullTextSearchServiceImpl extends FullTextSearchService {
  def searchTotalProfileDatas(search: ProfileDataSearch): Future[Int] = Future.successful(0)
  def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch): Future[(Int, Int)] = Future.successful((0, 0))
  def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] = Future.successful(Seq.empty)
  def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = Future.successful(Seq.empty)
  def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = Future.successful(Seq.empty)
  def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]] = Future.successful(Seq.empty)
}
