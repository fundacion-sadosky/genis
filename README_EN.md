# GENis 5.1.12-pre-release

GENis is a software developed by [Fundación Dr. Manuel Sadosky](https://www.fundacionsadosky.org.ar) to compare genetic profiles from biological samples obtained at different crime or disaster scenes, thus linking events that occurred at different times and places, increasing the probability of identifying criminals, missing persons, or victims of disasters.

For a detailed explanation on how to install GENis and configure the necessary software, please refer to the [GENis installation manual](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf). The following summarizes the steps for basic configuration and shows how to run the system in development and production environments. The files referred to can be found under the */utils* directory.

To find out about the operation of the system, consult the [GENis user manual](https://github.com/fundacion-sadosky/genis/files/9739748/manual.pdf).

## Setting up a GENis runtime environment
GENis is developed in Scala, JRE 8 is required to run the application and JDK 8 and Sbt are required for further development.

### Other requirements
- PostgreSQL 9.4.4
- MongoDB 2.6
- OpenLDAP

### ldap configuration 

Reconfigure ldap by entering **genis.local** for the domain and organization name:

```
sudo dpkg-reconfigure slapd 
```
Load the initial configuration data:
```
ldapadd -x -D cn=admin,dc=genis,dc=local -H ldap://:389 -W -f X-GENIS-LDAPConfig_Base_FULL.ldif -v
```
Check data loading:
```
ldapsearch -x -b "dc=genis,dc=local" -H ldap://:389 -D "cn=admin,dc=genis,dc=local" -W "objectclass=*"

### Postgresql setup
Create a postgres user and GENis databases:
```
sudo adduser genissqladmin
sudo -u postgres createuser -d -e -S -R genissqladmin
sudo -u postgres psql -c "ALTER USER genissqladmin PASSWORD '********';"
sudo -u genissqladmin createdb -e genisdb
sudo -u genissqladmin createdb -e genislogdb 
```
### mongodb setup
Create the initial setup collections:
```
sh < "MongoSetup.sh"
```
### Initial system datai
After running the system, the data schema is already created and the initial data of the system must be loaded.
```
sudo -u genissqladmin psql -d genisdb -f dml.sql
```

## Running GENis in a development environment
### System parameter configuration
Copy the *application-dev-template.conf* file to *application-dev.conf*. Edit database connection and ldap parameters as appropriate and specify the export path for profiles and lims files. The file *logger-dev-template.xml* also can be copied to *logger-dev.xml* to reconfigure the logger under development.

### GENis execution
From the application root directory, run (not all parameters are always necessary, they are included for illustrative purposes):
```
sbt run --java-home /usr/lib/jvm/java-8-openjdk-amd64
-Xms512M -Xmx10g -Xss1M -XX:+CMSClassUnloadingEnabled
-Dconfig.file=./application-dev.conf 
-Dlogger.file=./logger-dev.xml 
-Dhttps.port=9443 -Dhttp.port=9000
```
In the browser enter http://localhost:9000/. 
If this is the first time running the application, you will be prompted to run the evolutions scripts to create the data schema. To shut down the application in the console press `Ctrl + C`.

## Distribute and run GENis in production
To generate a new version of GENis, update the version number in the *build.sbt* file, delete the *target* folder and run
```
sbt dist
```
A zip will be generated in the *target/universal* folder with everything needed to run the system in production.
To run GENis:
- unzip under */usr/share*.
- grant run permission to the bin/genis script 
    ````sudo chmod +x bin/genis```
- modify the system configuration. The ldap database connections are in */conf/storage.conf* and the lab data and file export paths in */conf/genis-misc.conf*.
- run the system:
```
sudo ./bin/genis -v 
-DapplyEvolutions.default=true
-DapplyDownEvolutions.default=true
-DapplyEvolutions.logDb=true
-DapplyDownEvolutions.logDb=true
-Dhttp.port=9000 -Dhttps.port=9443 
-Dconfig.file=/usr/share/genis/conf/application.
The RUNNING_PID file contains the process number to stop the system execution.
```
cat RUNNING_PID
sudo kill -9 pid
sudo rm –rf RUNNING_PID
```
To upgrade GENis step on the */usr/share/genis* folder with the latest version but previously make a backup of the configuration files under */conf* to reuse them in the latest version if they were not modified or use them as a reference to configure the latest version.

### Initial system user
GENis uses an authentication mechanism based on TOPT. 
During system setup the user '*setup*' is created, with password '*pass*' and secret for TOPT '*ETZK6M66LFH3PHIG*'. 
Use this account freely for development purposes but for production request a new administrator account at the login screen, then login with user '*setup*' to enable it and finally disable user '*setup*'.
If you have problems logging in, you may need to install the NTP service as described in the [GENis installation manual](https://github.com/fundacion-sadosky/genis/files/9739746/instalacion.pdf).
To get the password from the TOPT you can use https://gauth.apps.gbraad.nl/

## Utils
Under */utils* are the scripts with the latest versions of the system configuration data, tools for maintenance and sample data files for testing.
The *cleanDatabases.sh* script is used to delete transactional data, profiles, matches, pedigrees, notifications, etc., without affecting configuration data.
```
sudo sh cleanDatabases.sh
```
