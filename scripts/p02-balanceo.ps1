# P02 - Balanceo HTTP: N solicitudes contra servicio-catalogo (2 instancias) y distribucion observada
param([int]$Total = 100)
. "$PSScriptRoot\common.ps1"
Start-Transcript -Path (Join-Path $Ev 'p02-balanceo.txt') -Force | Out-Null

$res = 1..$Total | ForEach-Object {
    $r = Invoke-Api -Url "$Gw/api/catalogo/info"
    [pscustomobject]@{ N = $_; Status = $r.Status; Instancia = $(if ($r.Body.instancia) { $r.Body.instancia } else { 'ERROR' }) }
}
"Total de solicitudes : $Total"
"Respuestas HTTP 200  : $(@($res | Where-Object Status -eq 200).Count)"
"Errores              : $(@($res | Where-Object Status -ne 200).Count)"
'Distribucion por instancia:'
Show-Dist $res 'Instancia'
$res | Export-Csv (Join-Path $Ev 'p02-balanceo.csv') -NoTypeInformation
Stop-Transcript | Out-Null
