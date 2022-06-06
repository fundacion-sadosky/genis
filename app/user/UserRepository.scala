package user

import scala.Left
import scala.Right
import scala.collection.JavaConversions.seqAsJavaList
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.util.Failure
import scala.util.Success
import scala.util.Try

import com.unboundid.ldap.sdk.AddRequest
import com.unboundid.ldap.sdk.AsyncRequestID
import com.unboundid.ldap.sdk.AsyncResultListener
import com.unboundid.ldap.sdk.AsyncSearchResultListener
import com.unboundid.ldap.sdk.Attribute
import com.unboundid.ldap.sdk.BindResult
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPConnectionPool
import com.unboundid.ldap.sdk.LDAPException
import com.unboundid.ldap.sdk.LDAPResult
import com.unboundid.ldap.sdk.Modification
import com.unboundid.ldap.sdk.ModificationType
import com.unboundid.ldap.sdk.ModifyRequest
import com.unboundid.ldap.sdk.RDN
import com.unboundid.ldap.sdk.ResultCode
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchResultReference
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.ldap.sdk.SimpleBindRequest
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest

import akka.actor.ActorSystem
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import play.api.Logger

abstract class UserRepository {
  def blockingGet(userId: String, password: String): Either[String, LdapUser]
  def get(userId: String): Future[LdapUser]
  def getOrEmpty(userId: String): Future[Option[LdapUser]]
  def findByRole(roleId: String): Future[Seq[LdapUser]]
  def bind(userId: String, password: String): Future[Boolean]
  def create(user: LdapUser, password: String): Future[String]
  def listAllUsers(): Future[Seq[LdapUser]]
  //def listAllUsersView: Future[Seq[LdapUser]]
  def finfByUid(uids: Seq[String]): Future[Seq[LdapUser]]
  def setStatus(userId: String, newStatus: UserStatus.Value): Future[Unit]
  def updateUser(user: LdapUser): Future[Boolean]
  def findByStatus(status: UserStatus.Value): Future[Seq[LdapUser]]
  def findByGeneMapper(geneMapper: String): Future[Option[LdapUser]]
  def clearPass(userId: String,
                newStatus: UserStatus.Value,
                password:String,
                encryptrdTotpSecret: Array[Byte]): Future[String]
  def findSuperUsers(): Future[Seq[LdapUser]]
}

