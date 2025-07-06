package connections

import types.{AlphanumericId, SampleCode}

case class ProfileCategoryModificationSetup(
                                             globalCode: SampleCode,
                                             currentCategory: Option[AlphanumericId],
                                             updatedCategory: AlphanumericId,
                                             assignee: String,
                                             profileApproval: ProfileApproval,
                                             approvalResult: Option[Either[String, SampleCode]],
                                             userName: String
                                           ) {
  def isCategoryUpdated() : Boolean = {
    this
      .currentCategory
      .fold(false)(
        x => !this.updatedCategory.equals(x)
      )
  }
}




