package user

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import javax.inject.{Inject, Named, Singleton}

import security.{LdapUser, UserRepository}

@Singleton
class LdapUserRepository @Inject() (
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("usersDn") usersDn: String
)(using ec: ExecutionContext) extends UserRepository with LdapRepository:

  val baseDn: DN = new DN(usersDn)
  val baseSearchConnection: LDAPConnection = searchConnection
  val baseBindConnectionPool: LDAPConnectionPool = bindConnectionPool

  private val entryToLdapUser = (entry: SearchResultEntry) => Try {
    val phone2 = Option(entry.getAttribute("mobile")).map(_.getValue)

    LdapUser(
      userName = entry.getAttribute("uid").getValue,
      firstName = entry.getAttribute("givenName").getValue,
      lastName = entry.getAttribute("sn").getValue,
      email = entry.getAttribute("mail").getValue,
      roles = entry.getAttribute("employeeType").getValues.toSeq,
      geneMapperId = entry.getAttribute("o").getValue,
      phone1 = entry.getAttribute("telephoneNumber").getValue,
      phone2 = phone2,
      status = UserStatus.valueOf(entry.getAttribute("street").getValue),
      encryptedPublicKey = Array.emptyByteArray,
      encryptedPrivateKey = Array.emptyByteArray,
      encryptrdTotpSecret = entry.getAttribute("jpegPhoto").getValueByteArray,
      superuser = entry.getAttribute("title").getValueAsBoolean
    )
  }

  override def bind(userId: String, password: String): Future[Boolean] = Future {
    withConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val bindRequest = new SimpleBindRequest(new DN(uidDn, baseDn), password)
      try
        val bindResult = connection.bind(bindRequest)
        bindResult.getResultCode == ResultCode.SUCCESS
      catch
        case ex: LDAPException =>
          logger.error(ex.getMessage)
          false
    }
  }

  override def get(userId: String): Future[LdapUser] = Future {
    val filter = Filter.createEqualityFilter("uid", userId)
    searchOne(filter) match
      case Some(entry) => entryToLdapUser(entry).get
      case None => throw new NoSuchElementException(s"Can't find $userId in LDAP")
  }
