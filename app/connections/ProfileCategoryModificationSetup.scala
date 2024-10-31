package connections

import types.{AlphanumericId, SampleCode}

case class ProfileCategoryModificationSetup(
  globalCode: SampleCode,
  currentCategory: Option[AlphanumericId],
  updatedCategory: AlphanumericId,
  assignee: String,
  profileApproval: ProfileApproval,
  approvalResult: Option[Either[String, SampleCode]]
) {
  def isCategoryUpdated() : Boolean = {
    this
      .currentCategory
      .fold(false)(this.updatedCategory.equals(_))
  }
}


