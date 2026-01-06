# GENis 6.0 - Rama "de-cero" con Tecnologías Modernas

## 🚀 Descripción

Esta rama implementa un marco de trabajo moderno para GENis utilizando:
- **Scala 3.3.1** (en lugar de 2.11.11)
- **Play Framework 3.x** (en lugar de 2.3)
- **SBT 1.9.7** (en lugar de 0.13.15)
- **PostgreSQL 18.x** (en lugar de 9.4.4)
- **FerretDB** en lugar de MongoDB
- **OpenLDAP moderno** para autenticación
- **JWT** para tokens de autenticación
- **UnboundID SDK** para mejor soporte LDAP

## 📋 Requisitos del Sistema

### Versiones Requeridas
- **Java**: OpenJDK 11+ o 17+ (compatible con Scala 3)
- **SBT**: 1.9.7+
- **PostgreSQL**: 13+
- **FerretDB**: Última versión (compatible con MongoDB API)
- **OpenLDAP**: 2.4+

### Instalación de Dependencias

#### 1. PostgreSQL
```bash
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

#### 2. FerretDB (MongoDB-compatible)
```bash
# Opción 1: Docker
docker run -d -p 27017:27017 \
  -e FERRETDB_POSTGRESQL_URL="postgres://user:password@localhost/ferretdb" \
  ghcr.io/ferretdb/ferretdb:latest

# Opción 2: Desde binarios
# Descargar desde: https://github.com/FerretDB/FerretDB/releases
```

#### 3. OpenLDAP
```bash
sudo apt install slapd ldap-utils
sudo dpkg-reconfigure slapd
```

## 🔧 Configuración

### 1. Variables de Entorno

Crear archivo `.env` en la raíz del proyecto:

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/genisdb
DATABASE_USER=genissqladmin
DATABASE_PASSWORD=genisadmin

# Log Database
LOG_DATABASE_URL=jdbc:postgresql://localhost:5432/genislogdb
LOG_DATABASE_USER=genissqladmin
LOG_DATABASE_PASSWORD=genisadmin

# LDAP
LDAP_URL=ldap://localhost:389
LDAP_BASE_DN=dc=genis,dc=local
LDAP_ADMIN_DN=cn=admin,dc=genis,dc=local
LDAP_ADMIN_PASSWORD=admin

# JWT
JWT_SECRET=CAMBIAR_A_VALOR_SEGURO_128_CARACTERES_MINIMO
JWT_EXPIRATION=86400
```

### 2. Base de Datos PostgreSQL

```bash
# Conectar como postgres
sudo -u postgres psql

# Crear usuario
CREATE USER genissqladmin WITH PASSWORD 'genisadmin' CREATEDB;

# Crear bases de datos
CREATE DATABASE genisdb OWNER genissqladmin;
CREATE DATABASE genislogdb OWNER genissqladmin;

# Salir
\q
```

### 3. Configurar LDAP

```bash
# Verificar estado
sudo slapcat

# Cargar datos iniciales (si existen)
ldapadd -x -D cn=admin,dc=genis,dc=local -W -f utils/X-GENIS-LDAPConfig_Base_FULL.ldif
```

## 📁 Estructura del Proyecto

```
genis/
├── app/
│   ├── controllers/          # Controladores
│   │   └── AuthController.scala
│   ├── services/             # Servicios
│   │   ├── AuthService.scala
│   │   └── LdapService.scala
│   └── models/               # Modelos de datos
├── conf/
│   ├── application-moderno.conf  # Configuración moderna
│   └── routes                    # Rutas de API
├── build-moderno.sbt        # Build moderno
└── project/
    └── build.properties     # Versión de SBT
```

## 🏗️ Compilación

### Usando Java 11

```bash
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

### Compilar el Proyecto

```bash
# Limpiar compilación anterior
sbt clean

# Compilar
sbt compile

# Ejecutar tests
sbt test
```

## ▶️ Ejecución

### Entorno de Desarrollo

```bash
sbt run -Dconfig.file=./conf/application-moderno.conf
```

La aplicación estará disponible en: `http://localhost:9000`

### Endpoints de Autenticación

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "usuario",
  "password": "contraseña"
}

Response:
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Autenticación exitosa"
}
```

#### Validar Token
```bash
GET /api/auth/validate
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

Response:
{
  "valid": true,
  "user": {
    "userId": "usuario",
    "email": "usuario@genis.local",
    "cn": "Usuario Nombre"
  }
}
```

#### Logout
```bash
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 🔄 Migraciones de Base de Datos

Las migraciones usan Play Framework Evolutions automáticamente.

### Crear una nueva migración

Crear archivo en `conf/evolutions/default/`:

```sql
# !Ups
CREATE TABLE usuario (
  id SERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

# !Downs
DROP TABLE usuario;
```

## 🧪 Testing

```bash
sbt test
```

## 📚 Documentación

- [Play Framework 3 Documentation](https://www.playframework.com/documentation/latest/Home)
- [Scala 3 Documentation](https://docs.scala-lang.org/)
- [FerretDB Documentation](https://docs.ferretdb.io/)
- [UnboundID LDAP SDK](https://ldap.com/unboundid-ldap-sdk-for-java/)

## 🔐 Seguridad

- Los tokens JWT expiran en 24 horas
- Las contraseñas no se almacenan localmente (LDAP)
- HTTPS debe estar habilitado en producción
- JWT_SECRET debe cambiarse en producción

## 🤝 Próximos Pasos

1. ✅ Framework moderno configurado
2. ⏳ Migrar módulos del login del código original
3. ⏳ Integrar con modelos de datos existentes
4. ⏳ Migrar otros servicios del proyecto original
5. ⏳ Actualizar UI/Frontend

## 📝 Notas

- Esta rama está separada del código original para permitir desarrollo limpio
- Se pueden hacer merges selectivos del código original cuando esté listo
- Las dependencias se actualizan regularmente por seguridad

---

**Rama**: `de-cero`
**Versión**: 6.0.0.develop
**Última actualización**: Enero 2026
