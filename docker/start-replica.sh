#!/usr/bin/env bash
# Arranque de una replica fisica: copia base la primera vez; despues retoma el streaming.
set -euo pipefail
if [ ! -s "$PGDATA/PG_VERSION" ]; then
  mkdir -p "$PGDATA"
  chmod 700 "$PGDATA"
  echo 'Esperando al primario y creando copia base...'
  until pg_basebackup -h primary -p 5432 -U replicator -D "$PGDATA" -Fp -Xs -R -P; do
    echo 'Copia fallida; reintentando en 3 segundos...'
    rm -rf "${PGDATA:?}"/*
    sleep 3
  done
fi
# cluster_name identifica al nodo (SHOW cluster_name) y sera su application_name en pg_stat_replication
exec postgres -D "$PGDATA" -c hot_standby=on -c cluster_name="${CLUSTER_NAME:-replica}"
