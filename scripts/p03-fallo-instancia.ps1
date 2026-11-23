# P03 - Fallo de instancia: se detiene catalogo-2 con trafico continuo y luego se recupera
param([int]$Duracion = 60, [int]$SegFallo = 10, [int]$SegRecuperar = 35, [string]$Servicio = 'catalogo-2')
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p03-fallo-instancia.txt') -Force | Out-Null

$t0 = Get-Date; $detenida = $false; $iniciada = $false; $filas = @()
while (($s = ((Get-Date) - $t0).TotalSeconds) -lt $Duracion) {
    if (-not $detenida -and $s -ge $SegFallo) {
        ('[{0,5:N1}s] >>> docker compose stop {1}' -f $s, $Servicio)
        Start-Process -FilePath docker -ArgumentList @('compose', 'stop', $Servicio) -NoNewWindow
        $detenida = $true
    }
    if (-not $iniciada -and $s -ge $SegRecuperar) {
        ('[{0,5:N1}s] >>> docker compose start {1}' -f $s, $Servicio)
        Start-Process -FilePath docker -ArgumentList @('compose', 'start', $Servicio) -NoNewWindow
        $iniciada = $true
    }
    $fase = if (-not $detenida) { 'antes' } elseif (-not $iniciada) { 'durante' } else { 'despues' }
    $r = Invoke-Api -Url "$Gw/api/catalogo/info"
    $inst = if ($r.Body.instancia) { $r.Body.instancia } else { 'ERROR' }
    $filas += [pscustomobject]@{ T = [math]::Round($s, 1); Fase = $fase; Status = $r.Status; Instancia = $inst; Ms = $r.Ms }
    '{0,5:N1}s {1,-8} HTTP {2,-3} {3,-11} {4} ms' -f $s, $fase, $r.Status, $inst, $r.Ms
    Start-Sleep -Milliseconds 300
}

'';'================ RESUMEN ================'
foreach ($f in 'antes', 'durante', 'despues') {
    $x = @($filas | Where-Object Fase -eq $f)
    "--- $f : $($x.Count) solicitudes, errores = $(@($x | Where-Object Status -ne 200).Count)"
    if ($x.Count) { Show-Dist $x 'Instancia' }
}
$err = @($filas | Where-Object Status -ne 200)
if ($err.Count) {
    "Primer error: t=$($err[0].T)s | ultimo error: t=$($err[-1].T)s | ventana de errores = $([math]::Round($err[-1].T - $err[0].T, 1)) s"
} else { 'Sin errores observados durante la prueba.' }
$vuelve = $filas | Where-Object { $_.Fase -eq 'despues' -and $_.Instancia -eq $Servicio } | Select-Object -First 1
if ($vuelve) { "$Servicio vuelve a atender solicitudes en t=$($vuelve.T)s (a $([math]::Round($vuelve.T - $SegRecuperar, 1)) s de arrancarla)" }
$filas | Export-Csv (Join-Path $Ev 'p03-fallo-instancia.csv') -NoTypeInformation
'Verifique tambien el panel de Eureka: http://localhost:8761'
Stop-Transcript | Out-Null
