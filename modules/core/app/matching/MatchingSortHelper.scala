package matching

import org.bson.Document

object MatchingSortHelper:

  def getSharedAllelePonderation(
    globalCode: String,
    document: Document,
    leftProfile: (String, Integer, java.lang.Boolean, String),
    rightProfile: (String, Integer, java.lang.Boolean, String)
  ): Double =
    val right = rightProfile._1 == globalCode
    val firingProfile   = if right then rightProfile else leftProfile
    val matchingProfile = if right then leftProfile else rightProfile

    if !firingProfile._3 && matchingProfile._3 then
      if right then document.get("result", classOf[Document]).getDouble("leftPonderation")
      else document.get("result", classOf[Document]).getDouble("rightPonderation")
    else
      if right then document.get("result", classOf[Document]).getDouble("rightPonderation")
      else document.get("result", classOf[Document]).getDouble("leftPonderation")

  def getGlobalCode(
    globalCode: String,
    leftProfile: (String, Integer, java.lang.Boolean, String),
    rightProfile: (String, Integer, java.lang.Boolean, String)
  ): String =
    if leftProfile._1 == globalCode then rightProfile._1
    else leftProfile._1

  def getStatus(
    globalCode: String,
    isSearchingByOwnerStatus: Boolean,
    document: Document,
    leftProfile: (String, Integer, java.lang.Boolean, String),
    rightProfile: (String, Integer, java.lang.Boolean, String)
  ): String =
    if leftProfile._1 == globalCode then
      if isSearchingByOwnerStatus then
        document.get("leftProfile", classOf[Document]).getString("status")
      else
        document.get("rightProfile", classOf[Document]).getString("status")
    else
      if isSearchingByOwnerStatus then
        document.get("rightProfile", classOf[Document]).getString("status")
      else
        document.get("leftProfile", classOf[Document]).getString("status")
