#!/usr/bin/env bash
#
# Reabre un pedigree cerrado por error: pasa su STATUS de 'Closed' a 'Active'
# en la base Postgres legacy (genisdb), APP.PEDIGREE. Corre contra el
# contenedor docker "genis_postgres" (ver utils/docker/docker-compose.yml).
#
# No hay transición 'Closed' -> 'Active' habilitada en la app (es una regla
# de negocio intencional en PedigreeService.validTransition), por eso el
# fix es un UPDATE manual acá.
#
# Uso:
#   ./reopen_pedigree_postgres.sh --name "ALARCON_GARCIA"
#   ./reopen_pedigree_postgres.sh --id 123
#
# Después de correr este script, correr también reopen_pedigree_mongo.sh
# con el mismo ID para mantener consistente el genograma en Mongo.

set -euo pipefail

CONTAINER="${GENIS_PG_CONTAINER:-genis_postgres}"
DB="genisdb"
DBUSER="genissqladmin"
DBPASS="genissqladminp"

NAME=""
ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --name) NAME="${2:?falta el nombre}"; shift 2 ;;
    --id) ID="${2:?falta el id}"; shift 2 ;;
    *) echo "Argumento desconocido: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "$NAME" && -z "$ID" ]]; then
  echo "Uso: $0 --name <nombre_pedigree> | --id <id>" >&2
  exit 1
fi

if [[ -n "$ID" ]]; then
  WHERE="\"ID\" = ${ID}"
else
  WHERE="\"NAME\" ILIKE '%${NAME}%'"
fi

psql_exec() {
  docker exec -i -e PGPASSWORD="$DBPASS" "$CONTAINER" psql -U "$DBUSER" -d "$DB" -v ON_ERROR_STOP=1 "$@"
}

echo "== Pedigrees encontrados en ${DB} (WHERE ${WHERE}) =="
psql_exec -c "SELECT \"ID\", \"ID_COURT_CASE\", \"NAME\", \"STATUS\" FROM \"APP\".\"PEDIGREE\" WHERE ${WHERE};"

read -rp "¿Confirmás reabrir (STATUS 'Closed' -> 'Active') el/los pedigree(s) de arriba? [y/N] " CONFIRM
if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" ]]; then
  echo "Cancelado, no se modificó nada."
  exit 0
fi

psql_exec -c "UPDATE \"APP\".\"PEDIGREE\" SET \"STATUS\" = 'Active' WHERE ${WHERE} AND \"STATUS\" = 'Closed';"

echo "== Estado luego del update =="
psql_exec -c "SELECT \"ID\", \"ID_COURT_CASE\", \"NAME\", \"STATUS\" FROM \"APP\".\"PEDIGREE\" WHERE ${WHERE};"

echo
echo "Anotá el ID de arriba y corré: ./reopen_pedigree_mongo.sh --id <ID>"
