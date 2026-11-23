# Informe de pruebas P01–P08

Cada prueba se ejecutó con su script de `scripts/` sobre el sistema completo levantado con `docker compose up -d --build`. La salida completa de cada ejecución está en `evidencias/`. Abajo se transcribe solo la parte relevante.

**Entorno:** Docker Desktop (Windows 11, PowerShell 5.1) · PostgreSQL 17 (1 primario + 2 réplicas, replicación asíncrona por streaming de WAL) · Spring Boot 3.5.8 · Spring Cloud 2025.0.1 · fecha de ejecución: 22 y 23 de noviembre de 2026

**Parámetros que determinan los tiempos observados**

| Componente | Parámetro | Valor |
|---|---|---|
| Eureka (clientes) | renovación del *lease* / expiración | 2 s / 6 s |
| Eureka (clientes) | refresco del registro (`registry-fetch-interval-seconds`) | 3 s |
| Spring Cloud LoadBalancer | TTL de la caché de instancias | 3 s |
| Gateway | reintentos en GET (`retry`) / *timeout* de conexión | 2 / 1 s |
| db-routing | intervalo del monitor de salud de réplicas | 2 s |
| db-routing | chequeos correctos para reincorporar una réplica | 2 consecutivos |
| db-routing | *timeout* de conexión por nodo (HikariCP) | 1,5 s |

## P01 · Descubrimiento dinámico

**Objetivo.** Verificar que las instancias se registran en Eureka y se localizan por nombre lógico.

**Procedimiento.** `.\scripts\p01-descubrimiento.ps1`: consulta el registro de Eureka (`/eureka/apps`) y hace llamadas a `/api/catalogo/info` y `/api/pedidos/info` a través del Gateway, que enruta con `lb://servicio-catalogo` y `lb://servicio-pedidos`.

**Resultado esperado.** Eureka lista SERVICIO-CATALOGO (2 instancias), SERVICIO-PEDIDOS y API-GATEWAY en estado UP, y las llamadas a `/api/*/info` responden por nombre lógico.

**Resultado obtenido.**

```text
Servicio          InstanceId                   IP         Puerto Estado
--------          ----------                   --         ------ ------
SERVICIO-PEDIDOS  servicio-pedidos:pedidos-1   172.26.0.5   8080 UP
API-GATEWAY       api-gateway:local            172.26.0.3   8080 UP
SERVICIO-CATALOGO servicio-catalogo:catalogo-1 172.26.0.7   8080 UP
SERVICIO-CATALOGO servicio-catalogo:catalogo-2 172.26.0.6   8080 UP

GET /api/catalogo/info -> HTTP 200 servicio=servicio-catalogo instancia=catalogo-1 host=fe1fbc41aecb
GET /api/catalogo/info -> HTTP 200 servicio=servicio-catalogo instancia=catalogo-2 host=6e2debbb85bd
GET /api/catalogo/info -> HTTP 200 servicio=servicio-catalogo instancia=catalogo-1 host=fe1fbc41aecb
GET /api/catalogo/info -> HTTP 200 servicio=servicio-catalogo instancia=catalogo-2 host=6e2debbb85bd
GET /api/pedidos/info  -> HTTP 200 servicio=servicio-pedidos instancia=pedidos-1 host=7e7e4e0aa656
```

**Evidencia.** `evidencias/p01-descubrimiento.txt` y el panel de Eureka en http://localhost:8761 (se muestra en el video).

**Análisis.** El resultado coincide con lo esperado. Los cuatro registros están UP, y cada instancia de catálogo tiene un `instanceId` distinto (`catalogo-1`, `catalogo-2`) gracias a la variable `INSTANCE_ID`. El Gateway no conoce ninguna IP ni puerto: resuelve el nombre lógico contra Eureka, y las dos instancias de catálogo aparecen alternadas con contenedores (`host`) diferentes, lo que confirma que son procesos independientes. No hubo errores. Limitación menor: el Gateway se registra como `api-gateway:local` porque en `docker-compose.yml` no recibe `INSTANCE_ID`. No afecta al enrutamiento, pero sería más claro asignarle un identificador.

