# P05 - Enrutamiento de BD: escrituras al primario; lecturas repartidas entre replica1 y replica2
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p05-enrutamiento-bd.txt') -Force | Out-Null

'--- ESCRITURA (POST /api/catalogo/productos)'
$w = Invoke-Api -Method POST -Url "$Gw/api/catalogo/productos" -Body @{ nombre = 'Producto P05'; precio = 9.99; stock = 10 }
"HTTP $($w.Status) | nodoBd = $($w.Body.nodoBd) | instancia = $($w.Body.instancia)"

'--- 30 LECTURAS ordinarias (GET /api/catalogo/productos)'
$lecturas = 1..30 | ForEach-Object { [pscustomobject]@{ Nodo = (Invoke-Api -Url "$Gw/api/catalogo/productos").Body.nodoBd } }
Show-Dist $lecturas 'Nodo'

'--- Estado de los nodos segun la aplicacion (/db/estado)'
(Invoke-Api -Url "$Gw/api/catalogo/db/estado").Body.nodos | Format-Table nombre, rol, saludable, clusterNameReportado -AutoSize

'--- Registros de la aplicacion: nodo y SQL de cada operacion'
docker compose logs --tail=80 catalogo-1 catalogo-2 | Select-String '\[DB\] ' | Select-Object -Last 12

'--- Verificacion en PostgreSQL: conexiones de la aplicacion por nodo (pg_stat_activity)'
foreach ($n in 'primary', 'replica1', 'replica2') {
    "[$n]"
    docker compose exec -T $n psql -U $AdminUser -d $Db -At -c "SELECT application_name, count(*) FROM pg_stat_activity WHERE application_name LIKE 'servicio-%' GROUP BY 1 ORDER BY 1;"
}
Stop-Transcript | Out-Null
