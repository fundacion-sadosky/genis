#!/bin/bash

cd conf/evolutions/default

mongo pdgdb --eval "db.profiles.drop()"

mongo pdgdb --eval "db.matches.drop()"

mongo pdgdb --eval "db.electropherograms.drop()"

mongo pdgdb --eval "db.scenarios.drop()"

mongo pdgdb --eval "db.pedigrees.drop()"

mongoimport --db pdgdb --collection profiles --file 1.json 

mongoimport --db pdgdb --collection matches --file 2.json 

mongoimport --db pdgdb --collection electropherograms --file 3.json

mongo pdgdb-unit-test --eval "db.profiles.drop()"

mongo pdgdb-unit-test --eval "db.matches.drop()"

mongo pdgdb-unit-test --eval "db.electropherograms.drop()"

mongo pdgdb-unit-test --eval "db.scenarios.drop()"

mongo pdgdb-unit-test --eval "db.pedigrees.drop()"

mongoimport --db pdgdb-unit-test --collection profiles --file 1.json 

mongoimport --db pdgdb-unit-test --collection matches --file 2.json 

mongoimport --db pdgdb-unit-test --collection electropherograms --file 3.json

echo "script finalizado"
