# 🚀 Guía Rápida - GENis 6.0 (Rama de-cero)

## En 30 Segundos

Estás en una rama nueva llamada **`de-cero`** que contiene un marco de trabajo completamente moderno para GENis.

```bash
# Ver en qué rama estás
git branch
# * de-cero    ← Estás aquí
#   dev
#   main

# Ver todos los cambios
git log --oneline -5

# Ver qué archivos se crearon
git diff main --name-only
```

---

## ⚡ Comenzar Rápido

```bash
# 1. Ejecutar setup automático
./setup-moderno.sh

# 2. Compilar
sbt compile

# 3. Ejecutar
sbt run -Dconfig.file=./conf/application-moderno.conf

# 4. Abrir navegador
# http://localhost:9000/login

# 5. Probar login (otra terminal)
# Credenciales: setup / pass / OTP: 123456
curl -X POST http://localhost:9000/api/v2/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"setup","password":"pass","otp":"123456"}'
```

---

## 📚 Documentación

| Archivo | Qué Contiene |
|---------|-------------|
| `03-README-MODERNO.md` | Documentación oficial actualizada |
| `01-TENER_EN_CUENTA.md` | Notas sobre limpieza y backup (⚠️ Leer antes) |
| `setup-moderno.sh` | Script de automatización |
| `conf/application-moderno.conf` | Configuración |
| `build.sbt` | Configuración de compilación |

---

## 🏗️ Qué Cambió

✅ **Scala 2.13.12** (era 2.11.11)
✅ **Play 3.x** (era 2.3)
✅ **PostgreSQL 14.9** (Docker) y **18.1** (Cliente)
✅ **JWT + LDAP moderno** (era solo LDAP legacy)
✅ **SBT 1.9.7** (era 0.13.15)

---

## 📁 Archivos Nuevos Principales

```
✨ app/services/AuthServiceV2.scala     → JWT + LDAP Renovado
✨ app/services/LdapService.scala       → LDAP moderno (UnboundID)
✨ app/controllers/AuthControllerV2.scala → API de auth V2
✨ app/controllers/HealthController.scala → Health check
✨ conf/application-moderno.conf        → Configuración moderna
✨ build.sbt                            → Build actualizado
✨ setup-moderno.sh                     → Setup automático
```

---

## 🔧 Troubleshooting Rápido

### Error: "Java version not compatible"
```bash
# Verificar Java
java -version
# Necesita ser 11+

# Cambiar Java si es necesario
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

### Error: "Cannot connect to database"
```bash
# Verificar PostgreSQL
sudo systemctl status postgresql

# Iniciar si está apagado
sudo systemctl start postgresql

# Ejecutar setup
./setup-moderno.sh
```

### Error: "LDAP not available"
```bash
# Verificar LDAP
sudo systemctl status slapd

# Iniciar si está apagado
sudo systemctl start slapd
```

### Error de compilación Scala
```bash
# Limpiar todo
sbt clean

# Limpiar caché
rm -rf ~/.sbt ~/.ivy2

# Compilar de nuevo
sbt compile
```

---

## 🔑 Endpoints Disponibles

| Método | Ruta | Descripción |
|--------|------|------------|
| `POST` | `/api/auth/login` | Login con usuario/password |
| `POST` | `/api/auth/logout` | Cerrar sesión |
| `GET` | `/api/auth/validate` | Validar token JWT |
| `GET` | `/api/health` | Estado de la aplicación |
| `GET` | `/api/health/db` | Estado de BD |

---

## 📋 Configuración Mínima

En `.env`:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/genisdb
DATABASE_USER=genissqladmin
DATABASE_PASSWORD=genisadmin
JWT_SECRET=CAMBIAR_EN_PRODUCCION
```

---

## 🎯 Próxima Fase

1. ✅ Marco de trabajo moderno **← COMPLETADO**
2. ⏳ Compilar proyecto
3. ⏳ Migrar login original
4. ⏳ Integrar servicios

---

## 📊 Comandos Útiles

```bash
# Compilar sin ejecutar
sbt compile

# Ejecutar tests
sbt test

# Limpiar todo
sbt clean

# Generar distribución
sbt dist

# Ver dependencias
sbt dependencyTree

# Ejecutar consola Scala
sbt console

# Recargar en caliente (sbt run ya lo hace)
# Edita archivos y recarga navegador
```

---

## 🔗 Referencias

- [Play Framework 3 Docs](https://www.playframework.com/documentation/latest/Home)
- [Scala 3 Docs](https://docs.scala-lang.org/)
- [SBT Docs](https://www.scala-sbt.org/documentation.html)
- [PostgreSQL 18 Docs](https://www.postgresql.org/docs/18/)
- [FerretDB Docs](https://docs.ferretdb.io/)

---

## 💡 Tips

- **Hot reload**: La app recarga cambios automáticamente en dev
- **Database migrations**: Se aplican automáticamente on startup
- **JWT debugging**: Decodifica tokens en https://jwt.io
- **LDAP testing**: Usa `ldapsearch` para verificar conexiones

---

## ❓ Preguntas Frecuentes

**P: ¿Puedo usar el código original?**
A: Sí, puedes mergear código del branch `dev` cuando esté listo.

**P: ¿Se perderán los datos existentes?**
A: No, las migraciones preservan datos. La estructura es compatible.

**P: ¿Debo cambiar mi cliente?**
A: Sí, los endpoints están en `/api/` y usan JSON + JWT.

**P: ¿Puedo volver a la versión antigua?**
A: Sí, solo cambia de branch: `git checkout dev`

---

**Rama**: `de-cero`
**Estado**: ✅ Listo para compilar y probar
**Última actualización**: Enero 2026
