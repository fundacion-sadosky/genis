# 🎉 GENis 6.0 - Framework Moderno

## ✅ Estado: Listo para Desarrollo

El proyecto ha sido compilado exitosamente con un framework completamente moderno:

- ✅ **Framework**: Play 3.0.0 
- ✅ **Lenguaje**: Scala 2.13.12
- ✅ **BD**: PostgreSQL  
- ✅ **Autenticación**: JWT + LDAP
- ✅ **Frontend**: HTML5 + Vanilla JS (sin dependencias)

## 🚀 Ejecutar el Servidor

Para iniciar la aplicación con la configuración moderna y limpiar posibles bloqueos anteriores:

```bash
cd /home/cdiaz/Descargas/genis
rm -f target/universal/stage/RUNNING_PID && sbt -Dconfig.file=./conf/application-moderno.conf run
```

Luego abre en tu navegador:
- **Login**: http://localhost:9000/login

### 🔑 Credenciales por defecto

| Usuario | Password | TOTP Secret |
| :--- | :--- | :--- |
| `setup` | `pass` | `ETZK6M66LFH3PHIG` |

> **Nota para TOTP**: Puedes usar una app como Google Authenticator o Authy, o generar el código en [gauth.apps.gbraad.nl](https://gauth.apps.gbraad.nl/) usando el secret.

## 🔐 Interfaz de Login

### Características

✨ **Diseño Moderno**
- Gradientes purpura elegantes
- Interfaz responsiva (mobile-friendly)
- Animaciones suaves

🔒 **Seguridad**
- Validación de campos en cliente
- Tokens JWT en localStorage
- Headers de autorización

🌐 **Funcionalidad**
- Llamadas AJAX a `/api/v2/auth/login`
- Manejo de errores y mensajes
- Redirección automática tras login

## 📡 Endpoints Disponibles

### Autenticación V2 (Recomendado)
```bash
POST   /api/v2/auth/login        # Autenticarse
POST   /api/v2/auth/logout       # Cerrar sesión
GET    /api/v2/auth/validate     # Validar token
POST   /api/v2/auth/refresh      # Refrescar token
```

### Health Checks
```bash
GET    /api/health               # Estado general
GET    /api/health/db            # Estado BD
```

### Rutas Estáticas
```bash
GET    /                          # Página de inicio
GET    /login                     # Página de login
```

## 🧪 Pruebas

### Desde la Web
```
1. Abre http://localhost:9000/login
2. Ingresa credenciales
3. Haz clic en "Acceder"
4. El token se guardará automáticamente
```

### Desde cURL
```bash
# Login (usa credenciales setup/pass + TOTP generado)
# TOTP Secret: ETZK6M66LFH3PHIG
curl -X POST http://localhost:9000/api/v2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"setup","password":"pass","otp":"123456"}'

# Respuesta
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": "setup",
  "expiresAt": 1735939200
}
```

## 📁 Archivos Importantes

```
app/
├── controllers/
│   ├── AuthControllerV2.scala    ← Endpoints de autenticación
│   ├── HomeController.scala      ← Rutas estáticas
│   └── HealthController.scala    ← Health checks
├── views/
│   ├── login.scala.html         ← Página de login (HTML+JS)
│   └── index.scala.html         ← Página de inicio
├── services/
│   ├── AuthServiceV2.scala      ← Lógica de autenticación
│   └── LdapService.scala        ← Validación LDAP
└── repositories/
    └── UserRepository.scala     ← Acceso a BD

conf/
├── routes                       ← Definición de rutas
└── application.conf             ← Configuración
```

## 🛠️ Solución de Problemas

### Puerto 9000 ya en uso
```bash
# Matar procesos previos
pkill -9 sbt

# Usar puerto diferente
sbt "run -Dplay.server.http.port=8888"
```

### Limpiar caché de compilación
```bash
sbt clean
sbt compile
```

### Actualizar dependencias
```bash
sbt reload
sbt update
```

## 📚 Documentación

Para más detalles, ver:
- `MIGRACION-LOGIN-DETALLE.md` - Migración del código legacy
- `INTEGRACION-BD-EXISTENTE.md` - Integración con BD existente
- `QUICK-START.md` - Documentación completa

## 🎯 Próximos Pasos

1. **Verificar login en navegador**: http://localhost:9000/login
2. **Crear dashboard**: Nueva página tras autenticación
3. **Implementar módulos**: Agregar funcionalidad específica
4. **Conectar BD real**: Usar credenciales actuales de PostgreSQL

## 📞 Información Técnica

- **Play Framework Documentation**: https://www.playframework.com/documentation/3.0.x
- **Scala Documentation**: https://scala-lang.org/api/2.13.x/
- **JWT Tokens**: Auth0 java-jwt 4.4.0
- **LDAP**: UnboundID SDK 7.0.1

---

**Rama**: de-cero  
**Estado**: ✅ Compilación exitosa  
**Fecha**: 5 de enero de 2026
