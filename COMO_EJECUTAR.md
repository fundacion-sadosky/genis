# Cómo ejecutar GENis

## Opción 1: Usar el script automático (RECOMENDADO)

```bash
/home/cdiaz/Descargas/genis/run-genis.sh
```

Este script:
- Inicia los contenedores Docker (PostgreSQL, MongoDB, OpenLDAP)
- Espera a que PostgreSQL esté listo
- Inicia la aplicación GENis en puerto 9000

## Opción 2: Ejecución manual paso a paso

### 1. Iniciar los servicios Docker:
```bash
cd /home/cdiaz/Descargas/genis/utils/docker
sudo docker compose up -d
```

### 2. Esperar a que PostgreSQL esté listo:
```bash
sleep 10
```

### 3. Iniciar GENis:
```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

cd /home/cdiaz/Descargas/genis/target/universal/stage

./bin/genis \
  -Dconfig.file=/tmp/genis-docker.conf \
  -DapplyEvolutions.default=true \
  -DapplyEvolutions.logDb=true \
  -Dplay.server.provider=play.core.server.NettyServerProvider \
  -Dplay.server.netty.transport=jvm \
  -Dhttp.port=9000
```

## Opción 3: Ejecutar en segundo plano

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

cd /home/cdiaz/Descargas/genis/target/universal/stage

nohup ./bin/genis \
  -Dconfig.file=/tmp/genis-docker.conf \
  -DapplyEvolutions.default=true \
  -DapplyEvolutions.logDb=true \
  -Dplay.server.provider=play.core.server.NettyServerProvider \
  -Dplay.server.netty.transport=jvm \
  -Dhttp.port=9000 > /tmp/genis.log 2>&1 &
```

## Acceder a GENis

Una vez que inicie, accede en tu navegador a:

**http://localhost:9000**

### Credenciales por defecto:

- **Usuario:** setup
- **Contraseña:** (se configura en LDAP)

## Monitoreo

### Ver logs de GENis:
```bash
tail -f /tmp/genis.log
```

### Ver estado de Docker:
```bash
cd /home/cdiaz/Descargas/genis/utils/docker
sudo docker compose ps
```

### Ver logs de PostgreSQL:
```bash
sudo docker compose logs postgres
```

### Ver logs de MongoDB:
```bash
sudo docker compose logs mongo
```

## Detener GENis

```bash
# Si ejecutaste en segundo plano:
pkill -f "bin/genis"

# Para detener los contenedores Docker:
cd /home/cdiaz/Descargas/genis/utils/docker
sudo docker compose down
```

## Reiniciar

```bash
# Reiniciar solo Docker:
cd /home/cdiaz/Descargas/genis/utils/docker
sudo docker compose restart

# O ejecutar el script nuevamente:
/home/cdiaz/Descargas/genis/run-genis.sh
```

## Puertos en uso

- **GENis (Play Framework):** 9000
- **PostgreSQL:** 5432
- **MongoDB:** 27017
- **Mongo Express (Web UI):** 8081
- **OpenLDAP:** 1389 (LDAP), 1636 (LDAPS)

## Interfaces web disponibles

- **GENis:** http://localhost:9000
- **Mongo Express:** http://localhost:8081

## Variables de configuración

Si necesitas cambiar la configuración, edita:
- `/tmp/genis-docker.conf` - Configuración de conectividad
- `/home/cdiaz/Descargas/genis/conf/storage.conf` - Configuración de almacenamiento
- `/home/cdiaz/Descargas/genis/utils/docker/docker-compose.yml` - Configuración de Docker
