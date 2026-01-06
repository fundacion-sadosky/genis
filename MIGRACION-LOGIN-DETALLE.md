# Migración del Sistema de Login - GENis 6.0

## 1. Análisis Comparativo

### Sistema Antiguo (app.viejo - Play 2.3, Scala 2.11)
```scala
// Authentication.scala (L62-92)
def login = Action.async(BodyParsers.parse.json) { request =>
  val input = request.body.validate[UserPassword]
  input.fold(
    errors => Future.successful(BadRequest(...)),
    userPassword => {
      val result = authService.authenticate(
        userPassword.userName.toLowerCase, 
        userPassword.password, 
        userPassword.otp
      )
      result map { userOpt =>
        userOpt.fold[Result](Results.NotFound)(user =>
          Ok(Json.toJson(user))
            .withHeaders("Date" -> new Date().toString())
            .withSession(("X-USER", userPassword.userName.toLowerCase))
        )
      }
    }
  )
}
```

**Características principales:**
- Requiere validación OTP (One-Time Password)
- Usa sesiones Play con headers de "X-USER"
- Requiere encriptación/desencriptación de claves privadas del usuario
- Valida contra LDAP directo
- Manejo de cache de usuarios

### Sistema Nuevo (app - Play 3.x, Scala 2.13)
```scala
def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
  val usernameOpt = (request.body \ "username").asOpt[String]
  val passwordOpt = (request.body \ "password").asOpt[String]
  
  (usernameOpt, passwordOpt) match {
    case (Some(username), Some(password)) =>
      authService.authenticate(username, password).map {
        case Right(token) => Ok(Json.obj("success" -> true, "token" -> token))
        case Left(error) => Unauthorized(Json.obj("success" -> false, "error" -> error))
      }
  }
}
```

**Características principales:**
- Basado en JWT tokens
- Más simple, sin OTP obligatorio (puede agregarse opcionalmente)
- Stateless, sin sesiones Play
- Autenticación LDAP mediante servicio dedicado
- Respuesta JSON estructurada

## 2. Cambios de Componentes

### Cambios en AuthService

| Aspecto | Antiguo | Nuevo | Acción |
|--------|--------|-------|--------|
| **Validación OTP** | Obligatoria | Opcional | Agregar campo OTP opcional en login |
| **Encriptación de claves** | Manual en AuthService | Delegada a AuthService | Mantener compatible |
| **Sesiones** | withSession() Play | JWT tokens | Usar headers Authorization |
| **Cache** | CacheService.set() | Redis/In-Memory | Usar cache existente si disponible |
| **Bind LDAP** | userRepository.bind() | ldapService.authenticate() | Usar LdapService |

### Cambios de Requests/Responses

**Antiguo:**
```json
{
  "userName": "usuario",
  "password": "pass123",
  "otp": "123456"
}
```

**Nuevo:**
```json
{
  "username": "usuario",
  "password": "pass123",
  "otp": "123456"  // opcional
}
```

**Response Antiguo:**
```json
{
  "user": {...},
  "cryptoCredentials": {...},
  "authenticatedPair": {...}
}
```

