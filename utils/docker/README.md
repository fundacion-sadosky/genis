# Docker GENis

Con la finalidad de facilitar la instalación de los servicios requeridos por GENis utilizamos [Docker](https://www.docker.com/) creando contenedores para las aplicaciones Postgresql, LDAP y MongoDB. Definimos además un contenedor que corre un cliente web de MongoDB.
La configuración del entorno se encuentra en el archivo *docker-compose.yml* donde además se puede consultar la versión utilizada de cada aplicación. El funcionamiento de GENis utilizando los servicios con Docker y el procedimiento de instalación descripto a continuación se ha probado sobre Ubuntu 22.04.

### Instalación de Docker en Ubuntu 22.04
Se puede consultar el procedimiento de instalación de Docker en Ubuntu [aquí](https://docs.docker.com/engine/install/ubuntu/). 
A continuación se resumen los pasos:

```
# Desinstalar paquetes que pueden ser conflictivos para Instalar docker a partir de los repositorios oficiales
for pkg in docker.io docker-doc docker-compose podman-docker containerd runc; do sudo apt-get remove $pkg; done

# Agregar la clave GPG oficial de Docker
sudo apt-get update
sudo apt-get install ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Agregar los repositorios
echo \
  "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update

# Instalar
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Agregar el usuario genis-user al grupo docker y reiniciar el sistema 
sudo usermod -a -G docker genis-user
reboot 
```

### Creación de los contenedores:

En el archivo *docker-compose.yml* se puede inspeccionar la configuración de cada contenedor y en particular para instalaciones de producción se recomienda modificar los passwords utilizados. Durante la creación de los contenedores se ejecutan scripts de configuración que se encuentran en las carpetas que finalizan con *_init* y realizan las siguientes tareas: 

- En Postgresql se crea el usuario **genissqladmin** con password **genissqladminp** y las bases **genisdb** y **genislogdb** con owner **genissqladmin** (se recomienda modificar los passwords en instalaciones de producción) 
- En LDAP se crean la estructura inicial y el usuario de primer acceso **setup** 
- En MongoDB se crea la base de datos **pdgdb** con las colecciones necesarias

Suponiendo que ha descargado la carpeta docker en el directorio genis-user, los pasos para crear los contenedores son:

```
cd docker
# otorgar permiso de ejecución para los scripts de configuración inicial de los contenedores
chmod -R 775 mongo_init/ openldap_init/ pgsql_init
# crear los contenedores
docker compose up -d
```

Se puede chequear si los contenedores se encuentran corriendo, detenerlos e iniciarlos con los comandos:

```
docker compose ps
docker compose stop
docker compose start
```

Para la persistencia de los datos en el sistema anfitrión independientemente del ciclo de vida del contenedor se definen volúmenes. Se listan con el comando:

```
docker volume ls
```

### Chequeo de configuración inicial y consulta de datos de los contenedores

Para conectarse a los contenedores se puede utilizar el nombre de host, **localhost**, **127.0.0.1** o bien el nombre del contenedor si se ingresa el mapeo correspondiente en el archivo */etc/hosts*:

```
127.0.0.1 genis_ldap
127.0.0.1 genis_postgres
127.0.0.1 genis_mongo
127.0.0.1 genis_mongo-express
```

Una vez creados los contenedores se pueden consultar con aplicaciones cliente para revisar la correcta carga de los datos iniciales: 

- Para Postgresql se puede utilizar [DataGrip](https://www.jetbrains.com/datagrip/) o bien el cliente `psql` dentro del contenedor:

Ingresar al contenedor:

```
docker exec -it genis_postgres /bin/bash
```

Dentro del contenedor:

```
su - postgres
psql
# listado de bases de datos, se esperan genisdb y genislogdb
\l	
# listado de usuarios, se espera genissqladmin
\dg
# chequeo de configuración md5
select * from  pg_settings where name ilike '%encr%';
table pg_hba_file_rules ;	
# salida del cliente psql
\q	
```

Salir del contenedor con `CTRL+D`

- Para LDAP se puede utilizar [Apache Directory Studio](https://directory.apache.org/studio/) o ingresar al contenedor y utilizar el comando `ldapsearch`

Ingresar al contenedor:

```
docker exec -it genis_ldap /bin/bash
```

Dentro del contenedor:

```
# chequeo de datos de ldap
ldapsearch -x -b "dc=genis,dc=local" -H ldap://:1389 -D "cn=admin,dc=genis,dc=local" -W "objectclass=*"
```

Salir del contenedor con `CTRL+D`

- Para consultar MongoDB se puede utilizar el cliente web ingresando a *http://genis_mongo-express:8081/db/pdgdb* desde el browser y revisar que se hayan creado la base **pdgdb** con las colecciones correspondientes.

- Si hubiera algún error en la instalación se puede determinar la causa y eliminar los contenedores y volúmenes a fin de recrearlos correctamente con los comandos (por ejemplo para postgresql):

```
docker logs genis_postgres
docker container rm genis_postgres
docker volume rm docker_pgsql_data
```

### Ejecución de GENis en ambiente de producción

En una instalación con fines de producción se debe consultar el [manual de instalación de GENis](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf) para la correcta configuración de la cuenta de usuario del sistema y otros servicios necesarios como NTP el entorno de ejecución de Java 8. Se debe descargar el [último release de GENis](https://github.com/fundacion-sadosky/genis/releases/latest) desde el repositorio en formato zip, descomprimirlo bajo */usr/share* y otorgar permisos de ejecución a la aplicación.

```
unzip genis-5.1.9.zip
cd genis
chmod +x ./bin/genis
```

Adecuar los parámetros de conexión a los servicios editando el archivo *./conf/storage.conf*.

```
# LDAP 
ldap {
  default {
    url = "genis_ldap"
    port = 1389
    adminPassword="adminp"
    ...
  }
}

# Pgsql
db {
  default {
    url = "jdbc:postgresql://genis_postgres:5432/genisdb"
    user = "genissqladmin"
    password ="genissqladminp"
    ...
  }
  logDb {
    url = "jdbc:postgresql://genis_postgres:5432/genislogdb"
    user= "genissqladmin"
    password = "genissqladminp"
    ...
  }
}

# mongodb
mongodb {
  uri = "mongodb://genis_mongo:27017/pdgdb"
  ...
}
```

Ingresar los datos del laboratorio editando el archivo *./conf/genis_misc.conf*, por ejemplo:

```
...
laboratory {
  country = "AR"
  province = "C"
  code = "SHDG"
}
```

Correr la aplicación.

```
sudo ./bin/genis -v 
-DapplyEvolutions.default=true
-DapplyDownEvolutions.default=true
-DapplyEvolutions.logDb=true
-DapplyDownEvolutions.logDb=true
-Dhttp.port=9000 -Dhttps.port=9443 
-Dconfig.file=./conf/application.conf &
```

Cargar los datos iniciales del sistema y configurar los usuarios como se indica en secciones posteriores.

### Ejecución de GENis en ambiente de desarrollo

Para correr GENis en un entorno de desarrollo se precisa tener instalados Java 8(JDK), Sbt y nodejs.

```
sudo apt install openjdk-8-jdk
sudo apt install nodejs
```

```
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```

Se debe descargar el código fuente, crear el archivo *application-dev.conf* a partir de *application-dev-template.conf* y editarlo para apuntar a los servicios levantados con docker en forma similar a lo comentado en la sección previa para producción. Se puede correr la aplicación utilizando Sbt.

```       
sbt run -Xms512M -Xmx10g -Xss1M -XX:+CMSClassUnloadingEnabled -Dconfig.file=./application-dev.conf -Dlogger.file=./logger-dev.xml -Dhttps.port=9443 -Dhttp.port=9000
```

Cargar los datos iniciales del sistema y configurar los usuarios como se indica a continuación.

### Datos inciales de GENis

Al ingresar a la aplicación desde el browser por primera vez van a correr los scripts de evolutions que definen el modelo de datos. Para finalizar la instalación debemos cargar los datos iniciales de GENis y los datos propios de la región de instalación (si el país no es Argentina). Los archivos de datos iniciales de GENis y de información local se encuentran bajo la carpeta *utils* en el directorio raíz del código fuente de GENis. 
Se deben copiar los scripts al contenedor **genis_postgres** para luego ejecutarlos:


Copiar los scripts al contenedor:

```
chmod o+rx dml.sql AR.sql
docker cp dml.sql genis_postgres:/tmp
docker cp AR.sql genis_postgres:/tmp
docker exec -it genis_postgres /bin/bash
```

Ejecutar los scripts (el password del usuario **genissqladmin** por defecto es **genissqladminp**):

```
su - postgres
psql -U genissqladmin -d genisdb -f /tmp/dml.sql
psql -U genissqladmin -d genisdb -f /tmp/AR.sql
```

Salir del contenedor con `CTRL+D`

### Usuario inicial del sistema

Durante la configuración del sistema se crea el usuario **setup**, con password **pass** y secret para TOPT '*ETZK6M66LFH3PHIG*'.
Se puede utilizar libremente para propósitos de desarrollo pero en producción solicite una nueva cuenta de administrador en la pantalla de login, luego ingrese con el usuario **setup** para habilitarla y finalmente inactive el usuario **setup**.
Si tuviera problemas para ingresar al sistema puede que precise instalar el servicio NTP como se indica en el [manual de instalación de GENis](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf).
Para obtener el password a partir del TOPT puede utilizar https://gauth.apps.gbraad.nl/

### Resguardo y recuperación de las bases de datos

#### Resguardo:
Crear en los contenedores una carpeta llamada /backup

Generar un script de resguardo llamado backup.sh:

```
#!/bin/bash

# Set date variable in YYYYMMDD format
DATE=$(date +%Y%m%d)

# Resguardar el LDAP
docker exec genis_ldap sh -c 'ldapsearch -x -H ldap://localhost:1389 -D "cn=admin,dc=genis,dc=local" -w adminp -b "dc=genis,dc=local" -LLL' > /home/genis-user/backups/ldap_backup_${DATE}.ldif

# Resguardar las bases PostgreSQL 
docker exec -e PGPASSWORD=genissqladminp genis_postgres pg_dump -U genissqladmin -h postgres -d genisdb -F c -b -v -f /backups/genisdb_backup_${DATE}.dump
docker exec -e PGPASSWORD=genissqladminp genis_postgres pg_dump -U genissqladmin -h postgres -d genislogdb -F c -b -v -f /backups/genislogdb_backup_${DATE}.dump
docker cp genis_postgres:/backups/genisdb_backup_${DATE}.dump /home/genis-user/backups/
docker cp genis_postgres:/backups/genislogdb_backup_${DATE}.dump /home/genis-user/backups/

# Eliminar el archivo de backup dentro del contenedor mongo
docker exec genis_postgres rm -f /backups/genisdb_backup_${DATE}.dump
docker exec genis_postgres rm -f /backups/genislogdb_backup_${DATE}.dump

# Backup MongoDB
docker exec genis_mongo mongodump --db pdgdb --archive=/tmp/mongodump_${DATE}.gz  --gzip
docker cp genis_mongo:/tmp/mongodump_${DATE}.gz /home/genis-user/backups/mongodump_pdgdb_${DATE}.gz

# Eliminar el archivo de backup dentro del contenedor mongo
docker exec genis_mongo rm -f /tmp/mongodump_${DATE}.gz

```

#### Recuperación:
Generar un script de recuperación llamado restore.sh:
```

#!/bin/bash

# Uso: ./restore.sh <YYYYMMDD>
# Ejemplo: ./restore.sh 20230601
if [ -z "$1" ]; then
  echo "Usage: $0 <backup_date in YYYYMMDD>"
  exit 1
fi

DATE=$1

# Rutas a los archivos de backup
LDAP_BACKUP="/home/genis-user/backups/ldap_backup_${DATE}.ldif"
PG_DUMP_GENISDB="/home/genis-user/backups/genisdb_backup_${DATE}.dump"
PG_DUMP_GENISLOGDB="/home/genis-user/backups/genislogdb_backup_${DATE}.dump"
MONGO_ARCHIVE="/home/genis-user/backups/mongodump_pdgdb_${DATE}.gz"

# Recuperar LDAP
if [ -f "$LDAP_BACKUP" ]; then
  echo "Restoring LDAP data..."
  # Copio ldif al contenedor
  docker cp "$LDAP_BACKUP" genis_ldap:/tmp/ldap_restore.ldif
  # Ejecuto ldapadd dentro del contenedor
  docker exec -i genis_ldap sh -c 'ldapadd -x -H ldap://localhost:1389 -D "cn=admin,dc=genis,dc=local" -w adminp -f /tmp/ldap_restore.ldif'
else
  echo "Archivo de resguardo de LDAP no encontrado: $LDAP_BACKUP"
fi

# Función para drop y recrear una base de datos PostgreSQL
drop_and_recreate_db() {
  local dbname=$1
  echo "Dropping and recreating $dbname..."

  # Forzar desconexión de usuarios
  docker exec -e PGPASSWORD=genissqladminp genis_postgres psql -U genissqladmin -d postgres -c "REVOKE CONNECT ON DATABASE $dbname FROM public;"
  docker exec -e PGPASSWORD=genissqladminp genis_postgres psql -U genissqladmin -d postgres -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE datname='$dbname' AND pid
 <> pg_backend_pid();"

  # Dropear y crear base de datos
  docker exec -e PGPASSWORD=genissqladminp genis_postgres dropdb -U genissqladmin --if-exists "$dbname"
  docker exec -e PGPASSWORD=genissqladminp genis_postgres createdb -U genissqladmin "$dbname"
}

# Recuperación de las bases de datos PostgreSQL
if [ -f "$PG_DUMP_GENISDB" ]; then
  # Drop y recreate genisdb
  drop_and_recreate_db "genisdb"

  # Copiar dump
  docker cp "$PG_DUMP_GENISDB" genis_postgres:/backups/
  echo "Restaurando genisdb..."
  docker exec -e PGPASSWORD=genissqladminp genis_postgres pg_restore -U genissqladmin -h postgres -d genisdb /backups/$(basename "$PG_DUMP_GENISDB")
else
  echo "Archivo de resguardo de PostgreSQL no encontrado: $PG_DUMP_GENISDB"
fi

if [ -f "$PG_DUMP_GENISLOGDB" ]; then
  # Drop y recreate genislogdb
  drop_and_recreate_db "genislogdb"

  # Copiar dump
  docker cp "$PG_DUMP_GENISLOGDB" genis_postgres:/backups/
  echo "Restaurando genislogdb..."
  docker exec -e PGPASSWORD=genissqladminp genis_postgres pg_restore -U genissqladmin -h postgres -d genislogdb /backups/$(basename "$PG_DUMP_GENISLOGDB")
else
  echo "Archivo de resguardo de PostgreSQL no encontrado: $PG_DUMP_GENISLOGDB"
fi

# Recuoeración de MongoDB
if [ -f "$MONGO_ARCHIVE" ]; then
  echo "Restaurando MongoDB..."
  # Elimino las bases existentes
  docker exec genis_mongo mongo --eval 'db.dropDatabase()' --quiet  pdgdb

  # Copiar el archivo de respaldo a la ubicación temporal en el contenedor
  docker cp "$MONGO_ARCHIVE" genis_mongo:/tmp/mongodump_${DATE}.gz

  # Restaurar desde el archivo comprimido
  docker exec genis_mongo mongorestore --archive=/tmp/mongodump_${DATE}.gz --gzip

else
  echo "Archivo de resguardo de MongoDB no encontrado: $MONGO_ARCHIVE"
fi

echo "Proceso de recuperación completado."


```

### Otras utilidades y ejemplos 

Se incluyen dos scripts útiles para desarrollo que borran los contenidos de las tablas de perfiles y matches en las bases de datos, se pueden correr con:

```
docker exec -i genis_mongo sh < "utils/clean-mongo-db.sh"
docker exec -i genis_postgres sh < "utils/clean-pgsql-db.sh"
```

Si se precisara correr un archivo ldif en ldap, por ejemplo *file.ldiff*, se debe copiar el script al contenedor y ejecutarlo.

Copiar el script al contenedor:

```
chmod o+rx file.ldiff
docker cp file.ldiff genis_ldap:/tmp
docker exec -it genis_ldap /bin/bash
```

Ejecutar el script:

```
ldapadd -x -D cn=admin,dc=genis,dc=local -H ldap://:1389 -W -f /tmp/file.ldiff -v
```
se solicitará el password del usuario **admin** que se puede consultar en *docker-compose.yml* y por defecto es **adminp**.

Salir del contenedor con `CTRL+D`.


