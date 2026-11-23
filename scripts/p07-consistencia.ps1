# P07 - Consistencia de lectura: crear y consultar de inmediato con lectura critica (primario)
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p07-consistencia.txt') -Force | Out-Null

'=== PARTE A: secuencia normal ==='
$c = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 3; cantidad = 1 }
$id = $c.Body.pedido.id
"1) POST /api/pedidos            -> HTTP $($c.Status), pedido=$id, escritura en nodoBd=$($c.Body.nodoBd)"
$f = Invoke-Api -Url "$Gw/api/pedidos/${id}?consistencia=fuerte"
"2) GET  /api/pedidos/$id (fuerte)   -> HTTP $($f.Status), leido de nodoBd=$($f.Body.nodoBd)"
$e = Invoke-Api -Url "$Gw/api/pedidos/${id}"
"3) GET  /api/pedidos/$id (eventual) -> HTTP $($e.Status), leido de nodoBd=$($e.Body.nodoBd)"

'';'=== PARTE B: demostracion del problema (se pausa la aplicacion de WAL en las replicas) ==='
foreach ($n in 'replica1', 'replica2') { docker compose exec -T $n psql -U $AdminUser -d $Db -At -c 'SELECT pg_wal_replay_pause();' | Out-Null }
'Replicas con replay PAUSADO (reciben WAL pero no lo aplican).'
$c2 = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 3; cantidad = 1 }
$id2 = $c2.Body.pedido.id
"POST /api/pedidos -> pedido=$id2 creado en $($c2.Body.nodoBd)"
1..4 | ForEach-Object {
    $x = Invoke-Api -Url "$Gw/api/pedidos/${id2}"
    "  lectura EVENTUAL -> HTTP $($x.Status) en nodoBd=$($x.Body.nodoBd)   <-- 404: la replica aun no lo ve"
}
$y = Invoke-Api -Url "$Gw/api/pedidos/${id2}?consistencia=fuerte"
"  lectura FUERTE   -> HTTP $($y.Status) en nodoBd=$($y.Body.nodoBd)   <-- 200: el primario siempre lo ve"
foreach ($n in 'replica1', 'replica2') { docker compose exec -T $n psql -U $AdminUser -d $Db -At -c 'SELECT pg_wal_replay_resume();' | Out-Null }
'Replay REANUDADO en ambas replicas.'
Start-Sleep 2
$z = Invoke-Api -Url "$Gw/api/pedidos/${id2}"
"  lectura EVENTUAL tras reanudar -> HTTP $($z.Status) en nodoBd=$($z.Body.nodoBd)"
Stop-Transcript | Out-Null