**Response Nuevo:**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "message": "Autenticación exitosa"
}
```

## 3. Plan de Migración (5 Fases)

### Fase 1: Extender AuthController con OTP y datos completos
**Status:** PRÓXIMO
**Duración:** 30 min

```scala
def login(): Action[JsValue] = Action.async(parse.json) { implicit request =>
  val username = (request.body \ "username").as[String]
  val password = (request.body \ "password").as[String]
  val otpOpt = (request.body \ "otp").asOpt[String]
  
  authService.authenticateFull(username, password, otpOpt).map {
    case Right((token, user)) => Ok(Json.obj(
      "success" -> true,
      "token" -> token,
      "user" -> Json.obj(
        "username" -> user.username,
        "email" -> user.email,
        "roles" -> user.roles
      )
    ))
    case Left(error) => Unauthorized(Json.obj("error" -> error))
  }
}
```

### Fase 2: Agregar validación JWT en AuthController
**Status:** TODO
**Duración:** 20 min

```scala
def validateToken(): Action[AnyContent] = Action.async { implicit request =>
  val tokenOpt = request.headers.get("Authorization")
    .map(_.replace("Bearer ", ""))
  
  tokenOpt match {
    case Some(token) =>
      authService.validateToken(token).map {
        case Right(claims) => Ok(Json.obj("valid" -> true, "user" -> claims.subject))
        case Left(_) => Unauthorized(Json.obj("valid" -> false))
      }
    case None => Unauthorized(Json.obj("valid" -> false, "error" -> "No token"))
  }
}
```

### Fase 3: Crear modelos de datos compatibles
**Status:** TODO
**Duración:** 20 min

```scala
// app/models/User.scala
case class UserLogin(username: String, password: String, otp: Option[String])
case class AuthResponse(success: Boolean, token: Option[String], error: Option[String])
case class TokenClaims(subject: String, email: String, roles: Seq[String])
```

### Fase 4: Integrar con UserRepository para roles/permisos
**Status:** TODO
**Duración:** 40 min

Adaptar `userRepository.get()` del sistema viejo:
```scala
// En AuthService.scala (nuevo)
def authenticateFull(username: String, password: String, otp: Option[String]): 
    Future[Either[String, (String, FullUser)]] = {
  
  ldapService.authenticate(username, password).flatMap { authResult =>
    if (authResult) {
      // Obtener datos completos del usuario desde BD existente
      userRepo.findByUsername(username).map { userOpt =>
        userOpt match {
          case Some(user) =>
            val token = authService.generateToken(username)
            Right((token, user))
          case None => Left("Usuario no encontrado")
        }
      }
    } else {
      Future.successful(Left("Credenciales inválidas"))
    }
  }
}
```

### Fase 5: Probar endpoints completamente
**Status:** TODO
**Duración:** 30 min

```bash
# Probar login
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario","password":"pass123"}'

# Probar validación
curl -X GET http://localhost:9000/api/auth/validate \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

## 4. Cambios en Dependencias

### Mantener compatible con código viejo
Código en `app.viejo/` sigue sin cambios, todos imports desde `security.*` siguen disponibles.

### Usar nuevos servicios
- `LdapService` (unboundid-ldapsdk 7.0.1)
- `AuthService` con JWT (auth0 java-jwt 4.4.0)
- Play 3.x JSON builders

### NO usar
- `slick.jdbc.PostgresProfile` (no disponible sin slick)
- `play.api.libs.iteratee` (deprecado en Play 3)
- `akka.actor.Actor` (usar Pekko)

## 5. Comandos para Compilar y Probar

```bash
# Compilar
cd /home/cdiaz/Descargas/genis
sbt clean compile

# Ejecutar tests
sbt test

# Correr servidor
sbt run

# Compilar distribución
sbt dist
```

## 6. Base de Datos Existente - Integración

### Tablas Esperadas (del esquema original)
```sql
-- Usuarios
SELECT * FROM users WHERE username = 'usuario';

-- Roles
SELECT * FROM user_roles WHERE user_id = ?;

-- Permisos
SELECT * FROM role_permissions WHERE role_id = ?;
```

### Adaptación para Scala 2.13 (sin Slick)
Usar JDBC directo o JdbcOps simple:

```scala
// app/repositories/UserRepository.scala
class UserRepository @Inject()(db: javax.sql.DataSource) {
  def findByUsername(username: String): Future[Option[User]] = Future {
    val conn = db.getConnection
    try {
      val stmt = conn.prepareStatement(
        "SELECT id, username, email, status FROM users WHERE username = ?"
      )
      stmt.setString(1, username)
      val rs = stmt.executeQuery()
      if (rs.next()) {
        Some(User(rs.getLong("id"), rs.getString("username"), ...))
      } else None
    } finally conn.close()
  }
}
```

## 7. Posibles Problemas y Soluciones

| Problema | Causa | Solución |
|----------|-------|----------|
| OTP no valida | Formato diferente | Usar código generador compatible |
| Encriptación incompatible | Diferentes algoritmos | Usar mismo cipher que antiguo sistema |
| BD no tiene datos | Migraciones no ejecutadas | Ejecutar evolutions |
| Token JWT no válido | Clave diferente | Usar misma SECRET_KEY |

## 8. Timeline

```
Hoy (2026-01-05):
- Fase 1: Extender AuthController ✓
- Fase 2: Validación JWT ✓
Mañana:
- Fase 3: Modelos de datos
- Fase 4: Integración UserRepository
- Fase 5: Testing completo
```

## Próximos Pasos

1. ✅ Compilación exitosa
2. ⏳ Fase 1: Extender respuesta login con datos completos
3. ⏳ Fase 2: Agregar endpoint /api/auth/validate con JWT
4. ⏳ Fase 3: Crear modelos de respuesta estructurados
5. ⏳ Fase 4: Conectar con UserRepository existente
6. ⏳ Fase 5: Pruebas end-to-end