@Singleton
class LdapUserRepository @Inject() (
    val akkaSystem: ActorSystem,
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("usersDn") usersDn: String,
    @Named("adminDn") adminDn: String,
    @Named("adminPassword") adminPassword: String) extends UserRepository with LdapRepository {

  private implicit val ldapContext= akkaSystem.dispatchers.lookup("play.akka.actor.ldap-context")

  val baseDn: DN = new DN(usersDn)
  val baseSearchConnection = searchConnection
  val baseBindConnectionPool = bindConnectionPool

  private val entryToLdapUser = (entry: SearchResultEntry) => Try {
    val phone2 = Option(entry.getAttribute("mobile")).map { _.getValue }

    LdapUser(
      entry.getAttribute("uid").getValue(),
      entry.getAttribute("givenName").getValue(),
      entry.getAttribute("sn").getValue(),
      entry.getAttribute("mail").getValue(),
      entry.getAttribute("employeeType").getValues(),
      entry.getAttribute("o").getValue, //TODO cambiar por un valor acorde
      entry.getAttribute("telephoneNumber").getValue,
      phone2,
      UserStatus.withName(entry.getAttribute("street").getValue), //TODO cambiar por un valor acorde
      Array.emptyByteArray,
      Array.emptyByteArray,
      entry.getAttribute("jpegPhoto").getValueByteArray, //TODO cambiar por un valor acorde
      entry.getAttribute("title").getValueAsBoolean)
  }

  private val entryToOptionLdapUser = (entry: SearchResultEntry) => Try {
    val phone2 = Option(entry.getAttribute("mobile")).map { _.getValue }

    Some(LdapUser(
      entry.getAttribute("uid").getValue(),
      entry.getAttribute("givenName").getValue(),
      entry.getAttribute("sn").getValue(),
      entry.getAttribute("mail").getValue(),
      entry.getAttribute("employeeType").getValues(),
      entry.getAttribute("o").getValue, //TODO cambiar por un valor acorde
      entry.getAttribute("telephoneNumber").getValue,
      phone2,
      UserStatus.withName(entry.getAttribute("street").getValue), //TODO cambiar por un valor acorde
      Array.emptyByteArray,
      Array.emptyByteArray,
      entry.getAttribute("jpegPhoto").getValueByteArray, //TODO cambiar por un valor acorde
      entry.getAttribute("title").getValueAsBoolean))
  }

  def setTOTP(userId: String, totpenc: Array[Byte]): Future[Boolean] = {
    val promise = Promise[Boolean]

    val uidDn = new RDN("uid", userId)
    val modT = new Modification(ModificationType.ADD, "jpegPhoto", totpenc)
    val modS = new Modification(ModificationType.ADD, "o", userId)
    val modG = new Modification(ModificationType.ADD, "street", UserStatus.active.toString)
    val modP = new Modification(ModificationType.ADD, "telephoneNumber", "41188080")
    val modUS = new Modification(ModificationType.ADD, "title", "false")
    val modReq = new ModifyRequest(new DN(uidDn, baseDn), Seq(modT, modS, modG, modP, modUS))

    val resultListener: AsyncResultListener = new AsyncResultListener {
      def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
        if (ldapResult.getResultCode == ResultCode.SUCCESS) {
          promise.success(true)
        } else {
          promise.failure(new LDAPException(ldapResult.getResultCode))
        }
        ()
      }
    }

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {
        connection.asyncModify(modReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }
    }

    promise.future
  }

  override def get(userId: String): Future[LdapUser] = {

    val userPromise = Promise[LdapUser]

    val searchResultListener = new AsyncSearchResultListener {
      def searchEntryReturned(entry: SearchResultEntry): Unit = {
        val user = entryToLdapUser(entry)
        userPromise complete user
        ()
      }
      def searchReferenceReturned(ref: SearchResultReference): Unit = ()
      def searchResultReceived(requestId: AsyncRequestID, result: SearchResult): Unit = {
        if (result.getEntryCount == 0) userPromise failure new NoSuchElementException(s"Can't find $userId in LDAP")
        else if (result.getEntryCount > 1) userPromise failure new Exception(s"Found  ${result.getEntryCount} entries with uid $userId")
        ()
      }
    }

    val filter: Filter = Filter.createEqualityFilter("uid", userId)
    val searchRequest = new SearchRequest(searchResultListener, baseDn.toString(), SearchScope.SUB, filter);
    searchConnection.asyncSearch(searchRequest)

    userPromise.future
  }

  override def getOrEmpty(userId: String): Future[Option[LdapUser]] = {

    val userPromise = Promise[Option[LdapUser]]

    val searchResultListener = new AsyncSearchResultListener {
      def searchEntryReturned(entry: SearchResultEntry): Unit = {
        val user = entryToOptionLdapUser(entry)
        userPromise complete user
        ()
      }
      def searchReferenceReturned(ref: SearchResultReference): Unit = ()
      def searchResultReceived(requestId: AsyncRequestID, result: SearchResult): Unit = {
        val userEmpty : Option[LdapUser] = None
        if (result.getEntryCount == 0) userPromise complete Try{userEmpty}
        else if (result.getEntryCount > 1) userPromise complete Try{userEmpty}
        ()
      }
    }

    val filter: Filter = Filter.createEqualityFilter("uid", userId)
    val searchRequest = new SearchRequest(searchResultListener, baseDn.toString(), SearchScope.SUB, filter);
    searchConnection.asyncSearch(searchRequest)

    userPromise.future
  }

  val objClasses = Seq("inetOrgPerson", "organizationalPerson", "person")

  def create(user: LdapUser, password: String): Future[String] = {
    val future = Promise[String]
    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {

        val attributes = Seq(
          new Attribute("uid", user.userName),
          new Attribute("givenName", user.firstName),
          new Attribute("sn", user.lastName),
          new Attribute("cn", user.fullName),
          new Attribute("mail", user.email),
          new Attribute("employeeType", user.roles),
          new Attribute("jpegPhoto", user.encryptrdTotpSecret),
          new Attribute("telephoneNumber", user.phone1),
          new Attribute("o", user.geneMapperId),
          new Attribute("street", UserStatus.pending.toString),
          new Attribute("title", user.superuser.toString),
          //new Attribute("photo", user.encryptedPrivateKey),
          //new Attribute("audio", user.encryptedPublicKey),
          new Attribute("objectclass", objClasses))

        val attributesWithPhone2 = user.phone2.fold(attributes)(p2 => attributes :+ new Attribute("mobile", p2))

        val uidDn = new RDN("uid", user.userName)
        val dn = new DN(uidDn, baseDn)
        val addRequest = new AddRequest(dn, attributesWithPhone2);

        connection.add(addRequest)

        val changePasswordReq = new PasswordModifyExtendedRequest(
          dn.toString(),
          null,
          password);
        connection.processExtendedOperation(changePasswordReq)
        future.success(user.userName)
      } else {
        future.failure(new LDAPException(bindResult.getResultCode()))
      }
    }
    future.future
  }

  override def updateUser(user: LdapUser): Future[Boolean] = {

    val promise = Promise[Boolean]

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {

        val modRs = new Modification(ModificationType.REPLACE, "employeeType", user.roles.toArray: _*)
        val modFn = new Modification(ModificationType.REPLACE, "givenName", user.firstName)
        val modLn = new Modification(ModificationType.REPLACE, "sn", user.lastName)
        val modEm = new Modification(ModificationType.REPLACE, "mail", user.email)
        val modGi = new Modification(ModificationType.REPLACE, "o", user.geneMapperId)
        val modPh1 = new Modification(ModificationType.REPLACE, "telephoneNumber", user.phone1)
        val modSU = new Modification(ModificationType.REPLACE, "title", user.superuser.toString)
        val mods = Seq(modRs, modFn, modLn, modEm, modGi, modPh1, modSU)

        val modsWithPhone2 = user.phone2
          .fold(mods :+ new Modification(ModificationType.REPLACE, "mobile")) (p2 => mods :+ new Modification(ModificationType.REPLACE, "mobile", p2))

        val uidDn = new RDN("uid", user.userName)
        val dn = new DN(uidDn, baseDn)
        val modReq = new ModifyRequest(dn, modsWithPhone2)

        val resultListener = new AsyncResultListener {
          def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
            if (ldapResult.getResultCode == ResultCode.SUCCESS) {
              promise.success(true)
            } else {
              promise.failure(new LDAPException(ldapResult.getResultCode))
            }
            ()
          }
        }
        connection.asyncModify(modReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }
    }

    promise.future
  }

  override def finfByUid(uids: Seq[String]): Future[Seq[LdapUser]] = {
    val filters = uids map { Filter.createEqualityFilter("uid", _) }
    findLdapObjects(Filter.createORFilter(filters), entryToLdapUser)
  }

  override def findByRole(roleId: String): Future[Seq[LdapUser]] = {
    val filter: Filter = Filter.createEqualityFilter("employeeType", roleId)
    findLdapObjects(filter, entryToLdapUser)
  }

  override def listAllUsers(): Future[Seq[LdapUser]] = {
    val filter: Filter = Filter.createEqualityFilter("objectClass", "person")
    findLdapObjects(filter, entryToLdapUser)
  }

  override def bind(userId: String, password: String): Future[Boolean] = Future {
    withConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val bindRequest = new SimpleBindRequest(new DN(uidDn, baseDn), password)
      try {
        val bindResult: BindResult = connection.bind(bindRequest)
        bindResult.getResultCode() == ResultCode.SUCCESS
      } catch {
        case ex: LDAPException => {
          logger.error(ex.getMessage())
          false
        }
      }
    }
  }
  override def clearPass(userId: String,
                         newStatus: UserStatus.Value,
                         password: String,
                         encryptrdTotpSecret: Array[Byte]): Future[String] = {

    val promise = Promise[String]

    val uidDn = new RDN("uid", userId)

    val modReq = new ModifyRequest(new DN(uidDn, baseDn),
      new Modification(ModificationType.REPLACE, "street", newStatus.toString),
      new Modification(ModificationType.REPLACE, "jpegPhoto", encryptrdTotpSecret)
    )

    val resultListener: AsyncResultListener = new AsyncResultListener {
      def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
        if (ldapResult.getResultCode == ResultCode.SUCCESS) {
          promise.success(userId)
        } else {
          promise.failure(new LDAPException(ldapResult.getResultCode))
        }
        ()
      }
    }

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {
        connection.asyncModify(modReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }

      val changePasswordReq = new PasswordModifyExtendedRequest(
        new DN(uidDn, baseDn).toString(),
        null,
        password)
      connection.processExtendedOperation(changePasswordReq)

    }
    promise.future
  }
  override def setStatus(userId: String, newStatus: UserStatus.Value): Future[Unit] = {

    val promise = Promise[Unit]

    val uidDn = new RDN("uid", userId)
    val mod = new Modification(ModificationType.REPLACE, "street", newStatus.toString)
    val modReq = new ModifyRequest(new DN(uidDn, baseDn), mod)

    val resultListener: AsyncResultListener = new AsyncResultListener {
      def ldapResultReceived(requestID: AsyncRequestID, ldapResult: LDAPResult) = {
        if (ldapResult.getResultCode == ResultCode.SUCCESS) {
          promise.success(())
        } else {
          promise.failure(new LDAPException(ldapResult.getResultCode))
        }
        ()
      }
    }

    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)

      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {
        connection.asyncModify(modReq, resultListener)
      } else {
        promise.failure(new LDAPException(bindResult.getResultCode))
      }
    }

    promise.future
  }

  override def findByStatus(status: UserStatus.Value): Future[Seq[LdapUser]] = {
    findLdapObjects(Filter.createEqualityFilter("street", status.toString), entryToLdapUser)
  }

  override def blockingGet(userId: String, password: String): Either[String, LdapUser] = {
    withConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val bindRequest = new SimpleBindRequest(new DN(uidDn, baseDn), password)
      val bindResult: BindResult = connection.bind(bindRequest)

      if (bindResult.getResultCode() == ResultCode.SUCCESS) {
        val filter: Filter = Filter.createEqualityFilter("uid", userId)
        val searchRequest = new SearchRequest(baseDn.toString(), SearchScope.SUB, filter);
        val searchResult = connection.search(searchRequest)
        val resultEntry = Option(searchResult.getSearchEntry(baseDn.toString()))

        resultEntry.fold[Either[String, LdapUser]] {
          Left("No entries found for " + searchRequest.toString())
        } {
          entry =>
            val user = entryToLdapUser(entry)

            user match {
              case Success(user)  => Right(user)
              case Failure(error) => Left(error.getMessage)
            }
        }

      } else {
        Left(bindResult.getDiagnosticMessage())
      }
    }
  }

  override def findByGeneMapper(geneMapper: String): Future[Option[LdapUser]] = {
    val filter: Filter = Filter.createANDFilter(Filter.createEqualityFilter("o", geneMapper), Filter.createEqualityFilter("street", UserStatus.active.toString))

    findLdapObjects(filter, entryToLdapUser) map { seq => seq.headOption }
  }

  override def findSuperUsers(): Future[Seq[LdapUser]] = {
    val filter: Filter = Filter.createEqualityFilter("title", true.toString)
    findLdapObjects(filter, entryToLdapUser)
  }
}

