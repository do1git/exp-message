param(
    [string]$Action = "up",
    [string]$Service = "",
    [switch]$Help
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ComposeDir = Join-Path $ScriptDir "..\..\01-infrastructure\00-compose-all"
$EnvFile = Join-Path $ScriptDir ".env"
$DefaultEnvFile = Join-Path $ScriptDir "default.env"

function Write-Info { Write-Host "[INFO] $args" -ForegroundColor Blue }
function Write-Success { Write-Host "[SUCCESS] $args" -ForegroundColor Green }
function Write-Warn { Write-Host "[WARN] $args" -ForegroundColor Yellow }
function Write-Error { Write-Host "[ERROR] $args" -ForegroundColor Red }

function Check-Env {
    if (-not (Test-Path $EnvFile)) {
        Write-Warn ".env file not found: $EnvFile"
        if (Test-Path $DefaultEnvFile) {
            Write-Info "Copying default.env to .env..."
            Copy-Item $DefaultEnvFile $EnvFile
            Write-Success ".env file created from default.env"
        } else {
            Write-Error "default.env not found. Please create it first."
            return $false
        }
    }
    return $true
}

function Do-Up {
    Write-Info "Starting development environment..."
    
    if (-not (Check-Env)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    docker-compose -f docker-compose.dev.yml --env-file $EnvFile up -d
    
    Write-Info "Checking service status..."
    docker-compose -f docker-compose.dev.yml --env-file $EnvFile ps
    Pop-Location
    
    Write-Success "Development environment started!"
    Write-Info "MySQL: localhost:3306"
    Write-Info "Redis: localhost:6379"
}

function Do-Down {
    Write-Info "Stopping development environment..."
    
    if (-not (Check-Env)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    docker-compose -f docker-compose.dev.yml --env-file $EnvFile down
    Pop-Location
    
    Write-Success "Development environment stopped!"
}

function Do-Ps {
    if (-not (Check-Env)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    docker-compose -f docker-compose.dev.yml --env-file $EnvFile ps
    Pop-Location
}

function Do-Logs {
    param([string]$Svc)
    
    if (-not (Check-Env)) {
        exit 1
    }
    
    Push-Location $ComposeDir
    
    if ([string]::IsNullOrEmpty($Svc)) {
        docker-compose -f docker-compose.dev.yml --env-file $EnvFile logs -f
    } else {
        docker-compose -f docker-compose.dev.yml --env-file $EnvFile logs -f $Svc
    }
    Pop-Location
}

function Show-Help {
    Write-Host "Usage: .\dev-compose.ps1 [action] [service]"
    Write-Host ""
    Write-Host "Actions:"
    Write-Host "  up              Start dev services [default]"
    Write-Host "  down            Stop dev services"
    Write-Host "  ps              Show status"
    Write-Host "  logs [service]  Show logs"
    Write-Host ""
    Write-Host "Examples:"
    Write-Host "  .\dev-compose.ps1              # Start services"
    Write-Host "  .\dev-compose.ps1 down         # Stop services"
    Write-Host "  .\dev-compose.ps1 logs mysql   # Show MySQL logs"
}

if ($Help) {
    Show-Help
    exit 0
}

switch ($Action.ToLower()) {
    "up" { Do-Up }
    "down" { Do-Down }
    "ps" { Do-Ps }
    "logs" { Do-Logs -Svc $Service }
    default {
        Write-Error "Unknown action: $Action"
        Show-Help
        exit 1
    }
}
