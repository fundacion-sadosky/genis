package repositories

import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.DriverManager

/**
 * Caso de clase para representar un usuario
 */
case class User(
  id: Long,
  username: String,
  email: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  status: String,
  roles: Seq[String] = Seq()
)

/**
 * Repositorio para acceso a usuarios en base de datos PostgreSQL
 * Sin dependencias de Slick, usando JDBC directo
 */
@Singleton
class UserRepository @Inject()()(implicit ec: ExecutionContext) {

  // Configuración desde variables de entorno o defaults
  private val dbUrl = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://localhost:5432/genis")
  private val dbUser = sys.env.getOrElse("DB_USER", "postgres")
  private val dbPassword = sys.env.getOrElse("DB_PASSWORD", "postgres")

  /**
   * Obtiene un usuario activo por nombre de usuario
   */
  def findByUsername(username: String): Future[Option[User]] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val stmt = conn.prepareStatement(
        """SELECT id, username, email, first_name, last_name, status 
           FROM users WHERE username = ? AND status = 'ACTIVE'"""
      )
      stmt.setString(1, username)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val user = User(
          id = rs.getLong("id"),
          username = rs.getString("username"),
          email = Option(rs.getString("email")),
          firstName = Option(rs.getString("first_name")),
          lastName = Option(rs.getString("last_name")),
          status = rs.getString("status")
        )
        Some(user)
      } else {
        None
      }
    } catch {
      case e: Exception =>
        println(s"Error finding user: ${e.getMessage}")
        None
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene usuario por ID
   */
  def findById(userId: Long): Future[Option[User]] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val stmt = conn.prepareStatement(
        """SELECT id, username, email, first_name, last_name, status 
           FROM users WHERE id = ? AND status = 'ACTIVE'"""
      )
      stmt.setLong(1, userId)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val user = User(
          id = rs.getLong("id"),
          username = rs.getString("username"),
          email = Option(rs.getString("email")),
          firstName = Option(rs.getString("first_name")),
          lastName = Option(rs.getString("last_name")),
          status = rs.getString("status")
        )
        Some(user)
      } else {
        None
      }
    } catch {
      case e: Exception =>
        println(s"Error finding user by ID: ${e.getMessage}")
        None
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene los roles de un usuario
   */
  def getUserRoles(userId: Long): Future[Seq[String]] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val stmt = conn.prepareStatement(
        """SELECT r.name FROM roles r
           INNER JOIN user_roles ur ON r.id = ur.role_id
           WHERE ur.user_id = ?"""
      )
      stmt.setLong(1, userId)
      val rs = stmt.executeQuery()

      val roles = scala.collection.mutable.ArrayBuffer[String]()
      while (rs.next()) {
        roles += rs.getString("name")
      }
      roles.toSeq
    } catch {
      case e: Exception =>
        println(s"Error getting user roles: ${e.getMessage}")
        Seq()
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene las permisos de un usuario (a través de sus roles)
   */
  def getUserPermissions(userId: Long): Future[Seq[(String, String)]] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val stmt = conn.prepareStatement(
        """SELECT DISTINCT p.resource, p.action FROM permissions p
           INNER JOIN role_permissions rp ON p.id = rp.permission_id
           INNER JOIN roles r ON rp.role_id = r.id
           INNER JOIN user_roles ur ON r.id = ur.role_id
           WHERE ur.user_id = ?"""
      )
      stmt.setLong(1, userId)
      val rs = stmt.executeQuery()

      val permissions = scala.collection.mutable.ArrayBuffer[(String, String)]()
      while (rs.next()) {
        permissions += ((rs.getString("resource"), rs.getString("action")))
      }
      permissions.toSeq
    } catch {
      case e: Exception =>
        println(s"Error getting user permissions: ${e.getMessage}")
        Seq()
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene usuario completo con roles y permisos
   */
  def findFullUser(username: String): Future[Option[(User, Seq[String], Seq[(String, String)])]] = {
    findByUsername(username).flatMap {
      case Some(user) =>
        for {
          roles <- getUserRoles(user.id)
          permissions <- getUserPermissions(user.id)
        } yield Some((user.copy(roles = roles), roles, permissions))
      case None => Future.successful(None)
    }
  }

  /**
   * Crear usuario (para testing/admin)
   */
  def createUser(username: String, email: Option[String], firstName: Option[String], lastName: Option[String]): Future[Option[User]] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      val stmt = conn.prepareStatement(
        """INSERT INTO users (username, email, first_name, last_name, status) 
           VALUES (?, ?, ?, ?, 'ACTIVE') RETURNING id""",
        java.sql.Statement.RETURN_GENERATED_KEYS
      )
      stmt.setString(1, username)
      stmt.setString(2, email.orNull)
      stmt.setString(3, firstName.orNull)
      stmt.setString(4, lastName.orNull)
      
      stmt.executeUpdate()
      val rs = stmt.getGeneratedKeys()
      
      if (rs.next()) {
        val newUser = User(
          id = rs.getLong(1),
          username = username,
          email = email,
          firstName = firstName,
          lastName = lastName,
          status = "ACTIVE"
        )
        Some(newUser)
      } else {
        None
      }
    } catch {
      case e: Exception =>
        println(s"Error creating user: ${e.getMessage}")
        None
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Asignar rol a usuario
   */
  def assignRoleToUser(userId: Long, roleName: String): Future[Boolean] = Future {
    var conn: java.sql.Connection = null
    try {
      conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)
      
      // Obtener role ID
      val roleStmt = conn.prepareStatement("SELECT id FROM roles WHERE name = ?")
      roleStmt.setString(1, roleName)
      val roleRs = roleStmt.executeQuery()
      
      if (roleRs.next()) {
        val roleId = roleRs.getLong("id")
        
        // Asignar rol
        val assignStmt = conn.prepareStatement(
          "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?) ON CONFLICT DO NOTHING"
        )
        assignStmt.setLong(1, userId)
        assignStmt.setLong(2, roleId)
        assignStmt.executeUpdate()
        true
      } else {
        false
      }
    } catch {
      case e: Exception =>
        println(s"Error assigning role: ${e.getMessage}")
        false
    } finally {
      if (conn != null) conn.close()
    }
  }
}
