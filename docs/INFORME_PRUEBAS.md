# Informe de pruebas P01–P08

> Ejecute cada script, pegue la salida relevante en **Resultado obtenido**, adjunte la evidencia (carpeta `evidencias/`, capturas de Eureka y de `docker compose ps`) y redacte el **Análisis** con sus propias palabras: debe explicar errores transitorios, tiempos de detección y limitaciones.

**Entorno:** Docker Compose · PostgreSQL 17 (1 primario + 2 réplicas, replicación asíncrona) · Spring Boot 3.5.8 · Spring Cloud 2025.0.1 · fecha de ejecución: <!-- completar -->

## P01 · Descubrimiento dinámico

**Objetivo.** Verificar que las instancias se registran en Eureka y se localizan por nombre lógico.

**Procedimiento.** `.\scripts\p01-descubrimiento.ps1` y captura de http://localhost:8761

**Resultado esperado.** Eureka lista SERVICIO-CATALOGO (2 instancias), SERVICIO-PEDIDOS y API-GATEWAY en estado UP; las llamadas a `/api/*/info` responden por nombre lógico.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p01-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P02 · Balanceo HTTP

**Objetivo.** 100 solicitudes contra servicio-catalogo (2 instancias) registrando qué instancia responde.

**Procedimiento.** `.\scripts\p02-balanceo.ps1 -Total 100`

**Resultado esperado.** 100 respuestas 200, 0 errores y reparto ≈ 50/50 entre `catalogo-1` y `catalogo-2` (round-robin).

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p02-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P03 · Fallo de instancia

**Objetivo.** Detener una instancia y comprobar que el sistema sigue atendiendo por la otra.

**Procedimiento.** `.\scripts\p03-fallo-instancia.ps1` (detiene `catalogo-2` en t=10 s y la recupera en t=35 s)

**Resultado esperado.** Antes: reparto en dos instancias. Durante: solo `catalogo-1`, con una breve ventana de errores/latencia transitoria. Después: `catalogo-2` vuelve a recibir tráfico.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p03-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P04 · Comunicación entre servicios

**Objetivo.** Operación de negocio que requiere llamada real pedidos → catálogo por descubrimiento dinámico.

**Procedimiento.** `.\scripts\p04-comunicacion.ps1`

**Resultado esperado.** Pedido creado (201) con `catalogoInstancia` alternándose; el stock baja; 404 y 409 controlados; trazas `[CATALOGO]`.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p04-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P05 · Enrutamiento de base de datos

**Objetivo.** Escrituras al primario y lecturas ordinarias repartidas entre las dos réplicas.

**Procedimiento.** `.\scripts\p05-enrutamiento-bd.ps1`

**Resultado esperado.** Escritura con `nodoBd=primary`; 30 lecturas ≈ 50/50 entre `replica1` y `replica2`; logs `[DB]` con nodo y SQL; `pg_stat_activity` coherente.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p05-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P06 · Fallo de réplica PostgreSQL

**Objetivo.** Detener una réplica y comprobar que las consultas continúan por la restante.

**Procedimiento.** `.\scripts\p06-fallo-replica.ps1`

**Resultado esperado.** `replica1` Exited; 0 errores en 30 lecturas, todas en `replica2`; log `[DB-HEALTH] replica1 EXCLUIDA`.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p06-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P07 · Consistencia de lectura

**Objetivo.** Crear un registro y consultarlo de inmediato con una operación crítica dirigida al primario.

**Procedimiento.** `.\scripts\p07-consistencia.ps1` (parte B pausa el replay de WAL para forzar el desfase)

**Resultado esperado.** Lectura fuerte → 200 en `primary`. Con replay pausado, la lectura eventual da 404 en réplica mientras la fuerte da 200.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p07-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->

## P08 · Recuperación de réplica

**Objetivo.** Reiniciar la réplica detenida, comprobar su recuperación y su regreso a las consultas.

**Procedimiento.** `.\scripts\p08-recuperacion.ps1`

**Resultado esperado.** `replica1` pasa a saludable en pocos segundos; `pg_stat_replication` muestra 2 réplicas en `streaming`; las lecturas vuelven a repartirse entre ambas.

**Resultado obtenido.**

```text
(pegar aquí la salida del script)
```

**Evidencia.** `evidencias/p08-*.txt`, capturas: <!-- completar -->

**Análisis.** <!-- completar: ¿coincide con lo esperado? ¿errores transitorios? ¿tiempo de detección/recuperación? ¿limitaciones? -->
