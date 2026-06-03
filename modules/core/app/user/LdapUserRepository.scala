package user

import scala.concurrent.Future
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
)(using ec: LdapExecutionContext) extends UserRepository with LdapRepository:

  val baseDn: DN = new DN(usersDn)
  val baseSearchConnection: LDAPConnection = searchConnection
  val baseBindConnectionPool: LDAPConnectionPool = bindConnectionPool

  private val entryToLdapUser = (entry: SearchResultEntry) => Try {
    val phone2 = Option(entry.getAttribute("mobile")).map(_.getValue)

    LdapUser(
      userName = requiredAttribute(entry, "uid").getValue,
      firstName = requiredAttribute(entry, "givenName").getValue,
      lastName = requiredAttribute(entry, "sn").getValue,
      email = requiredAttribute(entry, "mail").getValue,
      roles = requiredAttribute(entry, "employeeType").getValues.toSeq,
      geneMapperId = requiredAttribute(entry, "o").getValue,
      phone1 = requiredAttribute(entry, "telephoneNumber").getValue,
      phone2 = phone2,
      status = UserStatus.valueOf(requiredAttribute(entry, "street").getValue),
      encryptedPublicKey = Array.emptyByteArray,
      encryptedPrivateKey = Array.emptyByteArray,
      encryptrdTotpSecret = requiredAttribute(entry, "jpegPhoto").getValueByteArray,
      superuser = requiredAttribute(entry, "title").getValueAsBoolean
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
    searchAll(filter) match
      case Seq() => throw new NoSuchElementException(s"Can't find $userId in LDAP")
      case Seq(entry) => entryToLdapUser(entry).get
      case entries => throw new Exception(s"Found ${entries.size} entries with uid $userId")
  }
