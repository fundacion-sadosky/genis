package matching

import org.bson.Document
import scala.language.postfixOps

object MatchingSortHelper {
  def getSharedAllelePonderation(globalCode: String,
                                 document: Document,
                                 leftProfile: (String, Integer, java.lang.Boolean, String),
                                 rightProfile: (String, Integer, java.lang.Boolean, String)): Double = {
    val right = rightProfile._1 == globalCode
    val firingProfile = if (right) rightProfile else leftProfile
    val matchingProfile = if (right) leftProfile else rightProfile

    if (!firingProfile._3 && matchingProfile._3) {
      if (right) document.get("result", classOf[Document]).getDouble("leftPonderation") else document.get("result", classOf[Document]).getDouble("rightPonderation")
    } else {
      if (right) document.get("result", classOf[Document]).getDouble("rightPonderation") else document.get("result", classOf[Document]).getDouble("leftPonderation")
    }
  }

  def getGlobalCode(globalCode: String,
                    leftProfile: (String, Integer, java.lang.Boolean, String),
                    rightProfile: (String, Integer, java.lang.Boolean, String)): String = {
    if (leftProfile._1 == globalCode){
      rightProfile._1
    } else {
      leftProfile._1
    }
  }

  def getStatus(globalCode: String,
                isSearchingByOwnerStatus: Boolean,
                document: Document,
                leftProfile: (String, Integer, java.lang.Boolean, String),
                rightProfile: (String, Integer, java.lang.Boolean, String)): String = {

    if (leftProfile._1 == globalCode) {
      if (isSearchingByOwnerStatus){
        document.get("leftProfile", classOf[Document]).getString("status")
      } else {
        document.get("rightProfile", classOf[Document]).getString("status")
      }
    } else {
      if (isSearchingByOwnerStatus){
        document.get("rightProfile", classOf[Document]).getString("status")
      } else {
        document.get("leftProfile", classOf[Document]).getString("status")
      }
    }
  }
}
