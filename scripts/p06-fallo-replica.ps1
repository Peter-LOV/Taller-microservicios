# P06 - Fallo de replica PostgreSQL: se detiene replica1 y las consultas continuan por replica2
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p06-fallo-replica.txt') -Force | Out-Null

'--- Antes: docker compose ps'; docker compose ps primary replica1 replica2
'--- >>> docker compose stop replica1'; docker compose stop replica1 | Out-Null
'(esperando 6 s a que el monitor de salud detecte la caida...)'; Start-Sleep 6

'--- Estado de contenedores'; docker compose ps primary replica1 replica2
'--- 30 lecturas con replica1 detenida'
$lecturas = 1..30 | ForEach-Object { $r = Invoke-Api -Url "$Gw/api/catalogo/productos"; [pscustomobject]@{ Status = $r.Status; Nodo = $r.Body.nodoBd } }
"Errores: $(@($lecturas | Where-Object Status -ne 200).Count) de 30"
Show-Dist $lecturas 'Nodo'

'--- Estado de nodos segun la aplicacion'
(Invoke-Api -Url 'http://localhost:8081/catalogo/db/estado').Body.nodos | Format-Table nombre, saludable, ultimoError -AutoSize -Wrap
'--- Registros del mecanismo de seleccion'
docker compose logs --tail=60 catalogo-1 | Select-String 'DB-HEALTH|falla en nodo'
Stop-Transcript | Out-Null
