#!/bin/bash

mongo pdgdb < MongoUpdate_matches_v3_to_v4.js

mongo pdgdb-unit-test < MongoUpdate_matches_v3_to_v4.js
