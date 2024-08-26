package user

import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Try
import com.unboundid.ldap.sdk.AsyncRequestID
import com.unboundid.ldap.sdk.AsyncSearchResultListener
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchResultReference
import com.unboundid.ldap.sdk.SearchScope
import akka.actor.ActorSystem
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import play.api.Logger
import util.BufferActor

trait LdapRepository {
  val logger = Logger(this.getClass())
  val baseSearchConnection: LDAPConnection
  val baseDn: DN
  val baseBindConnectionPool: LDAPConnectionPool
  val akkaSystem: ActorSystem

  def findLdapObjects[T](filter: Filter, func: SearchResultEntry => Try[T]): Future[Seq[T]] = {

    val objectsPromise = Promise[Seq[T]]

    val objectsBuffer = akkaSystem.actorOf(BufferActor.props(objectsPromise))

    val searchResultListener = new AsyncSearchResultListener {
      def searchEntryReturned(entry: SearchResultEntry): Unit = {
        logger.trace(s"New entry found: ${entry.getAttribute("cn").getValue()}")
        func(entry) map { objectsBuffer ! _ }
        ()
      }
      def searchReferenceReturned(ref: SearchResultReference): Unit = ()
      def searchResultReceived(requestId: AsyncRequestID, result: SearchResult): Unit = {

        objectsBuffer ! Terminated
        ()
      }
    }
    val searchRequest = new SearchRequest(searchResultListener, baseDn.toString(), SearchScope.SUB, filter);
    baseSearchConnection.asyncSearch(searchRequest)

    objectsPromise.future
  }

  def withConnection[A](handler: LDAPConnection => A): A = {
    val connection = baseBindConnectionPool.getConnection()
    try {
      handler(connection)
    } finally {
      baseBindConnectionPool.releaseConnection(connection)
    }
  }

}