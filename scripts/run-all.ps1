# Ejecuta P01..P08 en orden (la evidencia queda en la carpeta evidencias/)
foreach ($p in 'p01-descubrimiento', 'p02-balanceo', 'p03-fallo-instancia', 'p04-comunicacion',
               'p05-enrutamiento-bd', 'p06-fallo-replica', 'p07-consistencia', 'p08-recuperacion') {
    "`n########## $p ##########"
    & (Join-Path $PSScriptRoot "$p.ps1")
}
