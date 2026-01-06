#!/bin/bash

#Exit immediately if a command exits with a non-zero status
set -e

createuser -d -S -R genissqladmin
psql -c "ALTER USER genissqladmin PASSWORD 'genissqladminp';" 
createdb genisdb -O genissqladmin
createdb genislogdb -O genissqladmin
