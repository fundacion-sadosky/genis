#!/bin/bash
db=$1

if [ -z "$db" ]; then
  db="pdgdb"
fi

mongo $db --eval "db.createCollection('profiles')"
mongo $db --eval "db.createCollection('matches')"
mongo $db --eval "db.createCollection('electropherograms')"
mongo $db --eval "db.createCollection('scenarios')"
mongo $db --eval "db.profiles.createIndex({categoryId:1})"
mongo $db --eval "db.createCollection('pedigrees')"
mongo $db --eval "db.createCollection('pedigreeMatches')"
mongo $db --eval "db.createCollection('pedigreeGenotypification')"
mongo $db --eval "db.createCollection('pedigreeScenarios')"
mongo $db --eval "db.createCollection('files')"
mongo $db --eval "db.createCollection('collapsingMatches')"
mongo $db --eval "db.createCollection('screeningMatches')"
