#!/usr/bin/env bash
# init_ferretdb.sh — Migrates pdgdb from genis_mongo (docker_mongo_data volume)
# into genis_ferretdb, preserving all collections, documents, and indexes.
#
# Prerequisites:
#   - genis_ferretdb must be running (port 27017)
#   - docker_mongo_data volume must exist
#   - genis_mongo must be stopped (the script does not touch it)
#
# Usage: ./init_ferretdb.sh [--dry-run]

set -euo pipefail

# ── configuration ────────────────────────────────────────────────────────────
MONGO_IMAGE="mongo:3.4.20"
TOOLS_IMAGE="mongodb/mongodb-community-server:7.0-ubuntu2204"
MONGO_VOLUME="docker_mongo_data"
SOURCE_DB="pdgdb"
DUMP_CONTAINER="mongo_ferretdb_dump_$$"

FERRET_HOST="localhost"
FERRET_PORT="27017"
FERRET_USER="ferret"
FERRET_PASS="ferretp"
FERRET_URI="mongodb://${FERRET_USER}:${FERRET_PASS}@${FERRET_HOST}:${FERRET_PORT}/?authSource=admin"

DUMP_DIR="$(mktemp -d /tmp/ferretdb_init_XXXXXX)"
DRY_RUN=false

# ── args ─────────────────────────────────────────────────────────────────────
for arg in "$@"; do
  [[ "$arg" == "--dry-run" ]] && DRY_RUN=true
done

# ── helpers ──────────────────────────────────────────────────────────────────
log()  { echo "[init_ferretdb] $*"; }
die()  { echo "[init_ferretdb] ERROR: $*" >&2; exit 1; }

cleanup() {
  log "Cleaning up..."
  docker rm -f "$DUMP_CONTAINER" 2>/dev/null || true
  rm -rf "$DUMP_DIR"
}
trap cleanup EXIT

# ── preflight ────────────────────────────────────────────────────────────────
log "Checking prerequisites..."

docker info > /dev/null 2>&1 || die "Docker is not running"

docker volume inspect "$MONGO_VOLUME" > /dev/null 2>&1 \
  || die "Volume '$MONGO_VOLUME' not found. Is genis_mongo's volume present?"

docker inspect genis_ferretdb --format '{{.State.Running}}' 2>/dev/null \
  | grep -q true || die "genis_ferretdb is not running"

nc -z "$FERRET_HOST" "$FERRET_PORT" 2>/dev/null \
  || die "Cannot reach FerretDB at ${FERRET_HOST}:${FERRET_PORT}"

if $DRY_RUN; then
  log "DRY RUN — no data will be written to FerretDB"
fi

# ── step 1: start source mongo with the data volume (no host port binding) ───
log "Starting temporary MongoDB container from volume '$MONGO_VOLUME'..."
docker run -d \
  --name "$DUMP_CONTAINER" \
  -v "${MONGO_VOLUME}:/data/db" \
  "$MONGO_IMAGE" \
  mongod --noauth > /dev/null

log "Waiting for temporary MongoDB to be ready..."
for i in $(seq 1 30); do
  if docker exec "$DUMP_CONTAINER" mongo --quiet --eval "db.adminCommand('ping')" admin \
       > /dev/null 2>&1; then
    break
  fi
  [[ $i -eq 30 ]] && die "Temporary MongoDB did not start in time"
  sleep 1
done

# ── step 2: verify source database exists ────────────────────────────────────
EXISTING_DBS=$(docker exec "$DUMP_CONTAINER" \
  mongo --quiet --eval \
    "db.adminCommand('listDatabases').databases.map(function(d){return d.name}).join(',')" \
  admin 2>/dev/null)

echo "$EXISTING_DBS" | grep -q "$SOURCE_DB" \
  || die "Database '$SOURCE_DB' not found in volume '$MONGO_VOLUME'. Found: $EXISTING_DBS"

# ── step 3: dump from source ─────────────────────────────────────────────────
log "Dumping '$SOURCE_DB' from source MongoDB..."
docker exec "$DUMP_CONTAINER" mongodump \
  --db "$SOURCE_DB" \
  --out "/tmp/mongodump" \
  --quiet

docker cp "${DUMP_CONTAINER}:/tmp/mongodump/${SOURCE_DB}" "$DUMP_DIR/"

COLLECTIONS=$(ls "$DUMP_DIR/$SOURCE_DB/"*.bson 2>/dev/null | xargs -I{} basename {} .bson | sort)
[[ -z "$COLLECTIONS" ]] && die "No collections found in dump"

log "Collections found: $(echo $COLLECTIONS | tr '\n' ' ')"

# ── step 4: report document counts ───────────────────────────────────────────
log "Document counts in source:"
for col in $COLLECTIONS; do
  count=$(docker exec "$DUMP_CONTAINER" mongo pdgdb --quiet \
    --eval "db['${col}'].count()" 2>/dev/null || echo "?")
  printf "  %-30s %s docs\n" "$col" "$count"
done

if $DRY_RUN; then
  log "DRY RUN complete — would import the collections above into FerretDB"
  exit 0
fi

# ── step 5: restore into FerretDB ────────────────────────────────────────────
# Uses mongodb-community-server 7.x which is compatible with FerretDB 2.x SCRAM auth.
# mongo:4.4 does NOT work — its SCRAM client is incompatible with FerretDB's salt length.
log "Restoring collections into FerretDB at ${FERRET_HOST}:${FERRET_PORT}..."

docker run --rm \
  --network host \
  -v "${DUMP_DIR}/${SOURCE_DB}:/dump" \
  "$TOOLS_IMAGE" \
  mongorestore \
    --uri "$FERRET_URI" \
    --db "$SOURCE_DB" \
    --drop \
    /dump

# ── step 6: verify ───────────────────────────────────────────────────────────
log "Verifying collections in FerretDB:"
docker run --rm \
  --network host \
  "$TOOLS_IMAGE" \
  mongosh --quiet \
    "mongodb://${FERRET_USER}:${FERRET_PASS}@${FERRET_HOST}:${FERRET_PORT}/${SOURCE_DB}?authSource=admin" \
    --eval "db.getCollectionNames().forEach(function(c){ print('  ' + c + ': ' + db[c].countDocuments() + ' docs') })" \
  2>/dev/null

log "Done. FerretDB '$SOURCE_DB' is ready."
