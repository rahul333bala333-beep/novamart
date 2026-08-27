# start-local.ps1
$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }

$logsDir = "$root\logs"
if (-not (Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir | Out-Null
}

Write-Host "=================================================" -ForegroundColor Cyan
Write-Host "  Starting ZENO MART Microservices & Frontend    " -ForegroundColor Cyan
Write-Host "=================================================" -ForegroundColor Cyan

$services = @(
    @{ Name = "auth-service"; Jar = "$root\services\auth-service\target\auth-service.jar"; Port = 8081 },
    @{ Name = "product-service"; Jar = "$root\services\product-service\target\product-service.jar"; Port = 8082 },
    @{ Name = "inventory-service"; Jar = "$root\services\inventory-service\target\inventory-service.jar"; Port = 8086 },
    @{ Name = "payment-service"; Jar = "$root\services\payment-service\target\payment-service.jar"; Port = 8085 },
    @{ Name = "notification-service"; Jar = "$root\services\notification-service\target\notification-service.jar"; Port = 8087 },
    @{ Name = "cart-service"; Jar = "$root\services\cart-service\target\cart-service.jar"; Port = 8083 },
    @{ Name = "order-service"; Jar = "$root\services\order-service\target\order-service.jar"; Port = 8084 },
    @{ Name = "api-gateway"; Jar = "$root\api-gateway\target\api-gateway.jar"; Port = 8080 }
)

$pids = @()

foreach ($svc in $services) {
    Write-Host "Starting $($svc.Name) on port $($svc.Port)..." -ForegroundColor Yellow
    $proc = Start-Process -FilePath "java.exe" `
        -ArgumentList "-jar `"$($svc.Jar)`" --spring.profiles.active=local" `
        -WorkingDirectory $root `
        -RedirectStandardOutput "$logsDir\$($svc.Name).log" `
        -RedirectStandardError "$logsDir\$($svc.Name)-err.log" `
        -WindowStyle Hidden `
        -PassThru
    $pids += [PSCustomObject]@{ Name = $svc.Name; Id = $proc.Id; Port = $svc.Port }
    Start-Sleep -Milliseconds 600
}

Write-Host "Starting Next.js Frontend on port 3000..." -ForegroundColor Yellow
$frontendProc = Start-Process -FilePath "cmd.exe" `
    -ArgumentList @("/c", "npm run dev") `
    -WorkingDirectory "$root\frontend" `
    -RedirectStandardOutput "$logsDir\frontend.log" `
    -RedirectStandardError "$logsDir\frontend-err.log" `
    -WindowStyle Hidden `
    -PassThru
$pids += [PSCustomObject]@{ Name = "frontend"; Id = $frontendProc.Id; Port = 3000 }

$pids | Export-Csv -Path "$root\.running-pids.csv" -NoTypeInformation

Write-Host ""
Write-Host "All services starting in background!" -ForegroundColor Green
Write-Host "Logs are streaming to: $logsDir" -ForegroundColor Gray
Write-Host "  - Storefront: http://localhost:3000" -ForegroundColor Cyan
Write-Host "  - Admin Portal: http://localhost:3000/admin" -ForegroundColor Cyan
Write-Host "  - API Gateway: http://localhost:8080/api/v1" -ForegroundColor Cyan
Write-Host ""
Write-Host "To stop all services, run: .\stop-local.ps1" -ForegroundColor Yellow
