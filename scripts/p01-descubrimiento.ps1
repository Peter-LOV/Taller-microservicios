# P01 - Descubrimiento dinamico: instancias registradas en Eureka y localizadas por nombre logico
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p01-descubrimiento.txt') -Force | Out-Null

'=== Instancias registradas en Eureka (http://localhost:8761) ==='
$apps = Invoke-RestMethod -Uri 'http://localhost:8761/eureka/apps' -Headers @{ Accept = 'application/json' }
$filas = foreach ($a in @($apps.applications.application)) {
    foreach ($i in @($a.instance)) {
        [pscustomobject]@{ Servicio = $a.name; InstanceId = $i.instanceId; IP = $i.ipAddr; Puerto = $i.port.'$'; Estado = $i.status }
    }
}
$filas | Format-Table -AutoSize

'=== Localizacion por nombre logico a traves del Gateway (lb://) ==='
foreach ($ruta in 'catalogo', 'pedidos') {
    1..4 | ForEach-Object {
        $r = Invoke-Api -Url "$Gw/api/$ruta/info"
        "GET /api/$ruta/info -> HTTP $($r.Status) servicio=$($r.Body.servicio) instancia=$($r.Body.instancia) host=$($r.Body.host)"
    }
}
Stop-Transcript | Out-Null
