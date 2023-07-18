# Docker GENis

Con la finalidad de facilitar la creación de un entorno de desarrollo para GENis utilizamos [Docker](https://www.docker.com/) creando contenedores para las aplicaciones Postgresql, LDAP y MongoDB. Definimos además un contenedor que corre un cliente web de MongoDB.
Los contenedores se definen en el archivo *docker-compose.yml*. Para crear los contenedores y configurarlos por primera vez se debe correr el comando:
```
docker-compose build && docker-compose up -d
```
Durante la creación de los contenedores se corren los scripts de configuración que se encuentran en las carpetas que finalizan con *_init*: 
- En Postgresql se crea el usuario **genissqladmin** con password **genissqladminp** y las bases **genisdb** y **genislogdb** con owner **genissqladmin** 
- En LDAP se crean la estructura inicial y el usuario de primer acceso **setup** 
- En MongoDB se crean las colecciones necesarias

Los contenedores persisten la información en las carpetas que finalizan con *_data* definidas como volúmenes de docker y en adelante se pueden ejecutar y detener utilizando `docker-compose start` y `docker-compose stop`.

### Consulta de datos de los contenedores

En el archivo *docker-compose.yml* se define un nombre para cada contenedor:**genis_postgres**, **genis_ldap** y **genis_mongo** y también los usuarios y passwords para cada uno de estos servicios. Para conectarse a los contenedores se puede utilizar el nombre de host, **localhost**, **127.0.0.1** o bien el nombre del contenedor si se ingresa el mapeo correspondiente en */etc/hosts*. 
Una vez creados los contenedores se pueden consultar con aplicaciones cliente: 
- Para Postgresql se puede utilizar [DataGrip](https://www.jetbrains.com/datagrip/) o bien el cliente `psql` dentro del contenedor, por ejemplo.

Ingresamos al contenedor:
```
docker exec -it genis_postgres /bin/bash
```
Dentro del contenedor:
```
su - postgres
psql
#lista las bases de datos
\l	
#lista los usuarios 
\dg 	
#sale del cliente psql
\q	
```
Salimos del contenedor con `CTRL+D`

- Para LDAP se puede utilizar [Apache Directory Studio](https://directory.apache.org/studio/) o ingresar al contenedor y utilizar el comando `ldapsearch`
- Para MongoDB utilizamos el cliente configurado ingresando a http://localhost:8081/db/pdgdb desde el browser.

### Ejecución de GENis
   
Para correr el sistema primero debemos editar el archivo *application-dev.conf* para apuntar a los servicios de docker:

```
# LDAP 
ldap {
  default {
    url = "genis_ldap"
    port = 1389
    adminPassword="adminp"
  }
}

# Pgsql
db {
  default {
    url = "jdbc:postgresql://genis_postgres:5432/genisdb"
    user = "genissqladmin"
    password ="genissqladminp"
  }
  logDb {
    url = "jdbc:postgresql://genis_postgres:5432/genislogdb"
    user= "genissqladmin"
    password = "genissqladminp"
  }
}

# mongodb
mongodb {
  uri = "mongodb://genis_mongo:27017/pdgdb"
}
```

Luego ejecutamos GENis con los comandos:
```       
export TERM=xterm-color    
sbt run --java-home /usr/lib/jvm/java-8-openjdk-amd64 -Xms512M -Xmx10g -Xss1M -XX:+CMSClassUnloadingEnabled -Dconfig.file=./application-dev.conf -Dlogger.file=./logger-dev.xml -Dhttps.port=9443 -Dhttp.port=9000
```
Al ingresar a la aplicación desde el browser por primera vez van a correr los scripts de evolutions que definen el modelo de datos y para finalizar la instalación debemos cargar los datos iniciales de GENis y los datos propios de la región de instalación. Los archivos de datos iniciales de GENis y de información local se encuentran bajo la carpeta *utils* en el directorio raíz de GENis. 
Copiamos el archivo *utils/dml.sql* al volúmen de datos e ingresamos al contenedor de Postgresql:

```
sudo cp utils/dml.sql pgsql_data/
docker exec -it genis_postgres /bin/bash
```
En el contenedor:
```
su - postgres
psql -U genissqladmin -d genisdb -f /var/lib/postgresql/data/dml.sql
```
Salimos del contenedor con `CTRL+D`

De forma similar debemos cargar la información particular de la región corriendo el archivo, por ejemplo, *utils/locales/AR.sql*. 

### Otras utilidades y ejemplos 

Incluímos dos scripts para limpiar las bases de datos, se pueden correr con:

```
docker exec -i genis_mongo sh < "clean-mongo-db.sh"
docker exec -i genis_postgres sh < "clean-pgsql-db.sh"
```

Si se precisara correr un archivo **LDIF**, por ejemplo *file.ldif*, en el contenedor de LDAP

```
cp file.ldif openldap_data/
docker exec -it genis_ldap /bin/bash
```
En el contenedor:
```
cd bitnami/openldap/
ldapadd -x -D cn=admin,dc=genis,dc=local -H ldap://:1389 -W -f file.ldiff -v
```
se solicitará el password que se puede consultar en *docker-compose.yml* y es **adminp**.

Salimos del contenedor con `CTRL+D`.


