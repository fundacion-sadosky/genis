<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://drive.usercontent.google.com/download?id=1C3S3zc3yGSdV4qJWuiqqzx75XCpeBWqI"  width="50%" >
  <img src="https://drive.usercontent.google.com/download?id=17uaYJPIi9NVJ9kgR9mGV5bm1JLb9otCt" alt=""  width="50%" >
</picture>



**GENis** es un sistema informático de código abierto para el **almacenamiento, gestión y comparación de perfiles genéticos con fines forenses**, desarrollado para apoyar la investigación judicial y la cooperación interinstitucional.

El sistema está diseñado para operar en contextos judiciales e institucionales, gestionando información genética sensible bajo criterios de **seguridad, trazabilidad y auditabilidad**, en concordancia con estándares y buenas prácticas del ámbito de la genética forense.

---

## Características principales

- Gestión y administración de perfiles genéticos humanos.
- Búsqueda y contraste automático entre perfiles almacenados.
- Gestión de coincidencias y de los resultados estadísticos asociados.
- Soporte para investigaciones judiciales y procesos de identificación genética.
- Capacidades de auditoría y trazabilidad de las acciones realizadas dentro del sistema.
- Arquitectura orientada a despliegues institucionales.
- Publicación como software de código abierto, auditable y adaptable.


## Tecnologías utilizadas

GENis está desarrollado sobre una arquitectura de software multicapa, utilizando tecnologías consolidadas en entornos institucionales:

- **Lenguaje**: Scala.
- **Plataforma**: JVM.
- **Framework backend**: Play Framework.
- **Interfaz de usuario**: aplicación web basada en AngularJS.
- **Bases de datos**:
  - PostgreSQL para información estructural, configuraciones y auditoría.
  - MongoDB para el almacenamiento y procesamiento de perfiles genéticos.
- **Seguridad**:
  - Autenticación de usuarios con doble factor.
  - Modelo de usuarios y roles.
  - Registro y auditoría de operaciones.
- **Criptografía y mecanismos de protección de datos**, según lo especificado en la documentación técnica.

Estas tecnologías y componentes se describen en detalle en la documentación técnica y en los manuales del sistema.



## Requerimientos generales de despliegue

GENis está diseñado para ser desplegado en **entornos institucionales controlados**, tales como laboratorios forenses u organismos judiciales. De manera general, el sistema requiere:

- Servidores dedicados o virtualizados bajo sistemas operativos GNU/Linux.
- Infraestructura de red confiable y controlada.
- Servicios de base de datos compatibles con PostgreSQL y MongoDB.
- Mecanismos de autenticación y gestión de usuarios acordes a entornos institucionales.
- Políticas de respaldo, auditoría y control de accesos.

Los requerimientos técnicos detallados, así como las configuraciones recomendadas y los procedimientos de instalación, se encuentran especificados en la documentación oficial del sistema.


## Documentación

La descripción completa del sistema, su arquitectura, funcionamiento, validaciones, procedimientos de instalación y modelos de operación se encuentra desarrollada en la documentación técnica del proyecto, incluyendo:

- Visión general y objetivos.
- Arquitectura del software.
- Manual de usuario.
- Procedimiento de instalación.
- Estándares, validaciones y lineamientos de seguridad.
- Modelos de soporte y gobernanza.


## Licencia

GENis se distribuye bajo la licencia **GNU Affero General Public License v3.0 (AGPL-3.0)**.

El texto completo de la licencia se encuentra disponible en el repositorio oficial del proyecto:  
https://github.com/fundacion-sadosky/genis/blob/main/LICENSE


## Instituciones participantes

GENis es el resultado de un trabajo conjunto entre instituciones del sistema judicial, la comunidad científica y académica, y organizaciones del sector tecnológico, en el marco de iniciativas de cooperación institucional orientadas a fortalecer las capacidades en genética forense.


## Estado del proyecto

GENis es un sistema con versiones estables utilizadas en entornos institucionales.
La versión documentada y desplegada institucionalmente de GENis es la **5.1.12**.  
El desarrollo y mantenimiento del software continúan de acuerdo con los lineamientos técnicos e institucionales definidos en su documentación oficial.

---

## Configurar un entorno de ejecución de GENis

GENis 5.1.12 está desarrollado en Scala, para correr la aplicación se requiere JRE 8 y para continuar su desarrollo JDK 8 y Sbt.

### Otros requerimientos para la version actual de GENis (5.1.12)
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
