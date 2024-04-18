#!/bin/bash

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

#Valida que el usuario sea root
if [ "$EUID" -ne 0 ]; then
        printf "${RED}Please run as root${NC}\n"
        exit
fi

#Define la ayuda para la usabilidad
USAGE="$(basename "$0") -F -w [-U] [-p] [-d] [-l] [-u] -- executes the automatic deploy
where:
	-p	genis path by default is /usr/share/genis
	-d	default postgresql database name by default is pdgdb
	-l	log postgresql database name by default is pdglogdb
	-u	postgresql database user by default is genissqladmin
	-w 	postgresql database password (required)
	-F	new version zip file path (required)
	-U	mongoUpdate.sh file path"

#Lectura de parametros
while getopts 'p:d:l:u:w:F:U:' option; do
	case "$option" in
		p) genisPath=${OPTARG};;
		d) postgresqlDb=${OPTARG};;
		l) postgresqlLogDb=${OPTARG};;
		u) postgresqlUser=${OPTARG};;
		w) postgresqlPassword=${OPTARG};;
		F) newVersion=${OPTARG};;
		C) checkDatabases=${OPTARG};;
		U) updateMongoDB=${OPTARG};;
		\?) echo "$USAGE"
		   exit 0;
		   ;;
	esac
done

#Validacion de parametros
if [ -z ${newVersion+x} ]; then
        echo "$USAGE"
	exit 0;
fi

if [ -z ${postgresqlPassword+x} ]; then
        echo "$USAGE"
        exit 0;
fi

if [ -z ${genisPath+x} ]; then
	genisPath="/usr/share/genis/"
fi

if [ -z ${postgresqlDb+x} ]; then
        postgresqlDb="pdgdb" 
fi

if [ -z ${postgresqlLogDb+x} ]; then
        postgresqlLogDb="pdglogdb" 
fi

if [ -z ${postgresqlUser+x} ]; then
        postgresqlUser="genissqladmin" 
fi

#Define contraseña de postgresql y fecha
export PGPASSWORD="$postgresqlPassword"
date=`date +%Y%m%d`

function runApp {
	cd $genisPath
	./bin/genis -v -DapplyEvolutions.default=true -DapplyDownEvolutions.default=true -DapplyEvolutions.logDb=true -DapplyDownEvolutions.logDb=true -Dhttp.port=9000 -Dhttps.port=9443 -Dconfig.file=/usr/share/genis/conf/application.conf &
}

#1.0 unzip de la nueva versión

cd
unzip $newVersion -d ./newGenis
if [ $? != 0 ]; then
	printf "${RED}A problem occured when the file was unzipping${NC}\n"
	exit 1;
fi
printf "${GREEN}New GENis version has been unzipped successfully${NC}\n"

#2.1 Matar el proceso
function killPID {
	pidFile='RUNNING_PID'
	runningPIDPath=$genisPath$pidFile
	if [ -e $runningPIDPath ]; then
        	pid=$(head -n 1 $runningPIDPath)
	        kill -9 $pid
	        rm $runningPIDPath
		printf "${GREEN}GENis has been stopped${NC}\n"
	else
		printf "GENis is not runnning\n"
	fi
}

killPID

#2.2 Backup de bases de datos

#PostgreSQL

cd

if [ -e backupPostgreSQL ]; then
	printf "backupPostgreSQL directory already exists\n"
else
	mkdir backupPostgreSQL
	printf "backupPostgreSQL directory was successfully created\n"
fi

cd backupPostgreSQL

pg_dump -U $postgresqlUser -h 127.0.0.1 -d $postgresqlDb > pdgdb_$date.sql
if [ $? != 0 ]; then
	printf "${RED}There was a problem making pdgdb backup${NC}\n"
	exit 1;
fi
printf "${GREEN}pdgdb backup was successfully created${NC}\n"

pg_dump -U $postgresqlUser -h 127.0.0.1 -d $postgresqlLogDb >  pdglogdb_$date.sql
if [ $? != 0 ]; then
	printf "${RED}There was a problem making pdglogdb backup${NC}\n"
	exit 1;
fi
printf "${GREEN}pdglogdb backup was successfully created${NC}\n"

#MongoDB

cd

mongodump -d pdgdb -o backupMongoDB
printf "${GREEN}mongo backup was successfully created${NC}\n"

#Define funcion para rollback

function rollback {
	cd
	if [ $1 == "Complete" ]; then
	        psql -U $postgresqlUser -h 127.0.0.1 -d $postgresqlDb < backupPostgreSQL/pdgdb_$date.sql
        	psql -U $postgresqlUser -h 127.0.0.1 -d $postgresqlLogDb < backupPostgreSQL/pdglogdb_$date.sql
		mongorestore -d pdgdb backupMongoDB/pdgdb

		killPID
	fi

	rm $genisPath -rf
	mv genisOld $genisPath
	runApp
	if [ $? == 0 ]; then
        	printf "${GREEN}The rollback was successfully executed${NC}\n"
        fi
        exit 0;
}

#2.3 Actualizar las bases de datos

#PostgreSQL

cd
evolutions='conf/evolutions/default'
rm newGenis/*/$evolutions/1.sql
cp $genisPath$evolutions/1.sql newGenis/*/$evolutions/

#MongoDB

if [ -z ${updateMongoDB+x} ]; then
        echo "No hay que actualizar mongo"
else
	cd
	chmod +x $updateMongoDB
	$updateMongoDB
	if [ $? != 0 ]; then
		printf "${RED}There was a problem making mongoDB upgrade${NC}\n"

		rollback ""

	        exit 1;
	fi
fi

#2.5 Archivo de configuración

conf='conf'
if [ -e $genisPath$conf ]; then
	cd
	cp $genisPath$conf/application.conf newGenis/*/$conf/
	printf "${GREEN}application.conf was successfully updated${NC}\n"
fi

#2.6 Levantar la app

cd
mv $genisPath  genisOld
rm $genisPath -rf
mv newGenis/* $genisPath
rm newGenis -rf

runApp

if [ $? != 0 ]; then
	printf "${RED}There was a problem executing the app${NC}\n"

	rollback "Complete"

	exit 1;
fi
cd
rm genisOld -rf
