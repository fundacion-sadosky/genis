# GENis 6.0 - Framework Moderno (Rama "de-cero")

## 🚀 Descripción

Esta rama (`de-cero`) representa la reescritura moderna de GENis, dejando atrás las tecnologías legacy.

### Stack Tecnológico
- **Framework**: Play Framework 3.0.0 (en lugar de 2.3)
- **Lenguaje**: Scala 2.13.12 (en lugar de 2.11)
- **Base de Datos**: PostgreSQL 14.9 (Contenedor Docker)
- **Cliente Local**: PostgreSQL 18.1 (psql)
- **Autenticación**: JWT + LDAP Moderno (UnboundID SDK)
- **Frontend**: HTML5 + Vanilla JS (sin dependencias legacy)

## 📁 Estructura del Proyecto (Limpieza 2026)

Se ha realizado una limpieza profunda para facilitar el desarrollo.
- **`app/`, `conf/`, `public/`**: Contienen **solo** el código nuevo y moderno.
- **`_LEGACY_BACKUP/`**: Contiene todo el código antiguo, scripts viejos y configuraciones anteriores. Si buscas algo viejo, está aquí.

## 🛠️ Configuración Rápida

Hemos preparado un script que autoconfigura el entorno (verifica Java, SBT, BD y crea el archivo `.env`).

```bash
chmod +x setup-moderno.sh
./setup-moderno.sh
```

### Configuración Manual (Referencia)

Si prefieres hacerlo manualmente, estas son las credenciales que usamos por defecto en desarrollo:

| Servicio | Host | Puerto | Usuario | Password | Base de Datos |
|---|---|---|---|---|---|
| **PostgreSQL** | localhost | **5455** | `genissqladmin` | `genissqladminp` | `genisdb` |
| **LDAP** | localhost | 389 | `cn=admin...` | `admin` | - |

## 🚀 Cómo Ejecutar

Debido a la coexistencia de configuraciones, **SIEMPRE** debes ejecutar el proyecto especificando el archivo de configuración moderno:

```bash
sbt -Dconfig.file=./conf/application-moderno.conf run
```

La aplicación estará disponible en: **http://localhost:9000/login**

## 🔐 Credenciales de Acceso

| Rol | Usuario | Password | TOTP (Google Auth) |
|---|---|---|---|
| **Setup** | `setup` | `pass` | `ETZK6M66LFH3PHIG` |

> El Token TOTP es fijo para desarrollo.

## 📡 Endpoints Principales

- `POST /api/v2/auth/login` - Login (retorna JWT)
- `GET /api/health` - Health Check de la app
- `GET /api/health/db` - Health Check de la BD

## 🧪 Notas para Desarrolladores

1. **Backup**: No elimines la carpeta `_LEGACY_BACKUP` a menos que estés 100% seguro.
2. **Migración**: El código se está migrando módulo por módulo desde el backup hacia `app/`.
3. **Frontend**: Las vistas están en `app/views/` usando Twirl y JavaScript nativo moderno.

---
*Última actualización: 6 de Enero 2026*
