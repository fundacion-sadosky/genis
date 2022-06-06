#!/bin/bash

dbname="pdgdb"
username="pdg"
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

profilesCount=`psql -U $username -d $dbname -c 'SELECT COUNT(*) FROM "APP"."PROFILE_DATA"' -A -t`
mongoProfiles=`mongo pdgdb --eval "db.profiles.count()" --quiet`
echo "La cantidad de perfiles en postgresql es de: $profilesCount"
echo "La cantidad de perfiles en mongo es de: $mongoProfiles"
if [ $profilesCount != $mongoProfiles ]; then
	printf "${RED}Inconsistencia en cantidad de perfiles${NC}\n"
else
	printf "${GREEN}La cantidad de perfiles coincide${NC}\n"
fi

pedigreesCount=`psql -U $username -d $dbname -c 'SELECT COUNT(*) FROM "APP"."COURT_CASE"' -A -t`
mongoPedigrees=`mongo pdgdb --eval "db.pedigrees.count()" --quiet`
echo "La cantidad de pedigrees en postgresql es de: $pedigreesCount"
echo "La cantidad de pedigrees en mongo es de: $mongoPedigrees"
if [ $pedigreesCount != $mongoPedigrees ]; then
        printf "${RED}Inconsistencia en cantidad de pedigrees${NC}\n"
else
        printf "${GREEN}La cantidad de pedigrees coincide${NC}\n"
fi