## P02 · Balanceo HTTP

**Objetivo.** Enviar 100 solicitudes contra servicio-catalogo (2 instancias) y registrar qué instancia responde a cada una.

**Procedimiento.** `.\scripts\p02-balanceo.ps1`: 100 solicitudes `GET /api/catalogo/info` por el Gateway. El campo `instancia` de cada respuesta se guarda en `evidencias/p02-balanceo.csv`.

**Resultado esperado.** 100 respuestas 200, 0 errores y un reparto cercano a 50/50 entre `catalogo-1` y `catalogo-2` (round-robin).

**Resultado obtenido.**

```text
Total de solicitudes : 100
Respuestas HTTP 200  : 100
Errores              : 0
Distribucion por instancia:
  catalogo-1       50  (50 %)
  catalogo-2       50  (50 %)
```

**Evidencia.** `evidencias/p02-balanceo.txt` (resumen) y `evidencias/p02-balanceo.csv` (instancia, código y latencia de cada solicitud).

**Análisis.** El reparto exacto 50/50 corresponde a la política por defecto de Spring Cloud LoadBalancer (`RoundRobinLoadBalancer`), que el Gateway aplica en las rutas `lb://`. El balanceo lo hace el Gateway por nombre lógico, no el cliente alternando URLs. No hubo errores. Este reparto perfecto se da con carga secuencial y con las dos instancias sanas. Con solicitudes concurrentes o con una instancia más lenta, round-robin seguiría repartiendo por turnos sin tener en cuenta la carga real de cada instancia.

## P03 · Fallo de instancia

**Objetivo.** Detener una instancia y comprobar que el sistema sigue atendiendo por la otra.

**Procedimiento.** `.\scripts\p03-fallo-instancia.ps1`: tráfico continuo durante 60 s (una solicitud cada ~0,3 s). Ejecuta `docker compose stop catalogo-2` en t=10 s y `docker compose start catalogo-2` en t=35 s.

**Resultado esperado.** Antes, reparto entre las dos instancias. Durante el fallo, solo responde `catalogo-1`, con una posible ventana breve de errores o de latencia. Después, `catalogo-2` vuelve a recibir tráfico.

**Resultado obtenido.**

```text
[ 10,0s] >>> docker compose stop catalogo-2
 10,0s durante  HTTP 200 catalogo-2  40 ms     <- sigue respondiendo mientras se apaga (~3 s)
 11,9s durante  HTTP 200 catalogo-1  40 ms
 15,1s durante  HTTP 200 catalogo-1  153 ms    <- a partir de aquí solo catalogo-1
 16,1s durante  HTTP 200 catalogo-1  626 ms
[ 35,1s] >>> docker compose start catalogo-2
 53,2s despues  HTTP 200 catalogo-1  26 ms
 53,5s despues  HTTP 200 catalogo-2  37 ms     <- catalogo-2 vuelve a atender

================ RESUMEN ================
--- antes : 23 solicitudes, errores = 0     catalogo-1 13 (57 %) | catalogo-2 10 (43 %)
--- durante : 63 solicitudes, errores = 0   catalogo-1 59 (94 %) | catalogo-2  4 (6 %)
--- despues : 70 solicitudes, errores = 0   catalogo-1 61 (87 %) | catalogo-2  9 (13 %)
Sin errores observados durante la prueba.
catalogo-2 vuelve a atender solicitudes en t=53.5s (a 18.5 s de arrancarla)
```

**Evidencia.** `evidencias/p03-fallo-instancia.txt` (traza segundo a segundo) y `evidencias/p03-fallo-instancia.csv`.

