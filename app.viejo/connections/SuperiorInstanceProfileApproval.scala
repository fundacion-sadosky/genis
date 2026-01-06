package connections

case class SuperiorInstanceProfileApproval(
                                            id: Long,
                                            globalCode: String,
                                            profile: String,
                                            laboratory: String,
                                            laboratoryInstanceOrigin: String,
                                            laboratoryImmediateInstance: String,
                                            sampleEntryDate: Option[java.sql.Date] = None,
                                            errors: Option[String] = None,
                                            receptionDate: Option[java.sql.Timestamp] = None,
                                            profileAssociated: Option[String] = None
                                          ) {

}
