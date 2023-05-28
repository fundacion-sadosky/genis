package search

import javax.inject.{Inject, Singleton}

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import profiledata.{ProfileData, ProfileDataRepository, ProfileDataSearch, ProfileDataWithBatch, ProfileDataFull}

import scala.concurrent.Future
import scala.language.postfixOps

abstract class FullTextSearchService {
  def searchTotalProfileDatas(search: ProfileDataSearch): Future[Int]
  def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch): Future[(Int, Int)]
  def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]]
  def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterPaging(input: String,page:Int,pageSize:Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]]
  def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]]

}

@Singleton
class FullTextSearchServiceImpl @Inject() (fullTextSearch: FullTextSearch, profileDataRepo: ProfileDataRepository) extends FullTextSearchService {

  override def searchProfileDatas(search: ProfileDataSearch): Future[Seq[ProfileDataFull]] = {
    if (search.input.isEmpty)
      profileDataRepo.getProfilesByUser(search)
    else
      {
        fullTextSearch.searchProfiles(search).flatMap(list=> {
          profileDataRepo.findUploadedProfilesByCodes(list.map(_.globalCode)).map(uploadedProfiles => {
            list.map(pd =>
              ProfileDataFull(
                pd.category,
                pd.globalCode,
                pd.attorney,
                pd.bioMaterialType,
                pd.court,
                pd.crimeInvolved,
                pd.crimeType,
                pd.criminalCase,
                pd.internalSampleCode,
                pd.assignee,
                pd.laboratory,
                pd.deleted,
                pd.deletedMotive,
                pd.responsibleGeneticist,
                pd.profileExpirationDate,
                pd.sampleDate,
                pd.sampleEntryDate,
                pd.dataFiliation,
                uploadedProfiles.contains(pd.globalCode),
                pd.isExternal))
          })
        })
      }
  }

  // solo se utiliza en un test
  override def searchTotalProfileDatas(search: ProfileDataSearch) : Future[Int] = {
    if (search.input.isEmpty)
      profileDataRepo.getTotalProfilesByUser(search)
    else
      fullTextSearch.searchTotalProfiles(search)
  }

  override def searchFilterTotalAndTotalProfileDatas(search: ProfileDataSearch) : Future[(Int, Int)] = {
    if (search.input.isEmpty)
      profileDataRepo.getTotalProfilesByUser(search.userId, search.isSuperUser,search.category).flatMap(totalDBSize => {
        profileDataRepo.getTotalProfilesByUser(search).flatMap(filteredSize => Future {(filteredSize, totalDBSize)})
      })

    else
      profileDataRepo.getTotalProfilesByUser(search.userId, search.isSuperUser, search.category).flatMap(totalDBSize => {
        fullTextSearch.searchTotalProfiles(search).flatMap(filteredSize => Future {(filteredSize, totalDBSize)})
      })
      //fullTextSearch.searchTotalProfiles(search)
  }

  override def searchProfileDatasWithFilter(input: String)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = {
    val search = ProfileDataSearch("", true, 0, Int.MaxValue, input, true, false)
    fullTextSearch.searchProfiles(search).map{ result => result.filter { filter } }
  }
  override def searchProfileDatasWithFilterPaging(input: String,page:Int,pageSize:Int)(filter: ProfileData => Boolean): Future[Seq[ProfileData]] = {
    val search = ProfileDataSearch("", true, page, pageSize, input, true, false)
    fullTextSearch.searchProfiles(search).map{ result => result.filter { filter } }
  }
  override def searchProfileDatasWithFilterNodeAssociation(input: String)(filter: ProfileDataWithBatch => Boolean): Future[Seq[ProfileDataWithBatch]] = {
    val search = ProfileDataSearch("", true, 0, Int.MaxValue, input, true, false)
    fullTextSearch.searchProfilesNodeAssociation(search).map{ result => result.filter { filter } }
  }

}