**Análisis.** El sistema siguió atendiendo sin interrupción: 156 solicitudes y 0 errores. Las 4 respuestas de `catalogo-2` contadas como "durante" corresponden a los ~3 s que tardó el contenedor en detenerse. Después solo respondió `catalogo-1`, y entre t=15 y t=17 s se ve un aumento transitorio de la latencia (hasta 626 ms) mientras el Gateway descartaba la instancia. No hubo errores por tres motivos: `docker compose stop` envía SIGTERM, así que Spring hace un apagado ordenado y se da de baja de Eureka; el TTL de 3 s de la caché del LoadBalancer deja de elegir pronto la instancia caída; y el filtro `retry` del Gateway reintenta un GET fallido contra otra instancia. **Tiempo de recuperación:** `catalogo-2` volvió a recibir tráfico 18,5 s después de arrancarla, que es lo que tarda Spring Boot en iniciar, pasar el *health check* y aparecer en el registro que consulta el Gateway. **Limitaciones:** con una caída abrupta (`docker compose kill`, o un fallo de red) no habría baja ordenada en Eureka, y la instancia seguiría listada hasta que expire su *lease* (6 s). En ese caso se esperan algunos errores o *timeouts* transitorios, mitigados pero no eliminados por los reintentos. Además, los reintentos solo cubren GET: un POST que falle no se reintenta, para no duplicar operaciones.

## P04 · Comunicación entre servicios

**Objetivo.** Ejecutar una operación de negocio que requiera una llamada real de pedidos a catálogo mediante descubrimiento dinámico.

**Procedimiento.** `.\scripts\p04-comunicacion.ps1`: `POST /api/pedidos`. Para crear el pedido, servicio-pedidos llama a `servicio-catalogo` (`POST /catalogo/productos/{id}/reservar`) a través de un `RestClient` con `@LoadBalanced`, resuelto por Eureka. También prueba un producto inexistente y una cantidad mayor que el stock.

**Resultado esperado.** Pedido creado (201), con `catalogoInstancia` alternándose, el stock reducido, los errores 404 y 409 controlados y las trazas `[CATALOGO]` en el log.

**Resultado obtenido.**

```text
Stock del producto 2 ANTES : 194
HTTP 201 | pedido=17 total=58.50 | atendido por pedidos-1 | reserva hecha por catalogo=catalogo-2 | nodoBd=primary
Stock del producto 2 DESPUES: 191  (debe haber bajado en 3)
--- Varios pedidos: observe como catalogo-1 y catalogo-2 se alternan
  pedido 18 -> reserva por catalogo-1
  pedido 19 -> reserva por catalogo-2
  pedido 20 -> reserva por catalogo-1
  pedido 21 -> reserva por catalogo-2
--- Error controlado 1: producto inexistente
HTTP 404 -> {"error":"El producto 9999 no existe"}
--- Error controlado 2: stock insuficiente
HTTP 409 -> {"error":"Stock insuficiente para el producto 1"}
--- Trazas de la aplicacion
pedidos-1 | [CATALOGO] reserva OK producto=2 atendida por instancia=catalogo-2 (intento 1)
pedidos-1 | [CATALOGO] reserva OK producto=1 atendida por instancia=catalogo-1 (intento 1)
```

**Evidencia.** `evidencias/p04-comunicacion.txt`.

**Análisis.** La comunicación entre servicios usa el nombre lógico `servicio-catalogo`, sin URL fija. Pedidos no accede a las tablas del catálogo: el stock solo cambia a través de la API de catálogo, y cada servicio tiene su propio esquema y usuario de base de datos. Las reservas se alternan entre las dos instancias de catálogo, así que el balanceo también se aplica en la llamada interna, no solo en el Gateway. Los errores de negocio de catálogo se propagan con su código (404 o 409) y un mensaje JSON, sin convertirse en un 500 genérico. Las líneas `ErrorTerminación(Invoke-WebRequest)` del archivo de evidencia las escribe la transcripción de PowerShell 5.1 cuando el script recibe a propósito esos 404 y 409; no son fallos del sistema. **Limitación:** la reserva de stock y el registro del pedido son dos transacciones separadas en dos servicios. Si catálogo reserva y pedidos falla antes de guardar, el stock queda descontado sin pedido. Resolverlo requiere una Saga o una compensación, que la actividad declara fuera de alcance.

## P05 · Enrutamiento de base de datos

**Objetivo.** Demostrar que las escrituras usan el primario y que las lecturas ordinarias se reparten entre las dos réplicas.

