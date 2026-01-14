# =============================================================================
# Helm Chart Deployment Script (PowerShell)
# Supports install, upgrade, uninstall actions.
# =============================================================================

param(
    [Parameter(Position=0)]
    [string]$Action,
    
    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$RemainingArgs,
    
    [switch]$Help
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ValuesFile = Join-Path $ScriptDir "values.yaml"
$KubeconfigFile = Join-Path $ScriptDir "kubeconfig.yaml"
$ReleaseName = "message-stack"

# Colored output functions
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Err { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# Prerequisites check
function Test-Prerequisites {
    # Check values.yaml exists
    if (-not (Test-Path $ValuesFile)) {
        Write-Err "values.yaml not found: $ValuesFile"
        Write-Info "Copy values.yaml.example to values.yaml first."
        exit 1
    }
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Config file: $ValuesFile"
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Release name: $ReleaseName"
}

# Install (create)
function Invoke-Install {
    Write-Info "=========================================="
    Write-Info "Helm Install Started"
    Write-Info "=========================================="
    
    Test-Prerequisites
    
    Write-Info "Updating dependencies..."
    helm dependency update --kubeconfig="$KubeconfigFile" $ScriptDir
    if ($LASTEXITCODE -ne 0) { throw "helm dependency update failed" }
    
    Write-Info "Running Helm Install..."
    helm install $ReleaseName $ScriptDir `
        --kubeconfig="$KubeconfigFile" `
        --values $ValuesFile
    if ($LASTEXITCODE -ne 0) { throw "helm install failed" }
    
    Write-Info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KubeconfigFile" `
        -l "app.kubernetes.io/instance=$ReleaseName"
    
    Write-Success "=========================================="
    Write-Success "Helm Install Completed!"
    Write-Success "=========================================="
}

# Upgrade (with rollout restart)
function Invoke-Upgrade {
    Write-Info "=========================================="
    Write-Info "Helm Upgrade Started"
    Write-Info "=========================================="
    
    Test-Prerequisites
    
    Write-Info "Updating dependencies..."
    helm dependency update --kubeconfig="$KubeconfigFile" $ScriptDir
    if ($LASTEXITCODE -ne 0) { throw "helm dependency update failed" }
    
    Write-Info "Running Helm Upgrade..."
    helm upgrade $ReleaseName $ScriptDir `
        --kubeconfig="$KubeconfigFile" `
        --values $ValuesFile
    if ($LASTEXITCODE -ne 0) { throw "helm upgrade failed" }
    
    Write-Info "Restarting pod rollout (required for latest tag)..."
    try {
        kubectl rollout restart "deployment/${ReleaseName}-app-monolitic" `
            --kubeconfig="$KubeconfigFile" 2>$null
    } catch {
        Write-Warn "app-monolitic deployment rollout failed (may not exist)"
    }
    
    Write-Info "Checking deployment status..."
    kubectl get pods --kubeconfig="$KubeconfigFile" `
        -l "app.kubernetes.io/instance=$ReleaseName"
    
    Write-Success "=========================================="
    Write-Success "Helm Upgrade Completed!"
    Write-Success "=========================================="
}

# Logs - App
function Invoke-LogsApp {
    Write-Info "=========================================="
    Write-Info "App (00-monolitic) Logs"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Press Ctrl+C to exit"
    Write-Host ""
    
    kubectl logs -f -l app.kubernetes.io/name=app-monolitic `
        --kubeconfig="$KubeconfigFile" `
        --tail=100
}

# Logs - Migration
function Invoke-LogsMigration {
    Write-Info "=========================================="
    Write-Info "Migration (01-db-migrations) Logs"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Press Ctrl+C to exit"
    Write-Host ""
    
    kubectl logs -f -l app.kubernetes.io/name=batch-db-migration `
        --kubeconfig="$KubeconfigFile" `
        --tail=100
}

# MySQL Port Forward (expose 3306)
function Invoke-MySQLPortForward {
    param([string]$LocalPort = "13306")
    
    Write-Info "=========================================="
    Write-Info "MySQL Port Forward ($LocalPort -> 3306)"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Forwarding 0.0.0.0:$LocalPort (Current PC) -> ${ReleaseName}-mysql:3306"
    
    # Check if mysqlsh or mysql command exists (prefer mysqlsh on Windows)
    $mysqlshExists = Get-Command mysqlsh -ErrorAction SilentlyContinue
    $mysqlExists = Get-Command mysql -ErrorAction SilentlyContinue
    
    if ($mysqlshExists -or $mysqlExists) {
        $clientName = if ($mysqlshExists) { "MySQL Shell (mysqlsh)" } else { "MySQL client" }
        Write-Info "$clientName found. Starting port-forward in background..."
        Write-Host ""
        
        # Start port-forward in background
        $job = Start-Job -ScriptBlock {
            param($kubeconfig, $releaseName, $localPort)
            kubectl port-forward "svc/$releaseName-mysql" "${localPort}:3306" `
                --address 0.0.0.0 `
                --kubeconfig="$kubeconfig"
        } -ArgumentList $KubeconfigFile, $ReleaseName, $LocalPort
        
        # Wait for port to be ready
        Start-Sleep -Seconds 2
        
        Write-Info "Connecting to MySQL... (type 'exit' or '\q' to quit)"
        Write-Host ""
        
        # Connect to MySQL (prefer mysqlsh)
        if ($mysqlshExists) {
            mysqlsh --sql -h 127.0.0.1 -P $LocalPort -u message_user -pmessage_password -D message_db
        } else {
            mysql -h 127.0.0.1 -P $LocalPort -u message_user -pmessage_password message_db
        }
        
        # Cleanup: stop port-forward job
        Write-Host ""
        Write-Info "Stopping port-forward..."
        Stop-Job -Job $job
        Remove-Job -Job $job
        Write-Success "Done!"
    } else {
        Write-Warn "MySQL client not found. Running port-forward only."
        Write-Info "Press Ctrl+C to stop port forwarding"
        Write-Host ""
        
        kubectl port-forward "svc/${ReleaseName}-mysql" "${LocalPort}:3306" `
            --address 0.0.0.0 `
            --kubeconfig="$KubeconfigFile"
    }
}

# MySQL Port Forward Only (no shell)
function Invoke-MySQLPortForwardOnly {
    param([string]$LocalPort = "13306")
    
    Write-Info "=========================================="
    Write-Info "MySQL Port Forward ($LocalPort -> 3306)"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Forwarding 0.0.0.0:$LocalPort (Current PC) -> ${ReleaseName}-mysql:3306"
    Write-Info "Press Ctrl+C to stop port forwarding"
    Write-Host ""
    
    kubectl port-forward "svc/${ReleaseName}-mysql" "${LocalPort}:3306" `
        --address 0.0.0.0 `
        --kubeconfig="$KubeconfigFile"
}

# MySQL Port Forward Background
function Invoke-MySQLPortForwardBackground {
    param([string]$LocalPort = "13306")
    
    Write-Info "=========================================="
    Write-Info "MySQL Port Forward Background ($LocalPort -> 3306)"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Forwarding 0.0.0.0:$LocalPort (Current PC) -> ${ReleaseName}-mysql:3306"
    
    # Start port-forward in background
    $job = Start-Job -ScriptBlock {
        param($kubeconfig, $releaseName, $localPort)
        kubectl port-forward "svc/$releaseName-mysql" "${localPort}:3306" `
            --address 0.0.0.0 `
            --kubeconfig="$kubeconfig"
    } -ArgumentList $KubeconfigFile, $ReleaseName, $LocalPort
    
    Start-Sleep -Seconds 1
    
    Write-Success "Port forward started in background (Job ID: $($job.Id))"
    Write-Info "To stop: Stop-Job -Id $($job.Id); Remove-Job -Id $($job.Id)"
    Write-Info "To list jobs: Get-Job"
}

# Kubectl with kubeconfig
function Invoke-Kubectl {
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    if ($RemainingArgs.Count -eq 0) {
        Write-Warn "No kubectl arguments provided."
        Write-Info "Usage: .\helm-deploy.ps1 kubectl [args...]"
        Write-Info "Example: .\helm-deploy.ps1 kubectl get pods"
        return
    }
    
    $kubectlArgs = $RemainingArgs + @("--kubeconfig=$KubeconfigFile")
    Write-Info "Running: kubectl $($RemainingArgs -join ' ') --kubeconfig=..."
    & kubectl @kubectlArgs
}

# MySQL Port Forward Background Kill
function Invoke-MySQLPortForwardBackgroundKill {
    Write-Info "=========================================="
    Write-Info "MySQL Port Forward Background Kill"
    Write-Info "=========================================="
    
    $jobs = Get-Job | Where-Object { $_.Command -like "*port-forward*mysql*" -or $_.Name -like "*mysql*" }
    
    if ($jobs.Count -eq 0) {
        Write-Warn "No MySQL port-forward jobs found."
        Write-Info "Listing all jobs:"
        Get-Job | Format-Table -AutoSize
        return
    }
    
    foreach ($job in $jobs) {
        Write-Info "Stopping Job ID: $($job.Id), State: $($job.State)"
        Stop-Job -Job $job -ErrorAction SilentlyContinue
        Remove-Job -Job $job -ErrorAction SilentlyContinue
    }
    
    Write-Success "Stopped $($jobs.Count) port-forward job(s)."
}

# Uninstall (delete)
function Invoke-Uninstall {
    Write-Info "=========================================="
    Write-Info "Helm Uninstall Started"
    Write-Info "=========================================="
    
    # Check kubeconfig.yaml exists
    if (-not (Test-Path $KubeconfigFile)) {
        Write-Err "kubeconfig.yaml not found: $KubeconfigFile"
        exit 1
    }
    
    Write-Info "Kubeconfig: $KubeconfigFile"
    Write-Info "Release name: $ReleaseName"
    
    Write-Warn "This will delete release '$ReleaseName'. Continue? (y/N)"
    $confirm = Read-Host
    if ($confirm -ne "y" -and $confirm -ne "Y") {
        Write-Info "Cancelled."
        exit 0
    }
    
    Write-Info "Running Helm Uninstall..."
    helm uninstall $ReleaseName --kubeconfig="$KubeconfigFile"
    if ($LASTEXITCODE -ne 0) { throw "helm uninstall failed" }
    
    Write-Success "=========================================="
    Write-Success "Helm Uninstall Completed!"
    Write-Success "=========================================="
}

# Help
function Show-Help {
    Write-Host @"
Usage: .\helm-deploy.ps1 [action]

Options:
  -Help            Show this help message

Actions:
  install, c       Install new release
  upgrade, u       Upgrade existing release (with pod rollout) [default]
  uninstall, d     Delete release
  logs-app, la     View App (00-monolitic) logs
  logs-migration, lm  View Migration (01-db-migrations) logs
  mysql-mono, mm [port]     Connect to MySQL shell (auto port-forward)
  mysql-mono-portforward, mmpf [port] Port forward MySQL only (default: 13306)
  mysql-mono-portforward-background, mmpfbg [port] Port forward in background
  mysql-mono-portforward-background-kill, mmpfbgkill Stop background port forward
  kubectl [args...]  Run kubectl with kubeconfig

Examples:
  .\helm-deploy.ps1        # Upgrade (default)
  .\helm-deploy.ps1 c      # Install
  .\helm-deploy.ps1 u      # Upgrade
  .\helm-deploy.ps1 d      # Uninstall
  .\helm-deploy.ps1 la     # App logs
  .\helm-deploy.ps1 lm     # Migration logs
  .\helm-deploy.ps1 mm     # MySQL shell (auto port-forward)
  .\helm-deploy.ps1 mmpf   # MySQL port forward only (13306)
  .\helm-deploy.ps1 mmpfbg # MySQL port forward in background
  .\helm-deploy.ps1 mmpfbgkill # Stop background port forward
  .\helm-deploy.ps1 kubectl get pods  # Run kubectl with kubeconfig
  .\helm-deploy.ps1 kubectl exec -it pod-name -- bash
"@
}

# Main
if ($Help) {
    Show-Help
    exit 0
}

if (-not $Action) {
    Write-Info "No action specified. Using default 'upgrade'."
    $Action = "upgrade"
}

switch ($Action) {
    { $_ -in "install", "c" } { Invoke-Install }
    { $_ -in "upgrade", "u" } { Invoke-Upgrade }
    { $_ -in "uninstall", "d" } { Invoke-Uninstall }
    { $_ -in "logs-app", "la" } { Invoke-LogsApp }
    { $_ -in "logs-migration", "lm" } { Invoke-LogsMigration }
    { $_ -in "mysql-mono", "mm" } { Invoke-MySQLPortForward -LocalPort $(if ($RemainingArgs) { $RemainingArgs[0] } else { "13306" }) }
    { $_ -in "mysql-mono-portforward", "mmpf" } { Invoke-MySQLPortForwardOnly -LocalPort $(if ($RemainingArgs) { $RemainingArgs[0] } else { "13306" }) }
    { $_ -in "mysql-mono-portforward-background", "mmpfbg" } { Invoke-MySQLPortForwardBackground -LocalPort $(if ($RemainingArgs) { $RemainingArgs[0] } else { "13306" }) }
    { $_ -in "mysql-mono-portforward-background-kill", "mmpfbgkill" } { Invoke-MySQLPortForwardBackgroundKill }
    "kubectl" { Invoke-Kubectl }
    default {
        Write-Err "Unknown action: $Action"
        Show-Help
        exit 1
    }
}
