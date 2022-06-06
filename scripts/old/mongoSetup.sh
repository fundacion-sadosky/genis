#!/bin/bash
mongo pdgdb --eval "db.createCollection('profiles')"
mongo pdgdb --eval "db.createCollection('matches')"
mongo pdgdb --eval "db.createCollection('electropherograms')"
mongo pdgdb --eval "db.createCollection('scenarios')"
mongo pdgdb --eval "db.profiles.createIndex({categoryId:1})"
mongo pdgdb --eval "db.createCollection('pedigrees')"
mongo pdgdb --eval "db.createCollection('pedigreeMatches')"

# A partir de 3.1.1

mongo pdgdb --eval "db.createCollection('pedigreeScenarios')"
mongo pdgdb --eval "db.createCollection('pedigreeGenotypification')"

# A partir de 4.0.0
mongo pdgdb --eval "db.createCollection('files')" 
mongo pdgdb --eval "db.createCollection('collapsingMatches')"
mongo pdgdb --eval "db.createCollection('screeningMatches')"