**Procedimiento.** `.\scripts\p05-enrutamiento-bd.ps1`: una escritura (`POST /api/catalogo/productos`), 30 lecturas (`GET /api/catalogo/productos`), el estado de los nodos según la aplicación (`/catalogo/db/estado`), los logs `[DB]` y las conexiones por nodo en `pg_stat_activity`.

**Resultado esperado.** Escritura con `nodoBd=primary`; 30 lecturas repartidas cerca de 50/50 entre `replica1` y `replica2`; logs `[DB]` con nodo y SQL; `pg_stat_activity` coherente.

**Resultado obtenido.**

```text
--- ESCRITURA (POST /api/catalogo/productos)
HTTP 201 | nodoBd = primary | instancia = catalogo-2
--- 30 LECTURAS ordinarias (GET /api/catalogo/productos)
  replica1         15  (50 %)
  replica2         15  (50 %)
--- Estado de los nodos segun la aplicacion (/db/estado)
nombre   rol                  saludable clusterNameReportado
primary  PRIMARIO (escritura)      True primary
replica1 REPLICA (lectura)         True replica1
replica2 REPLICA (lectura)         True replica2
--- Registros de la aplicacion
catalogo-1 | [DB] READ nodo=replica2 sql=SELECT id, nombre, precio, stock FROM productos ORDER BY id
catalogo-1 | [DB] READ nodo=replica1 sql=SELECT id, nombre, precio, stock FROM productos ORDER BY id
--- pg_stat_activity (conexiones de la aplicacion por nodo)
[primary]  servicio-catalogo-catalogo-1|1  servicio-catalogo-catalogo-2|1  servicio-pedidos-pedidos-1|1
[replica1] servicio-catalogo-catalogo-1|1  servicio-catalogo-catalogo-2|1  servicio-pedidos-pedidos-1|1
[replica2] servicio-catalogo-catalogo-1|1  servicio-catalogo-catalogo-2|1  servicio-pedidos-pedidos-1|1
```

**Evidencia.** `evidencias/p05-enrutamiento-bd.txt`.

**Análisis.** El enrutamiento lo hace el módulo `db-routing`, con un pool de conexiones (HikariCP) por nodo y una política explícita: `write()` va siempre al primario, `readPrimary()` atiende lecturas críticas en el primario y `read()` hace round-robin entre las réplicas sanas. No depende de `@Transactional(readOnly = true)`. La columna `clusterNameReportado` sale de consultar `cluster_name` en cada servidor, así que confirma que la conexión llega de verdad a ese nodo y no a otro por error de configuración. `pg_stat_activity` muestra una conexión por instancia en los tres nodos, identificada con su `application_name`, y los logs `[DB]` registran el nodo y la SQL de cada lectura. El reparto es exactamente 15/15. **Limitación:** la replicación es asíncrona, así que una lectura ordinaria puede devolver datos con algunos milisegundos de retraso respecto al primario. Por eso existe la lectura crítica de P07.

## P06 · Fallo de réplica PostgreSQL

**Objetivo.** Detener una réplica y comprobar que las consultas continúan por la restante.

**Procedimiento.** `.\scripts\p06-fallo-replica.ps1`: `docker compose stop replica1`, espera de 6 s, estado de los contenedores, 30 lecturas y estado de los nodos según la aplicación.

**Resultado esperado.** `replica1` detenida; 0 errores en 30 lecturas, todas en `replica2`; log `[DB-HEALTH] replica1 EXCLUIDA`.

**Resultado obtenido.**

```text
--- Estado de contenedores (despues de detener replica1)
taller-primary-1    postgres:17   ...   Up 2 hours (healthy)
taller-replica2-1   postgres:17   ...   Up 2 hours
--- 30 lecturas con replica1 detenida
Errores: 0 de 30
  replica2         30  (100 %)
--- Estado de nodos segun la aplicacion
primary       True
replica1     False Connection to replica1:5432 refused. Check that the hostname and port are correct ...
replica2      True
--- Registros del mecanismo de seleccion
[DB-HEALTH] replica1 EXCLUIDA de las lecturas: Connection to replica1:5432 refused. ...
```

