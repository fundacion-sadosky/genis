package search

import profiledata.{ProfileData, ProfileDataSearch, ProfileDataWithBatch, ProfileDataFull}
import scala.concurrent.Future

abstract class FullTextSearchService {
  def searchTotalProfileDatas(search: ProfileDataSearch): Future[Int]
  def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch): Future[(Int, Int)]
  def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]]
}
