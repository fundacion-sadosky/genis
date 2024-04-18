#!/bin/bash

USAGE="$(basename "$0") -d <destination path> [-h] -- takes a full genis backup where:
	-h show usage information
	-d destination path. Must be an existing directory in which to place backed up data.
"
while getopts ":d:" opt; do
	case $opt in
		d) DESTINATION_PATH=$OPTARG;;
		h) echo "$USAGE" && exit 0;;
		\?) echo "$USAGE" && exit 0;;
		:) echo "$USAGE" && exit 0;;
	esac
done

# Set and check global vaiable to use along the scripy. Abort execution if any is missing or invalid
function setAndCheckLocalVars {
  	RED='\033[0;31m'
	GREEN='\033[0;32m'
	NC='\033[0m'

	printf "${GREEN}Checking parameters and seting ENV VARS\n"


	DATE_STRING=`date -u +"%Y-%m-%dT%H%M%S"`

	if [ ! -d $DESTINATION_PATH ]; then
 		printf "${RED}\tDestination path $DESTINATION_PATH does not exist or is not a directory${NC}\n"
	    quit
	fi

	# set GENIS_CONF_DIR
	. /etc/default/genis

	BACKUP_NAME="${DATE_STRING}_backup_genis_v$GENIS_VERSION"
	BACKUP_DIR="$DESTINATION_PATH/$BACKUP_NAME"
	if [ -d $BACKUP_DIR ]; then
 		printf "\t${RED}$BACKUP_DIR alrready exist. Please remove it or use diferent destination path before continue${NC}\n"
	    quit
	fi
	if [[ -f "$BACKUP_DIR.tgz" || -f "$BACKUP_DIR.tar.gz" ]]; then
 		printf "\t${RED}$BACKUP_DIR.(tgz|tar.gz) already exist. Please remove it or use diferent destination path before continue${NC}\n"
	    quit
	fi

	mkdir $BACKUP_DIR
	chmod o+wx $BACKUP_DIR
	mkdir $BACKUP_DIR/ldap
	chmod o+wx $BACKUP_DIR/ldap
	if [ $? -ne 0 ] 
	then
 		printf "${RED}\tExecution aborted. Can't create directory $BACKUP_DIR.${NC}\n"
	    quit
	else
 		printf "${GREEN}\tBackup directory created $BACKUP_DIR${NC}\n"
	fi

}

function quit {
	printf "${RED}${NC}\n"
	printf "${RED}Script execution was aborted${NC}\n"
	printf "${RED}${NC}\n"
	printf "${RED}Debug info:${NC}\n"
	printf "${RED}\tDATE_STRING: $DATE_STRING${NC}\n"
	printf "${RED}\tDESTINATION_PATH: $DESTINATION_PATH${NC}\n"
	printf "${RED}\tGENIS_CONF_DIR: $GENIS_CONF_DIR${NC}\n"
	printf "${RED}\tGENIS_VERSION: $GENIS_VERSION${NC}\n"
	printf "${RED}\tBACKUP_NAME: $BACKUP_NAME${NC}\n"
	printf "${RED}\tBACKUP_DIR: $BACKUP_DIR${NC}\n"
	printf "${RED}\tGENIS_STATUS: $GENIS_STATUS${NC}\n"
	printf "${RED}\tPG_STATUS: $PG_STATUS${NC}\n"
	printf "${RED}\tMONGO_STATUS: $MONGO_STATUS${NC}\n"
	printf "${RED}\tLDAP_STATUS: $LDAP_STATUS${NC}\n"
	printf "${RED}\tNGINX_STATUS: $NGINX_STATUS${NC}\n"
	printf "${RED}\tPG_SRV_USR: $PG_SRV_USR${NC}\n"
	printf "${RED}\tMONGO_SRV_USR: $MONGO_SRV_USR${NC}\n"
	printf "${RED}\tLDAP_SRV_USR: $LDAP_SRV_USR${NC}\n"
	printf "${RED}\tPLAY_SRV_USR: $PLAY_SRV_USR${NC}\n"
	printf "${RED}\tNGINX_SRV_USR: $NGINX_SRV_USR${NC}\n"

	exit 1
}

# Verify if user is sudoer. If not abort execution
function checkPermissions {
	# Update the user's cached credentials, authenticating the user if necessary. So it will check if running user is sudoer
	printf "${GREEN}Checking user permissions...${NC}\n"		
	sudo -v
	if [ $? -ne 0 ] 
	then
		# Abort execution if user has not right permission
 		printf "${RED}\tExecution aborted. User must have sudoer permissions.${NC}\n"
	    quit
	else
 		printf "${GREEN}\tUser has right permissions.${NC}\n"
	fi

}