**Evidencia.** `evidencias/p06-fallo-replica.txt` (incluye `docker compose ps` antes y después).

**Análisis.** La continuidad está garantizada por dos mecanismos. El monitor de salud (`ReplicaHealthMonitor`) revisa cada réplica cada 2 s y la excluye en cuanto falla la conexión, que es lo que muestra el log `EXCLUIDA`. Además, si una lectura llega a una réplica que acaba de caer antes de que el monitor lo detecte, `DbRouter.read()` captura el error de conexión, marca esa réplica como caída y reintenta la misma consulta en la siguiente réplica sana. Por eso las 30 lecturas terminaron bien y todas en `replica2`. **Tiempo de detección:** como máximo un intervalo del monitor (2 s) más el *timeout* de conexión (1,5 s). **Limitaciones:** mientras queda una sola réplica, toda la carga de lectura recae en ella. Si también cayera `replica2`, el enrutador degradaría las lecturas al primario: se prioriza la disponibilidad sobre el aislamiento de cargas, y queda registrado como `READ sin replicas sanas`. No se implementa la promoción automática del primario, que la actividad no exige.

## P07 · Consistencia de lectura

**Objetivo.** Crear un registro y consultarlo de inmediato con una operación crítica dirigida al primario.

**Procedimiento.** `.\scripts\p07-consistencia.ps1`. **Parte A:** crea un pedido y lo lee con `?consistencia=fuerte` (primario) y sin ese parámetro (réplica). **Parte B:** pausa la aplicación de WAL en las dos réplicas (`pg_wal_replay_pause()`), crea otro pedido, lo lee 4 veces en modo eventual y una en modo fuerte, y reanuda el replay (`pg_wal_replay_resume()`).

**Resultado esperado.** La lectura fuerte responde 200 desde el `primary`. Con el replay pausado, la lectura eventual da 404 en las réplicas mientras la fuerte da 200.

**Resultado obtenido.**

```text
=== PARTE A: secuencia normal ===
1) POST /api/pedidos                -> HTTP 201, pedido=24, escritura en nodoBd=primary
2) GET  /api/pedidos/24 (fuerte)    -> HTTP 200, leido de nodoBd=primary
3) GET  /api/pedidos/24 (eventual)  -> HTTP 200, leido de nodoBd=replica1

=== PARTE B: replay de WAL pausado en las replicas ===
POST /api/pedidos -> pedido=25 creado en primary
  lectura EVENTUAL -> HTTP 404 en nodoBd=replica2
  lectura EVENTUAL -> HTTP 404 en nodoBd=replica1
  lectura EVENTUAL -> HTTP 404 en nodoBd=replica2
  lectura EVENTUAL -> HTTP 404 en nodoBd=replica1
  lectura FUERTE   -> HTTP 200 en nodoBd=primary
Replay REANUDADO en ambas replicas.
  lectura EVENTUAL tras reanudar -> HTTP 200 en nodoBd=replica2
```

**Evidencia.** `evidencias/p07-consistencia.txt`.

**Análisis.** **Política aplicada:** *read-your-writes* explícito. Toda lectura que deba ver una escritura recién hecha (por ejemplo, confirmar un pedido justo después de crearlo) se marca como crítica con `?consistencia=fuerte` y se dirige al primario mediante `readPrimary()`. Las demás lecturas aceptan consistencia eventual y van a las réplicas. En condiciones normales (parte A), el desfase de la replicación es tan pequeño que la lectura eventual ya encuentra el pedido, así que el problema no se observa. Por eso la parte B lo provoca: con el replay pausado, las réplicas reciben el WAL pero no lo aplican. Las 4 lecturas eventuales dan 404 en las dos réplicas mientras la fuerte da 200 en el primario, y al reanudar el replay la réplica alcanza al primario y responde 200. Esto demuestra que el riesgo existe con replicación asíncrona y que la política lo resuelve. Los 404 de la parte B son el resultado esperado de la demostración, no errores del sistema. **Limitación:** la decisión de qué lectura es crítica la toma quien llama a la API. Si un cliente olvida el parámetro, puede leer un dato desactualizado. Una alternativa más automática sería guardar el LSN de la última escritura de cada sesión y leer de una réplica solo si ya lo alcanzó.

