# Funciones comunes. Se carga con:  . "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Continue'
Set-Location (Join-Path $PSScriptRoot '..')            # raiz del repositorio
$Gw = if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { 'http://localhost:8080' }
$Ev = Join-Path (Get-Location).Path 'evidencias'
New-Item -ItemType Directory -Force -Path $Ev | Out-Null
$AdminUser = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { 'admin' }
$Db = if ($env:POSTGRES_DB) { $env:POSTGRES_DB } else { 'laboratorio' }

function Invoke-Api {
    param([string]$Method = 'GET', [string]$Url, $Body = $null)
    $sw = [Diagnostics.Stopwatch]::StartNew()
    $p = @{ Method = $Method; Uri = $Url; TimeoutSec = 6; UseBasicParsing = $true }
    if ($null -ne $Body) { $p.Body = ($Body | ConvertTo-Json -Compress); $p.ContentType = 'application/json' }
    try {
        $r = Invoke-WebRequest @p
        $json = $null
        if ($r.Content) { try { $json = $r.Content | ConvertFrom-Json } catch {} }
        [pscustomobject]@{ Status = [int]$r.StatusCode; Body = $json; Ms = $sw.ElapsedMilliseconds; Error = '' }
    } catch {
        $st = 0; $json = $null
        if ($_.Exception.Response) { $st = [int]$_.Exception.Response.StatusCode }
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) { try { $json = $_.ErrorDetails.Message | ConvertFrom-Json } catch {} }
        [pscustomobject]@{ Status = $st; Body = $json; Ms = $sw.ElapsedMilliseconds; Error = $_.Exception.Message }
    }
}

function Show-Dist($items, $prop) {
    $items = @($items)
    $items | Group-Object $prop | Sort-Object Name | ForEach-Object {
        '  {0,-14} {1,4}  ({2:P0})' -f $_.Name, $_.Count, ($_.Count / $items.Count)
    }
}

function Wait-Healthy([string]$url, [int]$seg = 120) {
    $fin = (Get-Date).AddSeconds($seg)
    while ((Get-Date) -lt $fin) { if ((Invoke-Api -Url $url).Status -eq 200) { return $true }; Start-Sleep 2 }
    return $false
}
