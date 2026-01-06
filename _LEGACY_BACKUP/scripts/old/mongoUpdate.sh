#!/bin/bash
mongo pdgdb --eval 'db.profiles.update({"processed": {"$exists":false}}, {"$set": {"processed": true}}, {"multi": true})'

mongo pdgdb-unit-test --eval 'db.profiles.update({"processed": {"$exists":false}}, {"$set": {"processed": true}}, {"multi": true})'

mongo pdgdb --eval 'db.matches.update({"type": {"$exists":false}}, {"$set": {"type": NumberInt(1)}}, {"multi": true})'

mongo pdgdb-unit-test --eval 'db.matches.update({"type": {"$exists":false}}, {"$set": {"type": NumberInt(1)}}, {"multi": true})'

mongo pdgdb < MongoUpdate_GenotypificationByType.js

mongo pdgdb-unit-test < MongoUpdate_GenotypificationByType.js

mongo pdgdb < MongoUpdate_Pedigrees.js

mongo pdgdb-unit-test < MongoUpdate_Pedigrees.js

mongo pdgdb --eval 'db.matches.update({"n":{"$exists":false}}, {"$set": {"n": NumberLong(db.profiles.count())}}, {"multi":true})'

mongo pdgdb-unit-test --eval 'db.matches.update({"n":{"$exists":false}}, {"$set": {"n": NumberLong(db.profiles.count())}}, {"multi":true})'


# A partir de 3.1.1

mongo pdgdb --eval 'db.pedigrees.update({"processed": {"$exists":false}}, {"$set": {"processed": true}}, {"multi": true})'

mongo pdgdb-unit-test --eval 'db.pedigrees.update({"processed": {"$exists":false}}, {"$set": {"processed": true}}, {"multi": true})'

mongo pdgdb --eval 'db.pedigreeMatches.update({"result": null}, {"$unset": {"result": 1}, "$set": {"profile.status": "discarded", "pedigree.status": "discarded"}}, {"multi": true})'

mongo pdgdb-unit-test --eval 'db.pedigreeMatches.update({"result": null}, {"$unset": {"result": 1}, "$set": {"profile.status": "discarded", "pedigree.status": "discarded"}}, {"multi": true})'

mongo pdgdb --eval 'db.pedigreeMatches.update({"result": {"$exists":false}}, {"$set": {"kind": "MissingInfo"}}, {multi: true})'

mongo pdgdb-unit-test --eval 'db.pedigreeMatches.update({"result": {"$exists":false}}, {"$set": {"kind": "MissingInfo"}}, {multi: true})'

mongo pdgdb --eval 'db.pedigreeMatches.update({"result": {"$exists":true}}, {"$set": {"kind": "DirectLink"}}, {multi: true})'

mongo pdgdb-unit-test --eval 'db.pedigreeMatches.update({"result": {"$exists":true}}, {"$set": {"kind": "DirectLink"}}, {multi: true})'

#cambiar assignee por el que corresponda
mongo pdgdb --eval 'db.pedigrees.update({"assignee": {"$exists":false}}, {"$set": {"assignee": "tst-admin"}}, {multi: true})'

mongo pdgdb-unit-test --eval 'db.pedigrees.update({"assignee": {"$exists":false}}, {"$set": {"assignee": "tst-admin"}}, {multi: true})'

mongo pdgdb-unit-test --eval 'db.matches.update({"result.leftPonderation":1}, {"$set": {"result.leftPonderation":1.0}}, {multi: true})'

mongo pdgdb-unit-test --eval 'db.matches.update({"result.rightPonderation":1}, {"$set": {"result.rightPonderation":1.0}}, {multi: true})'

#cambiar FrequencyTableName por la que esté configurada
mongo pdgdb --eval 'db.pedigrees.update({"frequencyTable": {"$exists":false}}, {"$set": {"frequencyTalbe": "FrequencyTableName"}}, {multi: true})'


# A partir de 3.2.3

mongo pdgdb < MongoUpdate_AnalysisDate.js

mongo pdgdb-unit-test < MongoUpdate_AnalysisDate.js