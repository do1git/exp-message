# =============================================================================
# Docker Compose Deployment Script (PowerShell)
# Manages docker-compose services for local integrated environment
# =============================================================================

param(
    [Parameter(Position=0)]
    [string]$Action,

    [Parameter(Position=1)]
    [string]$Service,

    [switch]$Help,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeDir = Join-Path (Split-Path -Parent (Split-Path -Parent $ScriptDir)) "01-infrastructure\00-compose-all"
$EnvFile = Join-Path $ScriptDir ".env"

# Colored output functions
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Err { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# Check .env file
function Test-EnvFile {
    if (-not (Test-Path $EnvFile)) {
        Write-Warn ".env file not found: $EnvFile"
        Write-Info "Copy default.env from 01-infrastructure/00-compose-all to .env first."
        Write-Info "Example: Copy-Item '$ComposeDir\default.env' '$EnvFile'"
        return $false
    }
    return $true
}

# Up (start services)
function Invoke-Up {
    param([bool]$ForceBuild = $false)

    Write-Info "=========================================="
    Write-Info "Docker Compose Up Started"
    Write-Info "=========================================="

    if (-not (Test-EnvFile)) {
        exit 1
    }

    Write-Info "Compose directory: $ComposeDir"
    Write-Info "Environment file: $EnvFile"

    Push-Location $ComposeDir
    try {
        if ($ForceBuild) {
            Write-Info "Force rebuilding images (--no-cache)..."
            docker-compose --env-file $EnvFile build --no-cache
            if ($LASTEXITCODE -ne 0) { throw "docker-compose build failed" }
        }

        docker-compose --env-file $EnvFile up -d
        if ($LASTEXITCODE -ne 0) { throw "docker-compose up failed" }

        Write-Info "Checking service status..."
        docker-compose --env-file $EnvFile ps
    } finally {
        Pop-Location
    }

    Write-Success "=========================================="
    Write-Success "Docker Compose Up Completed!"
    Write-Success "=========================================="
}

# Down (stop services)
function Invoke-Down {
    Write-Info "=========================================="
    Write-Info "Docker Compose Down Started"
    Write-Info "=========================================="
    
    if (-not (Test-EnvFile)) {
        exit 1
    }
    
    Write-Info "Compose directory: $ComposeDir"
    Write-Info "Environment file: $EnvFile"
    
    Push-Location $ComposeDir
    try {
        docker-compose --env-file $EnvFile down
        if ($LASTEXITCODE -ne 0) { throw "docker-compose down failed" }
    } finally {
        Pop-Location
    }
    
    Write-Success "=========================================="
    Write-Success "Docker Compose Down Completed!"
    Write-Success "=========================================="
}

# Status
function Invoke-Status {
    if (-not (Test-EnvFile)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    try {
        docker-compose --env-file $EnvFile ps
    } finally {
        Pop-Location
    }
}

# Logs
function Invoke-Logs {
    param([string]$ServiceName = "")
    
    if (-not (Test-EnvFile)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    try {
        if ([string]::IsNullOrEmpty($ServiceName)) {
            Write-Info "Showing logs for all services (Press Ctrl+C to exit)"
            docker-compose --env-file $EnvFile logs -f
        } else {
            Write-Info "Showing logs for service: $ServiceName (Press Ctrl+C to exit)"
            docker-compose --env-file $EnvFile logs -f $ServiceName
        }
    } finally {
        Pop-Location
    }
}

# Restart
function Invoke-Restart {
    param([string]$ServiceName = "")
    
    Write-Info "=========================================="
    Write-Info "Docker Compose Restart"
    Write-Info "=========================================="
    
    if (-not (Test-EnvFile)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    try {
        if ([string]::IsNullOrEmpty($ServiceName)) {
            Write-Info "Restarting all services..."
            docker-compose --env-file $EnvFile restart
        } else {
            Write-Info "Restarting service: $ServiceName"
            docker-compose --env-file $EnvFile restart $ServiceName
        }
        if ($LASTEXITCODE -ne 0) { throw "docker-compose restart failed" }
    } finally {
        Pop-Location
    }
    
    Write-Success "Restart completed!"
}

# Help
function Show-Help {
    Write-Host @"
Usage: .\docker-compose.ps1 [action] [service]

Options:
  -Help            Show this help message
  -Build           Force rebuild images (--no-cache) before up

Actions:
  up               Start all services [default]
  down             Stop all services
  ps               Show service status
  logs [service]   Show logs (all services or specific service)
  restart [service]  Restart services (all or specific)

Examples:
  .\docker-compose.ps1        # Start all services (default)
  .\docker-compose.ps1 up     # Start all services
  .\docker-compose.ps1 up -Build  # Force rebuild and start
  .\docker-compose.ps1 down   # Stop all services
  .\docker-compose.ps1 ps    # Show status
  .\docker-compose.ps1 logs  # Show all logs
  .\docker-compose.ps1 logs mysql  # Show mysql logs
  .\docker-compose.ps1 restart  # Restart all services
  .\docker-compose.ps1 restart nginx  # Restart nginx service
"@
}

# Main
if ($Help) {
    Show-Help
    exit 0
}

if (-not $Action) {
    Write-Info "No action specified. Using default 'up'."
    $Action = "up"
}

switch ($Action) {
    "up" { Invoke-Up -ForceBuild:$Build }
    "down" { Invoke-Down }
    "ps" { Invoke-Status }
    "logs" { Invoke-Logs -ServiceName $Service }
    "restart" { Invoke-Restart -ServiceName $Service }
    default {
        Write-Err "Unknown action: $Action"
        Show-Help
        exit 1
    }
}
