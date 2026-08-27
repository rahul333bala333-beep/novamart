# stop-local.ps1
Write-Host "Stopping all running ZENO MART microservices and frontend..." -ForegroundColor Yellow

if (Test-Path "$PSScriptRoot\.running-pids.csv") {
    $pids = Import-Csv -Path "$PSScriptRoot\.running-pids.csv"
    foreach ($entry in $pids) {
        try {
            Stop-Process -Id $entry.Id -Force -ErrorAction SilentlyContinue
            Write-Host "Stopped $($entry.Name) (PID: $($entry.Id))" -ForegroundColor Gray
        } catch {}
    }
    Remove-Item "$PSScriptRoot\.running-pids.csv" -Force -ErrorAction SilentlyContinue
}

# Also cleanup any orphaned ports
$ports = @(3000, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087)
foreach ($port in $ports) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        foreach ($conn in $conns) {
            Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
        }
    } catch {}
}

Write-Host "All services stopped." -ForegroundColor Green
