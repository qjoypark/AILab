param(
    [int]$TimeoutSeconds = 180,
    [switch]$RestartServices
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $Root "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message"
}

function Test-Port {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
}

function Stop-Port {
    param(
        [int]$Port,
        [string]$Name
    )

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Write-Host "STOP  $Name on port $Port (pid $($listener.OwningProcess))"
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

function Wait-Port {
    param(
        [int]$Port,
        [string]$Name,
        [int]$Seconds = $TimeoutSeconds
    )

    for ($i = 1; $i -le $Seconds; $i++) {
        if (Test-Port -Port $Port) {
            Write-Host "READY $Name on port $Port"
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "$Name did not listen on port $Port within $Seconds seconds."
}

function Wait-Nacos {
    Write-Step "Waiting for Nacos"
    for ($i = 1; $i -le $TimeoutSeconds; $i++) {
        $metricsReady = $false
        try {
            $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 `
                "http://localhost:8848/nacos/v1/ns/operator/metrics"
            $metricsReady = $response.Content -match '"status":"UP"'
        } catch {
            $metricsReady = $false
        }

        if ($metricsReady -and (Test-Port -Port 9848)) {
            Write-Host "READY Nacos on ports 8848 and 9848"
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "Nacos was not ready within $TimeoutSeconds seconds. Run: docker logs -f lab-nacos"
}

function Start-MavenService {
    param(
        [string]$Name,
        [string]$Path,
        [int]$Port
    )

    if ($RestartServices -and (Test-Port -Port $Port)) {
        Stop-Port -Port $Port -Name $Name
        Start-Sleep -Seconds 2
    }

    if (Test-Port -Port $Port) {
        Write-Host "SKIP  $Name already listening on port $Port"
        return
    }

    $servicePath = Join-Path $Root $Path
    $outLog = Join-Path $LogDir "$Name.out.log"
    $errLog = Join-Path $LogDir "$Name.err.log"

    Write-Host "START $Name"
    Start-Process -FilePath "mvn.cmd" `
        -ArgumentList "-Dmaven.test.skip=true", "spring-boot:run" `
        -WorkingDirectory $servicePath `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru | Out-Null

    Wait-Port -Port $Port -Name $Name
}

function Start-WebAdmin {
    $port = 3000
    if ($RestartServices -and (Test-Port -Port $port)) {
        Stop-Port -Port $port -Name "lab-web-admin"
        Start-Sleep -Seconds 2
    }

    if (Test-Port -Port $port) {
        Write-Host "SKIP  lab-web-admin already listening on port $port"
        return
    }

    $webPath = Join-Path $Root "lab-web-admin"
    $outLog = Join-Path $LogDir "lab-web-admin.out.log"
    $errLog = Join-Path $LogDir "lab-web-admin.err.log"

    Write-Host "START lab-web-admin"
    Start-Process -FilePath "npm.cmd" `
        -ArgumentList "run", "dev", "--", "--host", "0.0.0.0" `
        -WorkingDirectory $webPath `
        -WindowStyle Hidden `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError $errLog `
        -PassThru | Out-Null

    Wait-Port -Port $port -Name "lab-web-admin"
}

Write-Host "Smart Lab Management System"
Write-Host "Root: $Root"
if ($RestartServices) {
    Write-Host "RestartServices: enabled"
}

Write-Step "Starting infrastructure"
Push-Location $Root
try {
    docker compose up -d
} finally {
    Pop-Location
}

Wait-Nacos

Write-Step "Starting backend services"
Start-MavenService -Name "lab-gateway" -Path "lab-gateway" -Port 8080
Start-MavenService -Name "lab-service-user" -Path "lab-service-user" -Port 8081
Start-MavenService -Name "lab-service-material" -Path "lab-service-material" -Port 8082
Start-MavenService -Name "lab-service-inventory" -Path "lab-service-inventory" -Port 8083
Start-MavenService -Name "lab-service-approval" -Path "lab-service-approval" -Port 8084

Write-Step "Starting web admin"
Start-WebAdmin

Write-Step "Ready"
Write-Host "Web admin:        http://localhost:3000"
Write-Host "Gateway:          http://localhost:8080"
Write-Host "User API docs:    http://localhost:8081/doc.html"
Write-Host "Material docs:    http://localhost:8082/doc.html"
Write-Host "Inventory docs:   http://localhost:8083/doc.html"
Write-Host "Approval docs:    http://localhost:8084/doc.html"
Write-Host "Nacos:            http://localhost:8848/nacos"
Write-Host "RabbitMQ:         http://localhost:15672"
Write-Host "MinIO:            http://localhost:9001"
