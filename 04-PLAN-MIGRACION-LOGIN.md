# 📋 Plan de Migración del Login - GENis 6.0

## Fase 1: ✅ Marco de Trabajo Moderno (COMPLETADO)

Se ha creado un marco de trabajo limpio y moderno en la rama **`de-cero`** con:
- ✅ Autenticación JWT + LDAP (V2)
- ✅ Servicios REST
- ✅ PostgreSQL 14.9 (Docker) / 18.1 (Cliente)
- ✅ Scala 2.13.12 + Play 3.0.0
- ✅ Documentación completa

## Fase 2: ✅ Compilación y Verificación (COMPLETADO)

### 2.1 - Setup
Se completó el setup y la generación de entorno:
- ✅ Java 11+ verificado
- ✅ `.env` configurado
- ✅ Proyecto compilando exitosamente

### 2.2 - Comprobación
El servidor corre correctamente con:
```bash
sbt run -Dconfig.file=./conf/application-moderno.conf
```
Endpoints disponibles: `http://localhost:9000/login`

## Fase 3: ✅ Migración del Login (COMPLETADO)

Se migró la lógica de `Login.scala` (Legacy) a una arquitectura de servicios moderna:

- ✅ **LdapService.scala**: Implementado con UnboundID SDK.
- ✅ **AuthServiceV2.scala**: Gestión de JWT, TOTP y sesiones.
- ✅ **AuthControllerV2.scala**: Endpoints REST puros.
- ✅ **Frontend**: Login HTML5 + JS nativo (sin dependencias viejas).

## Fase 4: 🔄 Limpieza y Estrategia de Archivos (COMPLETADO)

Se optó por una estrategia de "Pizarra Limpia":
- ✅ Código legacy movido a `_LEGACY_BACKUP/`.
- ✅ Raíz del proyecto limpia solo con código Play 3.0.
- ✅ Configuración separada (`application-moderno.conf`).

## Fase 5: 🧪 Testing y Próximos Pasos (EN CURSO)

### 5.1 - Verificación Funcional
- [ ] Probar login con usuario `setup` en el navegador.
- [ ] Verificar persistencia del token JWT.
- [ ] Probar logout y expiración.

### 5.2 - Migración de Módulos Restantes
Ahora que el Login funciona, migrar gradualmente desde backup:
- [ ] Dashboard (Vista principal).
- [ ] Gestión de Laboratorios (CRUD).
- [ ] Integración completa con BD Legacy (si aplica).

### 5.3 - Conexión Real
- [ ] Configurar conexión a LDAP de producción (si existe).
- [ ] Apuntar BD a datos reales.

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
