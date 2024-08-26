# GENis 5.1.11

El software GENis es una herramienta informática desarrollada por la [Fundación Dr. Manuel Sadosky](https://www.fundacionsadosky.org.ar) que permite contrastar perfiles genéticos provenientes de muestras biológicas obtenidas en distintas escenas de crimen o de desastres, vinculando así eventos ocurridos en diferente tiempo y lugar, aumentando las probabilidades de individualización de delincuentes, personas desaparecidas o víctimas de siniestros.

Para una explicación detallada sobre como instalar GENis y configurar el software necesario consulte el [manual de instalación de GENis](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf). A continuación se resumen los pasos para una configuración básica y se indica como correr el sistema en entornos de desarrollo y producción. Los archivos a los que se haga referencia pueden encontrarse bajo el directorio */utils*.

Para informarse sobre el funcionamiento del sistema consulte el [manual de usuario de GENis](https://github.com/fundacion-sadosky/genis/files/9739748/manual.pdf).

## Configurar un entorno de ejecución de GENis

GENis está desarrollado en Scala, para correr la aplicación se requiere JRE 8 y para continuar su desarrollo JDK 8 y Sbt.

### Otros requerimientos
- PostgreSQL 9.4.4
- MongoDB 2.6
- OpenLDAP

### Configuración de ldap

Reconfigurar ldap ingresando **genis.local** para el nombre de dominio y de organización: 

```
sudo dpkg-reconfigure slapd 
```

Cargar los datos de configuración inicial:

```
ldapadd -x -D cn=admin,dc=genis,dc=local -H ldap://:389 -W -f X-GENIS-LDAPConfig_Base_FULL.ldif -v
```

Chequear la carga de los datos:
```
ldapsearch -x -b "dc=genis,dc=local" -H ldap://:389 -D "cn=admin,dc=genis,dc=local" -W "objectclass=*"
```

### Configuración de postgresql
Crear un usuario de postgres y las bases de datos de GENis:
```
sudo adduser genissqladmin
sudo -u postgres createuser -d -e -S -R genissqladmin
sudo -u postgres psql -c "ALTER USER genissqladmin PASSWORD '********';"
sudo -u genissqladmin createdb -e genisdb
sudo -u genissqladmin createdb -e genislogdb 
```
### Configuración de mongodb
Crear las colecciones de configuración inicial:
```
sh < "MongoSetup.sh"
```
### Datos inciales del sistema
Luego de correr el sistema el esquema de datos ya se encuentra creado y se deben cargar los datos iniciales del sistema y los datos particulares de la región.
```
sudo -u genissqladmin psql -d genisdb -f dml.sql
sudo -u genissqladmin psql -d genisdb -f locales/AR.sql
```
## Correr GENis en un entorno de desarrollo

### Adecuación de parámetros del sistema

Copiar el archivo *application-dev-template.conf* a *application-dev.conf*. Editar los parámetros de conexión a bases datos y ldap según corresponda y especificar la ruta de exportación de perfiles y archivos lims. El archivo *logger-dev-template.xml* también puede copiarse a *logger-dev.xml* para reconfigurar el logger en desarrollo.

### Ejecución de GENis

En el directorio raíz de la aplicación correr (no todos los parámetros son siempre necesarios, se incluyen en forma ilustrativa):
```
sbt run --java-home /usr/lib/jvm/java-8-openjdk-amd64
-Xms512M -Xmx10g -Xss1M -XX:+CMSClassUnloadingEnabled
-Dconfig.file=./application-dev.conf 
-Dlogger.file=./logger-dev.xml 
-Dhttps.port=9443 -Dhttp.port=9000
```

En el navegador ingresar a http://localhost:9000/. 
Si es la primera vez que corre la aplicación se le preguntará por la ejecución de los scripts de evolutions para crear el esquema de datos. Para detener la aplicación en la consola ingresar `Ctrl + C`

## Descargar, distribuir y correr GENis en producción
Puede descargar la última versión de GENis ingresando a la sección de releases. Para actualizar el sistema consulte [`UPGRADING.md`](https://github.com/fundacion-sadosky/genis/blob/main/UPGRADING.md).
Para generar una nueva versión de GENis actualizar el nro. de versión en el archivo *build.sbt*, borrar la carpeta *target* y correr

```
sbt dist
```

Se generará un zip en la carpeta *target/universal* con todo lo necesario para correr el sistema en producción.
Para correr GENis:
- descomprimir el zip bajo */usr/share*
- otorgar permiso de ejecución al script bin/genis 
    ```sudo chmod +x bin/genis```
- modificar los parámetros de configuración del sistema. Las conexiones a bases de datos ldap se encuentran en */conf/storage.conf* y los datos del laboratorio y rutas de exportación de archivos en */conf/genis-misc.conf*

- correr el sistema:
```
sudo ./bin/genis -v 
-DapplyEvolutions.default=true
-DapplyDownEvolutions.default=true
-DapplyEvolutions.logDb=true
-DapplyDownEvolutions.logDb=true
-Dhttp.port=9000 -Dhttps.port=9443 
-Dconfig.file=./conf/application.conf &
```
En el archivo RUNNING_PID se encuentra el nro. de proceso para detener la ejecución del sistema. 
```
cat RUNNING_PID
sudo kill -9 pid
sudo rm –rf RUNNING_PID
```
 
## Usuario inicial del sistema

GENis utiliza un mecanismo de autenticación basado en TOPT.
Durante la configuración del sistema se crea el usuario '*setup*', con password '*pass*' y secret para TOPT '*ETZK6M66LFH3PHIG*'.
Utilice ésta cuenta libremente para propósitos de desarrollo pero en producción solicite una nueva cuenta de administrador en la pantalla de login, luego ingrese con el usuario '*setup*' para habilitarla y finalmente inactive el usuario '*setup*'.
Si tuviera problemas para ingresar al sistema puede que precise instalar el servicio NTP como se indica en el [manual de instalación de GENis](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf).
Para obtener el password a partir del TOPT puede utilizar https://gauth.apps.gbraad.nl/

## Otras utilidades
Bajo */utils* se encuentran los scripts con las últimas versiones de los datos de configuración del sistema, utilidades para el mantenimiento y archivos con datos de ejemplo para para pruebas.
El script *cleanDatabases.sh* sirve para borrar datos transaccionales, de perfiles, matches, pedigrís, notificaciones, etc, sin afectar datos de configuración.
```
sudo sh cleanDatabases.sh
```