## P08 · Recuperación de réplica

**Objetivo.** Reiniciar la réplica detenida, comprobar su recuperación y su regreso a las consultas.

**Procedimiento.** `.\scripts\p08-recuperacion.ps1`: `docker compose start replica1`. Consulta cada 2 s el estado según la aplicación hasta que `replica1` esté saludable, revisa `pg_stat_replication` en el primario y hace 30 lecturas.

**Resultado esperado.** `replica1` pasa a saludable en pocos segundos, `pg_stat_replication` muestra 2 réplicas en `streaming` y las lecturas vuelven a repartirse entre ambas.

**Resultado obtenido.**

```text
--- >>> docker compose start replica1
  0,0s replica1 saludable=False The connection attempt failed.
  6,1s replica1 saludable=False The connection attempt failed.
  8,2s replica1 saludable=False
 10,2s replica1 saludable=True
replica1 REINCORPORADA tras 10.2 s
--- Estado de replicacion visto desde el primario
 application_name |   state   | sync_state | sent_lsn  | replay_lsn
------------------+-----------+------------+-----------+------------
 replica2         | streaming | async      | 0/5027620 | 0/5027620
 replica1         | streaming | async      | 0/5027620 | 0/5027620
--- 30 lecturas tras la recuperacion
  replica1         15  (50 %)
  replica2         15  (50 %)
--- Registros del mecanismo de seleccion
[DB-HEALTH] replica1 EXCLUIDA de las lecturas: Connection to replica1:5432 refused. ...
[DB-HEALTH] replica1 REINCORPORADA al grupo de lectura (cluster_name=replica1)
```

**Evidencia.** `evidencias/p08-recuperacion.txt`.

**Análisis.** La réplica no vuelve a recibir consultas solo porque su contenedor esté encendido. El monitor la considera apta cuando acepta conexiones, está en modo recuperación (`pg_is_in_recovery()`) y está recibiendo WAL del primario (`require-streaming: true`), y además exige **2 chequeos consecutivos** correctos. Eso evita reincorporar una réplica que aún está arrancando o que no está sincronizada. En la traza se ve primero el fallo de conexión (PostgreSQL arrancando), luego un chequeo sin error que todavía no basta, y al segundo chequeo correcto la réplica queda **reincorporada a los 10,2 s**. En una ejecución anterior el mismo proceso tardó 16,2 s, así que el tiempo de reincorporación observado está entre 10 y 16 s, según cuánto tarde PostgreSQL en arrancar y reconectarse al primario. `pg_stat_replication` confirma que las dos réplicas están en `streaming` y con el mismo `replay_lsn` que el primario, es decir, sin retraso. Las lecturas vuelven al reparto 15/15. **Limitación:** si la réplica estuvo detenida mucho tiempo y el primario ya descartó el WAL que necesita (más de `wal_keep_size = 256MB`), no podría ponerse al día por streaming y habría que reconstruirla con `pg_basebackup`, borrando su volumen.

## Conclusiones generales

- Las 8 pruebas dieron el resultado esperado: descubrimiento y balanceo por nombre lógico, continuidad ante la caída de una instancia y de una réplica, enrutamiento de lecturas y escrituras, y consistencia en lecturas críticas.
- No hubo errores visibles para el cliente en los fallos provocados. Esto se debe a que el apagado fue ordenado (`docker compose stop`) y a los reintentos en dos niveles: en el Gateway para HTTP y en `DbRouter` para la base de datos. Con fallos abruptos se esperan errores transitorios de pocos segundos.
- Tiempos medidos: una instancia de catálogo tarda unos **18 s** en volver a recibir tráfico, y una réplica entre **10 y 16 s** en reincorporarse a las lecturas.
- Limitaciones conocidas: solo hay una instancia de pedidos, Eureka y Gateway; la reserva de stock y el pedido no son atómicos entre servicios; el primario no tiene *failover* automático; y la lectura crítica depende de que el cliente la solicite.