# Verify if services are down. If not abort execution
function checkServices {
	printf "${GREEN}Checking services ...${NC}\n"		

	sudo service genis status >> /dev/null
	GENIS_STATUS=$?
	if [ $GENIS_STATUS -ne 0 ] 
	then 
		printf "${GREEN}\tOk: genis is down${NC}\n"
	else			
		printf "${RED}\tFail: genis is running${NC}\n"
	fi	

	sudo service nginx status >> /dev/null
	NGINX_STATUS=$?
	if [ $NGINX_STATUS -ne 0 ] 
	then 
		printf "${GREEN}\tOk: nginx is down${NC}\n"
	else			
		printf "${RED}\tFail: nginx is running${NC}\n"
	fi	

	sudo service postgresql status >> /dev/null
	PG_STATUS=$?
	if [ $PG_STATUS -eq 0 ] 
	then 
		printf "${GREEN}\tOk: postgresql is running${NC}\n"
	else			
		printf "${RED}\tFail: postgresql is down${NC}\n"
	fi	

	sudo service mongod status >> /dev/null
	MONGO_STATUS=$?
	if [ $MONGO_STATUS -eq 0 ] 
	then 
		printf "${GREEN}\tOk: mongod is running${NC}\n"
	else			
		printf "${RED}\tFail: mongod is down${NC}\n"
	fi	

	sudo service slapd status >> /dev/null
	LDAP_STATUS=$?
	if [ $LDAP_STATUS -ne 0 ] 
	then 
		printf "${GREEN}\tOk: slapd is down${NC}\n"
	else			
		printf "${RED}\tFail: slapd is running${NC}\n"
	fi	

	if [[ $GENIS_STATUS -eq 0 || $NGINX_STATUS -eq 0 || $PG_STATUS -ne 0 || $MONGO_STATUS -ne 0 || $LDAP_STATUS -eq 0 ]] 
	then 
		printf "${RED}Aborting execution. Please stop/start required services and retry${NC}\n"
		quit		
	fi	

  }


function backupPostgreSQL {
	printf "${GREEN}Taking backup of postgres${NC}\n"

	printf "${GREEN}\tClosing active connections${NC}\n"
	sudo -u $PG_SRV_USR psql -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pid <> pg_backend_pid();" >> /dev/null

	sudo -u $PG_SRV_USR pg_dumpall --clean | gzip > "$BACKUP_DIR/postgres.sql.gz"
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem making postgres backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: postgres backup was successfully created${NC}\n"
	fi

}

function backupMongoDB {
	printf "${GREEN}Taking backup of mongo${NC}\n"

	mongodump --gzip --archive=$BACKUP_DIR/mongo.archive
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem making mongo backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: mongo backup was successfully created${NC}\n"
	fi
}

function backupOpenLdap {
	printf "${GREEN}Taking backup of Open LDAP${NC}\n"

	#sudo -u $LDAP_SRV_USR slapcat -n 0 | gzip > $BACKUP_DIR/ldap/config.ldif.gz
	#if [ $? != 0 ]; then
	#	printf "${RED}\tFail: There was a problem making Open LDAP config backup${NC}\n"
	#	quit;
	#else
	#	printf "${GREEN}\tOk: Open LDAP config backup was successfully created${NC}\n"
	#fi

	sudo -u $LDAP_SRV_USR slapcat -n 1 | gzip > $BACKUP_DIR/ldap/data.ldif.gz
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem making Open LDAP data backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: Open LDAP data backup was successfully created${NC}\n"
	fi
}

function backupConfigs {
	printf "${GREEN}Taking backup of Genis configurations${NC}\n"
	sudo -u $PLAY_SRV_USR tar -C $GENIS_CONF_DIR -cvzf $BACKUP_DIR/genis-conf.tar.gz .
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem making Genis configurations backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: Genis configurations backup was successfully created${NC}\n"
	fi
}

function bundleBackup {

	printf "${GREEN}Bundle backup into tar file${NC}\n"
	tar -C $BACKUP_DIR -cvf "$DESTINATION_PATH/$BACKUP_NAME.tar" .
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem making backup bundle${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: Backup is placed in $DESTINATION_PATH/$BACKUP_NAME.tar${NC}\n"
	fi
	rm -fr $BACKUP_DIR
}

setAndCheckLocalVars
checkPermissions
checkServices
backupPostgreSQL
backupMongoDB
backupOpenLdap
backupConfigs
bundleBackup 