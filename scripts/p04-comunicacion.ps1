# P04 - Comunicacion entre servicios: pedidos -> (Eureka) -> catalogo, con casos de exito y de error
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p04-comunicacion.txt') -Force | Out-Null

$antes = (Invoke-Api -Url "$Gw/api/catalogo/productos/2?consistencia=fuerte").Body.datos
"Stock del producto 2 ANTES : $($antes.stock)"

'--- Caso exitoso: POST /api/pedidos (producto 2, cantidad 3)'
$r = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 2; cantidad = 3 }
"HTTP $($r.Status) | pedido=$($r.Body.pedido.id) total=$($r.Body.pedido.total) | atendido por $($r.Body.instancia) | reserva hecha por catalogo=$($r.Body.catalogoInstancia) | nodoBd=$($r.Body.nodoBd)"

$despues = (Invoke-Api -Url "$Gw/api/catalogo/productos/2?consistencia=fuerte").Body.datos
"Stock del producto 2 DESPUES: $($despues.stock)  (debe haber bajado en 3)"

'--- Varios pedidos: observe como catalogo-1 y catalogo-2 se alternan'
1..6 | ForEach-Object {
    $x = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 1; cantidad = 1 }
    "  pedido $($x.Body.pedido.id) -> reserva por $($x.Body.catalogoInstancia)"
}

'--- Error controlado 1: producto inexistente'
$e1 = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 9999; cantidad = 1 }
"HTTP $($e1.Status) -> $($e1.Body | ConvertTo-Json -Compress)"
'--- Error controlado 2: stock insuficiente'
$e2 = Invoke-Api -Method POST -Url "$Gw/api/pedidos" -Body @{ productoId = 1; cantidad = 100000 }
"HTTP $($e2.Status) -> $($e2.Body | ConvertTo-Json -Compress)"

'--- Trazas de la aplicacion (llamada pedidos -> catalogo)'
docker compose logs --tail=40 pedidos | Select-String '\[CATALOGO\]'
Stop-Transcript | Out-Null
