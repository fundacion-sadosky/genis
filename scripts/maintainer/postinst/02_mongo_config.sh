if [ $FISRT_INSTALL = true ] ; then
	echo "Mongo config..."

	echo "Enable mongo service at startup"
	systemctl enable mongod.service

	mongo genisgdb <<EOF
db.createCollection('profiles');
db.createCollection('matches');
db.createCollection('electropherograms');
db.createCollection('scenarios');
db.profiles.createIndex({categoryId:1});
db.createCollection('pedigrees');
db.createCollection('pedigreeMatches');
db.createCollection('pedigreeScenarios');
db.createCollection('pedigreeGenotypification');
db.createCollection('files');
EOF

fi


