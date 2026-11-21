#!/usr/bin/env bash
# Roles, esquemas y tablas. Cada microservicio tiene SU rol y SU esquema:
# catalogo_user no puede leer pedidos.* y pedidos_user no puede leer catalogo.*
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v cat_pwd="$CATALOGO_DB_PASSWORD" -v ped_pwd="$PEDIDOS_DB_PASSWORD" <<'SQL'
SELECT format('CREATE ROLE catalogo_user LOGIN PASSWORD %L', :'cat_pwd') \gexec
SELECT format('CREATE ROLE pedidos_user  LOGIN PASSWORD %L', :'ped_pwd') \gexec
-- pg_monitor permite consultar pg_stat_wal_receiver (chequeo de salud de las replicas)
GRANT pg_monitor TO catalogo_user, pedidos_user;

CREATE SCHEMA catalogo AUTHORIZATION catalogo_user;
CREATE SCHEMA pedidos  AUTHORIZATION pedidos_user;

SET SESSION AUTHORIZATION catalogo_user;
CREATE TABLE catalogo.productos (
  id     BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(120)  NOT NULL,
  precio NUMERIC(12,2) NOT NULL CHECK (precio > 0),
  stock  INTEGER       NOT NULL CHECK (stock >= 0)
);
INSERT INTO catalogo.productos (nombre, precio, stock) VALUES
  ('Laptop 14"',       899.90, 25),
  ('Mouse inalambrico', 19.50, 200),
  ('Teclado mecanico',  59.00, 80),
  ('Monitor 24"',      149.99, 40),
  ('Auriculares USB',   29.90, 120);
RESET SESSION AUTHORIZATION;

SET SESSION AUTHORIZATION pedidos_user;
CREATE TABLE pedidos.pedidos (
  id              BIGSERIAL PRIMARY KEY,
  producto_id     BIGINT        NOT NULL,
  cantidad        INTEGER       NOT NULL CHECK (cantidad > 0),
  precio_unitario NUMERIC(12,2) NOT NULL,
  total           NUMERIC(12,2) NOT NULL,
  estado          VARCHAR(20)   NOT NULL DEFAULT 'CREADO',
  creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
RESET SESSION AUTHORIZATION;
SQL
