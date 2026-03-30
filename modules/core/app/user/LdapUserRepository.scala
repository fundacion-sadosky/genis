package user

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.jdk.CollectionConverters.*

import com.unboundid.ldap.sdk.*
import com.unboundid.ldap.sdk.extensions.PasswordModifyExtendedRequest
import javax.inject.{Inject, Named, Singleton}

import security.{LdapUser, UserRepository}

@Singleton
class LdapUserRepository @Inject() (
    bindConnectionPool: LDAPConnectionPool,
    searchConnection: LDAPConnection,
    @Named("usersDn") usersDn: String,
    @Named("adminDn") adminDn: String,
    @Named("adminPassword") adminPassword: String
)(using ec: ExecutionContext) extends UserRepository with LdapRepository:

  val baseDn: DN = new DN(usersDn)
  val baseSearchConnection: LDAPConnection = searchConnection
  val baseBindConnectionPool: LDAPConnectionPool = bindConnectionPool

  private val entryToLdapUser = (entry: SearchResultEntry) => Try {
    try {
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
    } catch {
      case ex: Exception =>
        logger.error(s"Error al convertir entry LDAP a LdapUser: ${ex.getMessage}", ex)
        throw ex
    }
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

  override def getOrEmpty(userName: String): Future[Option[LdapUser]] = Future {
    val filter = Filter.createEqualityFilter("uid", userName)
    searchOne(filter).map(entry => entryToLdapUser(entry).get)
  }

  override def listAllUsers(): Future[Seq[LdapUser]] = Future {
    val filter = Filter.createEqualityFilter("objectClass", "person")
    searchAll(filter).flatMap(entry => entryToLdapUser(entry).toOption)
  }

  override def findByRole(roleId: String): Future[Seq[LdapUser]] = Future {
    val filter = Filter.createEqualityFilter("employeeType", roleId)
    searchAll(filter).flatMap(entry => entryToLdapUser(entry).toOption)
  }

  override def finfByUid(uids: Seq[String]): Future[Seq[LdapUser]] = Future {
    val filters = uids.map(Filter.createEqualityFilter("uid", _))
    val orFilter = Filter.createORFilter(filters*)
    searchAll(orFilter).flatMap(entry => entryToLdapUser(entry).toOption)
  }

  override def findByStatus(status: UserStatus): Future[Seq[LdapUser]] = Future {
    val filter = Filter.createEqualityFilter("street", status.toString)
    searchAll(filter).flatMap(entry => entryToLdapUser(entry).toOption)
  }

  override def findByGeneMapper(geneMapper: String): Future[Option[LdapUser]] = Future {
    val filter = Filter.createANDFilter(
      Filter.createEqualityFilter("o", geneMapper),
      Filter.createEqualityFilter("street", UserStatus.active.toString)
    )
    searchAll(filter).flatMap(entry => entryToLdapUser(entry).toOption).headOption
  }

  override def findSuperUsers(): Future[Seq[LdapUser]] = Future {
    val filter = Filter.createEqualityFilter("title", true.toString)
    searchAll(filter).flatMap(entry => entryToLdapUser(entry).toOption)
  }

  override def blockingGet(userId: String, password: String): Either[String, LdapUser] =
    withConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val bindRequest = new SimpleBindRequest(new DN(uidDn, baseDn), password)
      val bindResult = connection.bind(bindRequest)

      if bindResult.getResultCode == ResultCode.SUCCESS then
        val filter = Filter.createEqualityFilter("uid", userId)
        val searchRequest = new SearchRequest(baseDn.toString, SearchScope.SUB, filter)
        val searchResult = connection.search(searchRequest)
        val resultEntry = Option(searchResult.getSearchEntry(baseDn.toString))

        resultEntry.fold[Either[String, LdapUser]](
          Left(s"No entries found for $searchRequest")
        ) { entry =>
          entryToLdapUser(entry) match
            case Success(user) => Right(user)
            case Failure(error) => Left(error.getMessage)
        }
      else
        Left(bindResult.getDiagnosticMessage)
    }

  private def withAdminConnection[A](handler: LDAPConnection => A): A =
    withConnection { connection =>
      val bindRequest = new SimpleBindRequest(new DN(adminDn), adminPassword)
      val bindResult = connection.bind(bindRequest)
      if bindResult.getResultCode == ResultCode.SUCCESS then
        handler(connection)
      else
        throw new LDAPException(bindResult.getResultCode)
    }

  override def setStatus(userId: String, newStatus: UserStatus): Future[Unit] = Future {
    withAdminConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val mod = new Modification(ModificationType.REPLACE, "street", newStatus.toString)
      val modReq = new ModifyRequest(new DN(uidDn, baseDn), mod)
      connection.modify(modReq)
      ()
    }
  }

  private val objClasses = Seq("inetOrgPerson", "organizationalPerson", "person")

  override def create(user: LdapUser, password: String): Future[String] = Future {
    withAdminConnection { connection =>
      val attributes = Seq(
        new Attribute("uid", user.userName),
        new Attribute("givenName", user.firstName),
        new Attribute("sn", user.lastName),
        new Attribute("cn", user.fullName),
        new Attribute("mail", user.email),
        new Attribute("employeeType", user.roles*),
        new Attribute("jpegPhoto", user.encryptrdTotpSecret),
        new Attribute("telephoneNumber", user.phone1),
        new Attribute("o", user.geneMapperId),
        new Attribute("street", UserStatus.pending.toString),
        new Attribute("title", user.superuser.toString),
        new Attribute("objectclass", objClasses*)
      )

      val attributesWithPhone2 = user.phone2.fold(attributes)(p2 => attributes :+ new Attribute("mobile", p2))

      val uidDn = new RDN("uid", user.userName)
      val dn = new DN(uidDn, baseDn)
      val addRequest = new AddRequest(dn, attributesWithPhone2*)

      connection.add(addRequest)

      val changePasswordReq = new PasswordModifyExtendedRequest(dn.toString, null, password)
      connection.processExtendedOperation(changePasswordReq)
      user.userName
    }
  }

  override def updateUser(user: LdapUser): Future[Boolean] = Future {
    withAdminConnection { connection =>
      val modRs = new Modification(ModificationType.REPLACE, "employeeType", user.roles*)
      val modFn = new Modification(ModificationType.REPLACE, "givenName", user.firstName)
      val modLn = new Modification(ModificationType.REPLACE, "sn", user.lastName)
      val modEm = new Modification(ModificationType.REPLACE, "mail", user.email)
      val modGi = new Modification(ModificationType.REPLACE, "o", user.geneMapperId)
      val modPh1 = new Modification(ModificationType.REPLACE, "telephoneNumber", user.phone1)
      val modSU = new Modification(ModificationType.REPLACE, "title", user.superuser.toString)
      val mods = Seq(modRs, modFn, modLn, modEm, modGi, modPh1, modSU)

      val modsWithPhone2 = user.phone2
        .fold(mods :+ new Modification(ModificationType.REPLACE, "mobile"))(
          p2 => mods :+ new Modification(ModificationType.REPLACE, "mobile", p2)
        )

      val uidDn = new RDN("uid", user.userName)
      val dn = new DN(uidDn, baseDn)
      val modReq = new ModifyRequest(dn, modsWithPhone2*)

      connection.modify(modReq)
      true
    }
  }

  override def clearPass(
    userId: String,
    newStatus: UserStatus,
    password: String,
    encryptrdTotpSecret: Array[Byte]
  ): Future[String] = Future {
    withAdminConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val dn = new DN(uidDn, baseDn)

      val modReq = new ModifyRequest(dn,
        new Modification(ModificationType.REPLACE, "street", newStatus.toString),
        new Modification(ModificationType.REPLACE, "jpegPhoto", encryptrdTotpSecret)
      )
      connection.modify(modReq)

      val changePasswordReq = new PasswordModifyExtendedRequest(dn.toString, null, password)
      connection.processExtendedOperation(changePasswordReq)
      userId
    }
  }

  override def setTOTP(userId: String, totpenc: Array[Byte]): Future[Boolean] = Future {
    withAdminConnection { connection =>
      val uidDn = new RDN("uid", userId)
      val modT = new Modification(ModificationType.ADD, "jpegPhoto", totpenc)
      val modS = new Modification(ModificationType.ADD, "o", userId)
      val modG = new Modification(ModificationType.ADD, "street", UserStatus.active.toString)
      val modP = new Modification(ModificationType.ADD, "telephoneNumber", "41188080")
      val modUS = new Modification(ModificationType.ADD, "title", "false")
      val modReq = new ModifyRequest(new DN(uidDn, baseDn), modT, modS, modG, modP, modUS)
      connection.modify(modReq)
      true
    }
  }
