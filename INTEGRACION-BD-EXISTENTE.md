# Integración con Base de Datos Existente

## 1. Estructura de Tablas Esperadas

### Tabla `users`
```sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255),
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Tabla `user_roles`
```sql
CREATE TABLE user_roles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  role_id BIGINT NOT NULL REFERENCES roles(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, role_id)
);
```

### Tabla `roles`
```sql
CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Tabla `role_permissions`
```sql
CREATE TABLE role_permissions (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL REFERENCES roles(id),
  permission_id BIGINT NOT NULL REFERENCES permissions(id),
  UNIQUE(role_id, permission_id)
);
```

### Tabla `permissions`
```sql
CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  resource VARCHAR(255) NOT NULL,
  action VARCHAR(255) NOT NULL,
  description TEXT,
  UNIQUE(resource, action)
);
```

## 2. Repositorio de Usuarios (Sin Slick)

Crear `app/repositories/UserRepository.scala`:

```scala
package repositories

import play.api.libs.json._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import java.sql.DriverManager
import javax.sql.DataSource

case class User(
  id: Long,
  username: String,
  email: Option[String],
  firstName: Option[String],
  lastName: Option[String],
  status: String,
  roles: Seq[String] = Seq()
)

@Singleton
class UserRepository @Inject()()(implicit ec: ExecutionContext) {

  // Configuración desde variables de entorno
  private val dbUrl = sys.env.getOrElse("DATABASE_URL", "jdbc:postgresql://localhost:5432/genis")
  private val dbUser = sys.env.getOrElse("DB_USER", "postgres")
  private val dbPassword = sys.env.getOrElse("DB_PASSWORD", "postgres")

  /**
   * Obtiene un usuario por nombre de usuario
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
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene las permisiones de un usuario (a través de sus roles)
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
    } finally {
      if (conn != null) conn.close()
    }
  }

  /**
   * Obtiene usuario con todos sus roles y permisos
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
}
```

## 3. Inyección de Dependencias

Actualizar `app/services/AuthServiceV2.scala`:

```scala
@Singleton
class AuthServiceV2 @Inject()(
    ldapService: LdapService,
    userRepository: UserRepository  // NUEVO
)(implicit ec: ExecutionContext) {
  
  /**
   * Autentica y obtiene datos completos del usuario
   */
  def authenticateWithDetails(username: String, password: String): 
      Future[Either[String, (String, JsObject)]] = {
    ldapService.authenticate(username, password).flatMap { isValid =>
      if (isValid) {
        // Obtener datos del usuario desde BD existente
        userRepository.findByUsername(username).map { userOpt =>
          userOpt match {
            case Some(user) =>
              val token = generateToken(username)
              val details = Json.obj(
                "id" -> user.id,
                "username" -> user.username,
                "email" -> user.email,
                "firstName" -> user.firstName,
                "lastName" -> user.lastName,
                "roles" -> user.roles,
                "status" -> user.status
              )
              Right((token, details))
            case None =>
              // Usuario no está en BD (solo existe en LDAP)
              val token = generateToken(username)
              val details = Json.obj(
                "username" -> username,
                "status" -> "LDAP_ONLY",
                "roles" -> Json.arr()
              )
              Right((token, details))
          }
        }
      } else {
        Future.successful(Left("Credenciales inválidas"))
      }
    }
  }
}
```

## 4. Filtro para Validar Tokens (Middleware)

Crear `app/filters/AuthFilter.scala`:

