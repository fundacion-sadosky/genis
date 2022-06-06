#!/bin/bash

USAGE="$(basename "$0") -f <backup bundle> [-h] -- restores a full genis backup where:
	-h show usage information
	-f full path of backup bundle.
"
while getopts ":f:" opt; do
	case $opt in
		f) BACKUP_FILE=$OPTARG;;
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

	printf "${GREEN}Checking parameters and seting ENV VARS${NC}\n"


	if [ ! -f $BACKUP_FILE ]; then
 		printf "${RED}\tBackup bundle $BACKUP_FILE does not exist${NC}\n"
	    quit
	fi

	DATE_STRING=`date -u +"%Y-%m-%dT%H%M%S"`

	# set GENIS_CONF_DIR
	. /etc/default/genis

}

function quit {
	printf "${RED}${NC}\n"
	printf "${RED}Script execution was aborted${NC}\n"
	printf "${RED}${NC}\n"
	printf "${RED}Debug info:${NC}\n"
	printf "${RED}\tGENIS_CONF_DIR: $GENIS_CONF_DIR${NC}\n"
	printf "${RED}\tGENIS_VERSION: $GENIS_VERSION${NC}\n"
	printf "${RED}\tBACKUP_FILE: $BACKUP_FILE${NC}\n"
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


function restorePostgreSQL {
	printf "${GREEN}Restoring backup of postgres${NC}\n"

	printf "${GREEN}\tClosing active connections${NC}\n"
	sudo -u $PG_SRV_USR psql -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pid <> pg_backend_pid();" >> /dev/null

	tar -xOvf $BACKUP_FILE ./postgres.sql.gz | gunzip | sudo -u $PG_SRV_USR psql
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem resoring postgres backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: postgres backup was successfully restored${NC}\n"
	fi
}

function rstoreMongoDB {
	printf "${GREEN}Restoring backup of mongo${NC}\n"

	tar -xOvf $BACKUP_FILE ./mongo.archive | mongorestore --stopOnError --drop --gzip --archive 
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem restoring mongo backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: mongo backup was successfully restored${NC}\n"
	fi
}

function restoreOpenLdap {
	printf "${GREEN}Restoring backup of Open LDAP${NC}\n"

	#sudo cp -r /etc/ldap/slapd.d "/etc/ldap/slapd.d_$DATE_STRING"
	#sudo -u $LDAP_SRV_USR rm -fr /etc/ldap/slapd.d/*
	#tar -xOvf $BACKUP_FILE ./ldap/config.ldif.gz | gunzip | sudo slapadd -n 0
	#if [ $? != 0 ]; then
	#	printf "${RED}\tFail: There was a problem restoring Open LDAP config${NC}\n"
	#	quit;
	#else
	#	printf "${GREEN}\tOk: Open LDAP config was successfully restored${NC}\n"
	#fi
	#sudo rm -fr "/etc/ldap/slapd.d_$DATE_STRING"

	sudo cp -r /var/lib/ldap "/var/lib/ldap_$DATE_STRING"
	sudo -u $LDAP_SRV_USR rm -fr /var/lib/ldap/*
	tar -xOvf $BACKUP_FILE ./ldap/data.ldif.gz | gunzip | sudo slapadd -n 1
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem restoring Open LDAP data${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: Open LDAP data was successfully restored${NC}\n"
	fi
	sudo rm -fr "/var/lib/ldap_$DATE_STRING"
}

function restoreConfigs {
	printf "${GREEN}Restoring backup of Genis configurations${NC}\n"

	sudo cp -r ${GENIS_CONF_DIR} ${GENIS_CONF_DIR}_${DATE_STRING}
	sudo -u $PLAY_SRV_USR rm -fr $GENIS_CONF_DIR/*
	tar -xOvf $BACKUP_FILE ./genis-conf.tar.gz | sudo -u $GENIS_SRV_USER tar -C $GENIS_CONF_DIR -xvz
	if [ $? != 0 ]; then
		printf "${RED}\tFail: There was a problem restoring Genis configurations backup${NC}\n"
		quit;
	else
		printf "${GREEN}\tOk: Genis configurations backup was successfully restored${NC}\n"
	fi
	sudo rm -fr ${GENIS_CONF_DIR}_${DATE_STRING}
}

setAndCheckLocalVars
checkServices
restorePostgreSQL
rstoreMongoDB
restoreOpenLdap
restoreConfigs
