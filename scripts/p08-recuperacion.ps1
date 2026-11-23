# P08 - Recuperacion de replica: se reinicia replica1 y se verifica que vuelve a atender lecturas
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p08-recuperacion.txt') -Force | Out-Null

'--- >>> docker compose start replica1'; docker compose start replica1 | Out-Null
$t0 = Get-Date; $ok = $false
while (((Get-Date) - $t0).TotalSeconds -lt 90) {
    $nodos = (Invoke-Api -Url 'http://localhost:8081/catalogo/db/estado').Body.nodos
    $r1 = $nodos | Where-Object nombre -eq 'replica1'
    '{0,5:N1}s replica1 saludable={1} {2}' -f ((Get-Date) - $t0).TotalSeconds, $r1.saludable, $r1.ultimoError
    if ($r1.saludable) { $ok = $true; break }
    Start-Sleep 2
}
if ($ok) { "replica1 REINCORPORADA tras $([math]::Round(((Get-Date) - $t0).TotalSeconds, 1)) s" } else { 'replica1 NO se reincorporo en 90 s' }

'--- Estado de replicacion visto desde el primario'
docker compose exec -T primary psql -U $AdminUser -d $Db -c 'SELECT application_name, state, sync_state, sent_lsn, replay_lsn FROM pg_stat_replication;'
'--- 30 lecturas tras la recuperacion'
$l = 1..30 | ForEach-Object { [pscustomobject]@{ Nodo = (Invoke-Api -Url "$Gw/api/catalogo/productos").Body.nodoBd } }
Show-Dist $l 'Nodo'
'--- Registros del mecanismo de seleccion'
docker compose logs --tail=60 catalogo-1 | Select-String 'DB-HEALTH'
Stop-Transcript | Out-Null
