#!/usr/bin/env bash
# Crea el rol de replicacion y habilita su acceso en pg_hba.conf (solo en el primer arranque).
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -v repl_password="$REPLICATION_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD %L', :'repl_password') \gexec
SQL
echo 'host replication replicator all scram-sha-256' >> "$PGDATA/pg_hba.conf"
