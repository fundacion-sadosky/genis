# Script para crear dbs y subir los documentos en JSON
# Primero exportarlos desde mongo
# Luego corregirlos para couch
# Ponerlos en esta misma carpeta
# Personalizar BASE_URL con tu usuario y contraseña de admin en couch
# Correr

#!/bin/bash

# List of JSON files
files=(
  "collapsingMatches.json"
  "pedigreeGenotypification.json"
  "electropherograms.json"
  "pedigreeMatches.json"
  "profiles.json"
  "files.json"
  "pedigreeScenarios.json"
  "scenarios.json"
  "matches.json"
  "pedigrees.json"
  "screeningMatches.json"
)

# Base URL for the database
BASE_URL="http://usuario:contraseña@localhost:5984"

# Loop through each file
for file in "${files[@]}"; do
  # Extract the database name, convert camel case to snake case, and ensure all lowercase
  db_name=$(echo "${file%.*}" | sed -E 's/([a-z])([A-Z])/\1_\2/g' | tr '[:upper:]' '[:lower:]')

  echo "Creating database: $db_name"
  curl -X PUT "$BASE_URL/$db_name"

  echo "Uploading data to $db_name"
  curl -X POST "$BASE_URL/$db_name/_bulk_docs" -H "Content-Type: application/json" -d "@$file"

  echo "Done with $db_name"
  echo "------------------------------------"
done
