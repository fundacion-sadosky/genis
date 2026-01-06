package repositories

import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import javax.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import scala.jdk.CollectionConverters._
import scala.concurrent.{Future, ExecutionContext}
import models.User
import java.util.Collections

@Singleton
class UserRepository @Inject() (ldapPool: LDAPConnectionPool, config: Configuration)(implicit ec: ExecutionContext) {

  private val baseDn = config.getOptional[String]("ldap.base_dn").getOrElse("dc=genis,dc=local")
  
  def list(): Future[Seq[User]] = Future {
    try {
      // Filter for people
      val filter = "(objectClass=inetOrgPerson)"
      val searchRequest = new SearchRequest(
        baseDn,
        SearchScope.SUB,
        filter,
        "uid", "givenName", "sn", "mail", "employeeType"
      )
      
      val searchResult: SearchResult = ldapPool.search(searchRequest)
      val entries = searchResult.getSearchEntries.asScala
      
      entries.map { entry =>
        User(
          username = Option(entry.getAttributeValue("uid")).getOrElse(""),
          firstName = Option(entry.getAttributeValue("givenName")).getOrElse(""),
          lastName = Option(entry.getAttributeValue("sn")).getOrElse(""),
          email = Option(entry.getAttributeValue("mail")).getOrElse(""),
          role = Option(entry.getAttributeValue("employeeType"))
        )
      }.toSeq
      
    } catch {
      case e: Exception =>
        e.printStackTrace()
        Seq.empty[User]
    }
  }
}
