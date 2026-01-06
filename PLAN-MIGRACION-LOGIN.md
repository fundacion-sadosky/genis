# 📋 Plan de Migración del Login - GENis 6.0

## Fase 1: ✅ Marco de Trabajo Moderno (COMPLETADO)

Se ha creado un marco de trabajo limpio y moderno en la rama **`de-cero`** con:
- ✅ Autenticación JWT + LDAP
- ✅ Servicios REST
- ✅ PostgreSQL 18.x
- ✅ Scala 3.3.1 + Play 3.x
- ✅ Documentación completa

## Fase 2: 📥 Compilación y Verificación (PRÓXIMO)

### 2.1 - Setup Automático
```bash
cd /home/cdiaz/Descargas/genis
./setup-moderno.sh
```

Este script:
- ✅ Verifica Java 11+
- ✅ Crea archivo `.env`
- ✅ Configura PostgreSQL
- ✅ Verifica LDAP
- ✅ Compila proyecto

### 2.2 - Compilación Manual
```bash
sbt clean
sbt compile
```

### 2.3 - Prueba de Ejecución
```bash
sbt run -Dconfig.file=./conf/application-moderno.conf
```

Acceder a: `http://localhost:9000/api/health`

## Fase 3: 🔄 Migración del Login Original

Una vez que el marco compile y ejecute correctamente:

### 3.1 - Analizar Código Original
```bash
git show dev:app/controllers/security/Login.scala
```

Identificar:
- ✓ Métodos de autenticación
- ✓ Validaciones LDAP
- ✓ Flujo de tokens (si existen)
- ✓ Modelos de usuario

### 3.2 - Adaptar a AuthService Moderno

Integrar lógica original en:
- `AuthService.scala` → Métodos de autenticación
- `LdapService.scala` → Búsquedas LDAP
- `AuthController.scala` → Endpoints

### 3.3 - Mantener Compatibilidad

Considerar:
- ✓ Migración de datos LDAP existentes
- ✓ Compatibilidad de contraseñas
- ✓ Mantenimiento de roles y permisos
- ✓ Auditoría de cambios

## Fase 4: 🧪 Testing

```bash
# Tests unitarios
sbt test

# Test de autenticación
curl -X POST http://localhost:9000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"usuario","password":"password"}'

# Validar token
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:9000/api/auth/validate
```

## Fase 5: 🔀 Integración Selectiva

Cuando el login funcione:

```bash
# Mergear código selectivamente desde dev
git merge dev --no-commit

# Resolver conflictos
git status
git add [archivos]

# Commit
git commit -m "feat: Integrar código existente compatible con 6.0"
```

## 🎯 Checklist de Migración

- [ ] Compilación exitosa
- [ ] Endpoints de auth funcionando
- [ ] JWT tokens generándose correctamente
- [ ] LDAP autenticando usuarios
- [ ] Tests pasando
- [ ] Código original analizado
- [ ] AuthService adaptado
- [ ] LdapService mejorado
- [ ] Compatibilidad verificada
- [ ] Documentación actualizada

## 📊 Comparación: Login Original vs Moderno

### LOGIN ORIGINAL
```scala
// Controlador antiguo
class LoginController {
  def authenticate(user: String, pass: String) = {
    // Llamada LDAP directa
    LdapUtils.authenticate(user, pass)
    // Response en HTML/JSON antiguo
  }
}
```

### LOGIN MODERNO (NUEVA RAMA)
```scala
// Servicio de autenticación
class AuthService {
  def authenticate(user: String, pass: String) = {
    ldapService.authenticate(user, pass).map { case Right(user) =>
      Right(generateJWT(user))
    }
  }
}

// Controlador REST
class AuthController {
  def login() = Action.async(parse.json) { req =>
    val (user, pass) = ...
    authService.authenticate(user, pass).map { 
      case Right(token) => Ok(Json.obj("token" -> token))
      case Left(err) => Unauthorized(Json.obj("error" -> err))
    }
  }
}
```

## ⚠️ Consideraciones Importantes

### Seguridad
- ✓ JWT con firma HMAC256
- ✓ Timeouts configurables
- ✓ Validación de tokens en cada request
- ✓ Contraseñas solo en LDAP (nunca almacenadas)

### Performance
- ✓ Pool de conexiones LDAP (10 conexiones)
- ✓ Pool HikariCP para BD (20 conexiones)
- ✓ Caché opcional de usuarios
- ✓ Lazy loading de datos

### Compatibilidad
- ✓ Mantener endpoints antiguos si es necesario
- ✓ Migración gradual de clientes
- ✓ Versionado de API (/api/v1/, /api/v2/)
- ✓ Logs de cambios de autenticación

## 📚 Archivos Relacionados

```
# Código original (para referencia)
git show dev:app/controllers/security/Login.scala
git show dev:app/services/LdapService.scala

# Código nuevo
cat app/services/AuthService.scala
cat app/services/LdapService.scala
cat app/controllers/AuthController.scala

# Configuración
cat conf/application-moderno.conf
cat conf/routes-moderno
```

## 🔗 Recursos

- Documentación completa: [README-MODERNO.md](README-MODERNO.md)
- Guía rápida: [QUICK-START.md](QUICK-START.md)
- Resumen técnico: [MARCO-TRABAJO-MODERNO.md](MARCO-TRABAJO-MODERNO.md)

## ✅ Recomendaciones

1. **Empezar con compilación**
   ```bash
   ./setup-moderno.sh && sbt compile
   ```

2. **Probar endpoints básicos**
   ```bash
   curl http://localhost:9000/api/health
   ```

3. **Luego migrar login**
   - Copiar lógica original
   - Adaptar a AuthService
   - Testear autenticación

4. **Finalmente integrar**
   - Mergear código compatible
   - Actualizar clientes
   - Deprecar endpoints antiguos

---

**Rama**: `de-cero`
**Estado**: Marco completo, listo para compilar
**Próximo paso**: `./setup-moderno.sh`
