#!/usr/bin/env bash
#
# Contraparte Mongo de reopen_pedigree_postgres.sh: actualiza el campo
# "status" del documento del genograma en la colección "pedigrees" de la
# base "pdgdb", para el mismo pedigree cuyo STATUS ya se pasó a 'Active'
# en Postgres (APP.PEDIGREE). Corre contra el contenedor docker "genis_mongo"
# (ver utils/docker/docker-compose.yml).
#
# El "_id" del documento en Mongo es el mismo ID numérico de APP.PEDIGREE,
# pero guardado como string (ver app/pedigree/Pedigree.scala - longWrites).
#
# Uso:
#   ./reopen_pedigree_mongo.sh --id 123

set -euo pipefail

CONTAINER="${GENIS_MONGO_CONTAINER:-genis_mongo}"
DB="pdgdb"

ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --id) ID="${2:?falta el id}"; shift 2 ;;
    *) echo "Argumento desconocido: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "$ID" ]]; then
  echo "Uso: $0 --id <id_pedigree>" >&2
  exit 1
fi

echo "== Documento encontrado en pedigrees (_id='${ID}') =="
docker exec -i "$CONTAINER" mongo "$DB" --quiet --eval "printjson(db.pedigrees.findOne({_id: '${ID}'}, {_id:1, status:1, idCourtCase:1}))"

read -rp "¿Confirmás reabrir (status 'Closed' -> 'Active') este documento en Mongo? [y/N] " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Cancelado, no se modificó nada."
  exit 0
fi

docker exec -i "$CONTAINER" mongo "$DB" --quiet --eval "db.pedigrees.update({_id: '${ID}', status: 'Closed'}, {\$set: {status: 'Active'}})"

echo "== Documento luego del update =="
docker exec -i "$CONTAINER" mongo "$DB" --quiet --eval "printjson(db.pedigrees.findOne({_id: '${ID}'}, {_id:1, status:1, idCourtCase:1}))"