```scala
package filters

import play.api.mvc._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import javax.inject._
import services.AuthServiceV2

/**
 * Filtro para validar tokens JWT en endpoints protegidos
 */
@Singleton
class AuthFilter @Inject()(
    authService: AuthServiceV2
)(implicit ec: ExecutionContext) extends Filter {

  private val publicEndpoints = Set(
    "/api/auth/login",
    "/api/auth/logout",
    "/api/health",
    "/api/health/db"
  )

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    def apply(request: RequestHeader) = {
      if (publicEndpoints.contains(request.path)) {
        // Endpoint público, pasar sin validación
        nextFilter(request)
      } else {
        // Endpoint protegido, validar token
        val tokenOpt = request.headers.get("Authorization")
          .flatMap { authHeader =>
            val parts = authHeader.split(" ")
            if (parts.length == 2 && parts(0) == "Bearer") {
              Some(parts(1))
            } else {
              None
            }
          }

        tokenOpt match {
          case Some(token) =>
            authService.validateToken(token).flatMap {
              case Right(_) =>
                // Token válido, pasar al siguiente filtro
                nextFilter(request).map { result =>
                  result.withHeaders("X-Username" -> 
                    (authService.decodeToken(token) match {
                      case Right(claims) => (claims \ "username").as[String]
                      case Left(_) => ""
                    })
                  )
                }
              case Left(_) =>
                // Token inválido
                Future.successful(Results.Unauthorized(Json.obj(
                  "error" -> "Token inválido o expirado"
                )))
            }
          case None =>
            // Sin token
            Future.successful(Results.Unauthorized(Json.obj(
              "error" -> "Token requerido"
            )))
        }
      }
    }
  }
}
```

## 5. Registrar Filtro en Application

Crear `app/ApplicationModule.scala`:

```scala
package config

import play.api.inject.AbstractModule
import filters.AuthFilter
import repositories.UserRepository

class ApplicationModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[UserRepository]).to(classOf[UserRepository])
    bind(classOf[AuthFilter]).to(classOf[AuthFilter])
  }
}
```

En `conf/application.conf`:
```hocon
play.modules.enabled += "config.ApplicationModule"
play.filters.enabled += "filters.AuthFilter"
```

## 6. Configuración de BD

### Variables de Entorno
```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/genis"
export DB_USER="postgres"
export DB_PASSWORD="password"
export JWT_SECRET="your-secret-key-min-32-chars-long"
```

### En application.conf
```hocon
database {
  default {
    url = ${?DATABASE_URL}
    user = ${?DB_USER}
    password = ${?DB_PASSWORD}
    driver = "org.postgresql.Driver"
  }
}

jwt {
  secret = ${?JWT_SECRET}
  expirationHours = 24
}

ldap {
  url = "ldap://ldap.example.com:389"
  baseDn = "dc=example,dc=com"
  userSearchDn = "ou=users,dc=example,dc=com"
  bindDn = ${?LDAP_BIND_DN}
  bindPassword = ${?LDAP_BIND_PASSWORD}
}
```

## 7. Migraciones Play Evolutions

Crear `conf/evolutions/default/1.sql`:

```sql
# --- !Ups

CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255),
  first_name VARCHAR(255),
  last_name VARCHAR(255),
  status VARCHAR(50) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  role_id BIGINT NOT NULL REFERENCES roles(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(user_id, role_id)
);

CREATE TABLE permissions (
  id BIGSERIAL PRIMARY KEY,
  resource VARCHAR(255) NOT NULL,
  action VARCHAR(255) NOT NULL,
  description TEXT,
  UNIQUE(resource, action)
);

CREATE TABLE role_permissions (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL REFERENCES roles(id),
  permission_id BIGINT NOT NULL REFERENCES permissions(id),
  UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);

# --- !Downs

DROP INDEX IF EXISTS idx_role_permissions_role_id;
DROP INDEX IF EXISTS idx_user_roles_user_id;
DROP INDEX IF EXISTS idx_users_username;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
```

## 8. Testing de Integración

```bash
# Compilar
sbt compile

# Ejecutar servidor
sbt run

# Probar login
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}'

# Respuesta esperada
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "roles": ["admin"],
    "status": "ACTIVE"
  }
}

# Validar token
curl -X GET http://localhost:9000/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## 9. Checklist de Integración

- [ ] Crear tablas en PostgreSQL
- [ ] Insertar datos de prueba
- [ ] Implementar UserRepository
- [ ] Integrar con AuthServiceV2
- [ ] Registrar filtro de autenticación
- [ ] Probar login con BD existente
- [ ] Probar validación de tokens
- [ ] Validar permisos por roles
- [ ] Testing end-to-end
- [ ] Documentar usuarios/roles para sistema
