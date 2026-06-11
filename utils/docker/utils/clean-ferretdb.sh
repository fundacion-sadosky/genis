#!/bin/sh
# Borra el contenido de todas las colecciones de pdgdb en FerretDB.
# Uso: docker exec -i ferretdb_postgres sh < "utils/clean-ferretdb.sh"

PGPASSWORD=ferretp psql -U ferret -d postgres << 'EOSQL'
DO $$
DECLARE
  rec record;
BEGIN
  FOR rec IN
    SELECT collection_id, collection_name
    FROM documentdb_api_catalog.collections
    WHERE database_name = 'pdgdb'
      AND collection_name NOT LIKE 'system.%'
    ORDER BY collection_name
  LOOP
    RAISE NOTICE 'Limpiando colección: %', rec.collection_name;
    EXECUTE format('DELETE FROM documentdb_data.documents_%s', rec.collection_id);
  END LOOP;
END;
$$;
EOSQL
