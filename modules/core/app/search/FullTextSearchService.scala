package search

import javax.inject.{Inject, Singleton}
import profiledata.{ProfileData, ProfileDataFull, ProfileDataRepository,
                    ProfileDataSearch, ProfileDataWithBatch}

import scala.concurrent.{ExecutionContext, Future}

abstract class FullTextSearchService {
  def searchTotalProfileDatas(search: ProfileDataSearch): Future[Int]
  def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch): Future[(Int, Int)]
  def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]]
}

@Singleton
class FullTextSearchServiceImpl @Inject()(
  fullTextSearch: FullTextSearch,
  profileDataRepo: ProfileDataRepository
)(implicit ec: ExecutionContext) extends FullTextSearchService {

  override def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] =
    if (search.input.isEmpty)
      profileDataRepo.getProfilesByUser(search)
    else
      fullTextSearch.searchProfiles(search).flatMap { list =>
        profileDataRepo.findUploadedProfilesByCodes(list.map(_.globalCode).toList).flatMap { _ =>
          Future.sequence(list.map { pd =>
            profileDataRepo.getIsProfileReplicated(pd.globalCode).map { readOnly =>
              ProfileDataFull(
                pd.category, pd.globalCode, pd.attorney, pd.bioMaterialType,
                pd.court, pd.crimeInvolved, pd.crimeType, pd.criminalCase,
                pd.internalSampleCode, pd.assignee, pd.laboratory, pd.deleted,
                None, pd.responsibleGeneticist, pd.profileExpirationDate,
                pd.sampleDate, pd.sampleEntryDate, None, readOnly, pd.isExternal
              )
            }
          })
        }
      }

  override def searchTotalProfileDatas(search: ProfileDataSearch): Future[Int] =
    if (search.input.isEmpty)
      profileDataRepo.getTotalProfilesByUser(search)
    else
      fullTextSearch.searchTotalProfiles(search)

  override def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch): Future[(Int, Int)] =
    profileDataRepo.getTotalProfilesByUser(search.userId, search.isSuperUser, search.category)
      .flatMap { totalDBSize =>
        (if (search.input.isEmpty)
          profileDataRepo.getTotalProfilesByUser(search)
        else
          fullTextSearch.searchTotalProfiles(search)
        ).map(filteredSize => (filteredSize, totalDBSize))
      }

  override def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = {
    val search = ProfileDataSearch(userId = "", isSuperUser = true, pageSize = Int.MaxValue, input = input)
    fullTextSearch.searchProfiles(search).map(_.filter(filter))
  }

  override def searchProfileDatasWithFilterPaging(input: String, page: Int, pageSize: Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = {
    val search = ProfileDataSearch(userId = "", isSuperUser = true, page = page, pageSize = pageSize, input = input)
    fullTextSearch.searchProfiles(search).map(_.filter(filter))
  }

  override def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]] = {
    val search = ProfileDataSearch(userId = "", isSuperUser = true, pageSize = Int.MaxValue, input = input)
    fullTextSearch.searchProfilesNodeAssociation(search).map(_.filter(filter))
  }
}
