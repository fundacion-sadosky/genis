package services

import javax.inject._
import javax.naming.Context
import javax.naming.directory.{InitialDirContext, SearchControls, SearchResult}
import javax.naming.ldap.InitialLdapContext
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.util.{Failure, Success, Try}
import play.api.Configuration
import com.unboundid.ldap.sdk.{LDAPConnection, LDAPConnectionPool, LDAPException, SearchScope}
import scala.jdk.CollectionConverters._

/**
 * Servicio LDAP Moderno
 * Utiliza UnboundID SDK para mayor compatibilidad y mejor manejo de errores
 */
@Singleton
class LdapService @Inject()(
    config: Configuration
)(implicit ec: ExecutionContext) {

  private val ldapEnabled = config.getOptional[Boolean]("ldap.enabled").getOrElse(false)
  private val providerUrl = config.getOptional[String]("ldap.provider_url").getOrElse("ldap://localhost:389")
  private val baseDn = config.getOptional[String]("ldap.base_dn").getOrElse("dc=genis,dc=local")
  private val userDnPattern = config.getOptional[String]("ldap.user_dn_pattern")
    .getOrElse("uid={0},ou=people,dc=genis,dc=local")
  private val groupSearchBase = config.getOptional[String]("ldap.group_search_base")
    .getOrElse("ou=groups,dc=genis,dc=local")
  private val connectionTimeout = config.getOptional[Int]("ldap.connection_timeout").getOrElse(5000)
  private val readTimeout = config.getOptional[Int]("ldap.read_timeout").getOrElse(5000)

  private val ldapPool = if (ldapEnabled) createConnectionPool else null

  /**
   * Crea un pool de conexiones LDAP
   */
  private def createConnectionPool: Option[LDAPConnectionPool] = {
    Try {
      val host = extractHost(providerUrl)
      val port = extractPort(providerUrl)
      
      val connection = new LDAPConnection(host, port)
      val pool = new LDAPConnectionPool(connection, 10) // 10 conexiones en el pool
      pool
    } match {
      case Success(pool) => Some(pool)
      case Failure(e) => 
        println(s"Error inicializando pool LDAP: ${e.getMessage}")
        None
    }
  }

  /**
   * Extrae el host del URL LDAP
   */
  private def extractHost(url: String): String = {
    url.replace("ldap://", "").replace("ldaps://", "")
      .split(":")(0)
  }

  /**
   * Extrae el puerto del URL LDAP
   */
  private def extractPort(url: String): Int = {
    val parts = url.replace("ldap://", "").replace("ldaps://", "").split(":")
    if (parts.length > 1) parts(1).toInt else 389
  }

  /**
   * Autentica un usuario contra LDAP
   */
  def authenticate(username: String, password: String): Future[Either[String, LdapUser]] = {
    if (!ldapEnabled) {
      return Future.successful(Left("LDAP está deshabilitado"))
    }

    blocking {
      Future {
        Try {
          val userDn = userDnPattern.replace("{0}", username)
          
          ldapPool match {
            case Some(pool) =>
              val connection = pool.getConnection
              try {
                connection.bind(userDn, password)
                val searchResults = connection.search(
                  userDn,
                  SearchScope.BASE,
                  "(objectClass=*)"
                )

                if (searchResults.getEntryCount > 0) {
                  val entry = searchResults.getSearchEntries.get(0)
                  Right(LdapUser(
                    uid = username,
                    cn = Option(entry.getAttributeValue("cn")).getOrElse(""),
                    mail = Option(entry.getAttributeValue("mail")).getOrElse(""),
                    distinguishedName = entry.getDN
                  ))
                } else {
                  Left("Usuario no encontrado")
                }
              } catch {
                case e: LDAPException => 
                  Left(s"Error de autenticación: ${e.getMessage}")
              } finally {
                pool.releaseConnection(connection)
              }
            case None =>
              Left("Conexión LDAP no disponible")
          }
        } match {
          case Success(result) => result
          case Failure(e) => Left(s"Error en LDAP: ${e.getMessage}")
        }
      }
    }
  }

  /**
   * Busca un usuario en LDAP
   */
  def searchUser(username: String): Future[Either[String, LdapUser]] = {
    if (!ldapEnabled) {
      return Future.successful(Left("LDAP está deshabilitado"))
    }

    blocking {
      Future {
        Try {
          ldapPool match {
            case Some(pool) =>
              val connection = pool.getConnection
              try {
                val searchResults = connection.search(
                  baseDn,
                  SearchScope.SUB,
                  s"(uid=$username)"
                )

                if (searchResults.getEntryCount > 0) {
                  val entry = searchResults.getSearchEntries.get(0)
                  Right(LdapUser(
                    uid = username,
                    cn = Option(entry.getAttributeValue("cn")).getOrElse(""),
                    mail = Option(entry.getAttributeValue("mail")).getOrElse(""),
                    distinguishedName = entry.getDN
                  ))
                } else {
                  Left("Usuario no encontrado")
                }
              } finally {
                pool.releaseConnection(connection)
              }
            case None =>
              Left("Conexión LDAP no disponible")
          }
        } match {
          case Success(result) => result
          case Failure(e) => Left(s"Error en búsqueda LDAP: ${e.getMessage}")
        }
      }
    }
  }

  /**
   * Obtiene los grupos de un usuario
   */
  def getUserGroups(username: String): Future[Either[String, List[String]]] = {
    if (!ldapEnabled) {
      return Future.successful(Left("LDAP está deshabilitado"))
    }

    blocking {
      Future {
        Try {
          ldapPool match {
            case Some(pool) =>
              val connection = pool.getConnection
              try {
                val searchResults = connection.search(
                  groupSearchBase,
                  SearchScope.SUB,
                  s"(member=uid=$username,ou=people,$baseDn)"
                )

                val groups = searchResults.getSearchEntries.asScala
                  .map(_.getAttributeValue("cn"))
                  .filter(_ != null)
                  .toList

                Right(groups)
              } finally {
                pool.releaseConnection(connection)
              }
            case None =>
              Left("Conexión LDAP no disponible")
          }
        } match {
          case Success(result) => result
          case Failure(e) => Left(s"Error obteniendo grupos: ${e.getMessage}")
        }
      }
    }
  }

  /**
   * Cierra el pool de conexiones
   */
  def close(): Unit = {
    ldapPool.foreach(_.close())
  }
}
