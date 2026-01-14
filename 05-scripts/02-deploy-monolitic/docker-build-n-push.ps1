# =============================================================================
# Docker Image Build and Push Script (PowerShell)
# Reads values.yaml and builds/pushes each image to its registry.
# =============================================================================

param(
    [switch]$Help
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ValuesFile = Join-Path $ScriptDir "values.yaml"
$BackendDir = Join-Path $ScriptDir "..\..\02-backend"

# Colored output functions
function Write-Info { param($Message) Write-Host "[INFO] $Message" -ForegroundColor Blue }
function Write-Success { param($Message) Write-Host "[SUCCESS] $Message" -ForegroundColor Green }
function Write-Warn { param($Message) Write-Host "[WARN] $Message" -ForegroundColor Yellow }
function Write-Err { param($Message) Write-Host "[ERROR] $Message" -ForegroundColor Red }

# Extract value from values.yaml
function Get-YamlValue {
    param(
        [string]$KeyPath,
        [string]$FilePath
    )
    
    $content = Get-Content $FilePath -Raw
    $lines = $content -split "`n"
    
    $parts = $KeyPath -split "\."
    $section = $parts[0]
    $key1 = if ($parts.Length -gt 1) { $parts[1] } else { "" }
    $key2 = if ($parts.Length -gt 2) { $parts[2] } else { "" }
    
    $inSection = $false
    $inSubSection = $false
    
    foreach ($line in $lines) {
        # Detect section start
        if ($line -match "^${section}:") {
            $inSection = $true
            continue
        }
        
        # Exit when another top-level section starts
        if ($inSection -and $line -match "^[a-zA-Z]" -and $line -notmatch "^\s") {
            $inSection = $false
        }
        
        if ($inSection) {
            # Detect subsection (e.g., image:)
            if ($key1 -and $line -match "^\s+${key1}:") {
                $inSubSection = $true
                continue
            }
            
            # Exit subsection when another starts
            if ($inSubSection -and $line -match "^\s{2}[a-zA-Z]" -and $line -notmatch "^\s{4}") {
                $inSubSection = $false
            }
            
            # Find value
            if ($inSubSection -and $key2 -and $line -match "^\s+${key2}:\s*(.+)") {
                $value = $Matches[1].Trim()
                $value = $value -replace '\s*#.*$', ''  # Remove comments
                $value = $value -replace '^["'']|["'']$', ''  # Remove quotes
                return $value
            }
        }
    }
    
    return ""
}

# Build and push script block (for parallel execution with log file)
$BuildScript = {
    param(
        [string]$Name,
        [string]$Registry,
        [string]$Repository,
        [string]$Tag,
        [string]$SourceDir,
        [string]$LogFile
    )
    
    # Helper to write to log file (with shared access)
    function Write-Log {
        param($Message)
        $stream = [System.IO.File]::Open($LogFile, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
        $writer = [System.IO.StreamWriter]::new($stream)
        $writer.WriteLine($Message)
        $writer.Close()
        $stream.Close()
    }
    
    if (-not $Registry -or -not $Repository) {
        Write-Log "[WARN] [$Name] Registry or repository not configured. Skipping."
        return 0
    }
    
    if (-not (Test-Path $SourceDir)) {
        Write-Log "[WARN] [$Name] Source directory does not exist: $SourceDir. Skipping."
        return 0
    }
    
    $fullImage = "${Registry}/${Repository}:${Tag}"
    
    Write-Log "[INFO] [$Name] Starting build: $fullImage"
    Write-Log "[INFO] [$Name] Source directory: $SourceDir"
    
    try {
        # Build (--progress=plain for real-time output)
        Write-Log "[INFO] [$Name] Running docker build..."
        & docker build --progress=plain -t "${Repository}:${Tag}" $SourceDir 2>&1 | ForEach-Object { Write-Log $_ }
        if ($LASTEXITCODE -ne 0) { throw "Docker build failed" }
        
        # Tag
        docker tag "${Repository}:${Tag}" $fullImage 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "Docker tag failed" }
        
        # Push
        Write-Log "[INFO] [$Name] Pushing image: $fullImage"
        & docker push $fullImage 2>&1 | ForEach-Object { Write-Log $_ }
        if ($LASTEXITCODE -ne 0) { throw "Docker push failed" }
        
        Write-Log "[SUCCESS] [$Name] Completed: $fullImage"
        return 0
    } catch {
        Write-Log "[ERROR] [$Name] Failed: $_"
        return 1
    }
}

# Help
if ($Help) {
    Write-Host @"
Usage: .\docker-build-n-push.ps1 [options]

Options:
  -Help    Show this help message

Description:
  Reads image settings from values.yaml and builds/pushes
  each image to its configured registry.

Prerequisites:
  - Docker must be installed
  - values.yaml file must exist
  - Registry authentication must be configured
"@
    exit 0
}

# Main execution
function Main {
    Write-Info "=========================================="
    Write-Info "Docker Build and Push Script (Parallel)"
    Write-Info "=========================================="
    
    # Check values.yaml exists
    if (-not (Test-Path $ValuesFile)) {
        Write-Err "values.yaml not found: $ValuesFile"
        Write-Info "Copy values.yaml.example to values.yaml first."
        exit 1
    }
    
    Write-Info "Config file: $ValuesFile"
    Write-Host ""
    
    # Extract image settings
    $appRegistry = Get-YamlValue -KeyPath "app-monolitic.image.registry" -FilePath $ValuesFile
    $appRepository = Get-YamlValue -KeyPath "app-monolitic.image.repository" -FilePath $ValuesFile
    $appTag = Get-YamlValue -KeyPath "app-monolitic.image.tag" -FilePath $ValuesFile
    if (-not $appTag) { $appTag = "latest" }
    $appSourceDir = Join-Path $BackendDir "00-monolitic"
    
    $migRegistry = Get-YamlValue -KeyPath "batch-db-migration.image.registry" -FilePath $ValuesFile
    $migRepository = Get-YamlValue -KeyPath "batch-db-migration.image.repository" -FilePath $ValuesFile
    $migTag = Get-YamlValue -KeyPath "batch-db-migration.image.tag" -FilePath $ValuesFile
    if (-not $migTag) { $migTag = "latest" }
    $migSourceDir = Join-Path $BackendDir "01-db-migrations"
    
    # Display parsed image settings
    Write-Info "Image configurations:"
    Write-Info "  [app]       ${appRegistry}/${appRepository}:${appTag}"
    Write-Info "  [migration] ${migRegistry}/${migRepository}:${migTag}"
    Write-Host ""
    
    # Create log files in .log directory
    $logDir = Join-Path $ScriptDir ".log"
    if (-not (Test-Path $logDir)) { New-Item -Path $logDir -ItemType Directory -Force | Out-Null }
    $appLogFile = Join-Path $logDir "docker-build-n-push-app.log"
    $migLogFile = Join-Path $logDir "docker-build-n-push-migration.log"
    New-Item -Path $appLogFile -ItemType File -Force | Out-Null
    New-Item -Path $migLogFile -ItemType File -Force | Out-Null
    
    Write-Info "Starting parallel builds..."
    Write-Host ""
    
    # Show log file paths for real-time monitoring
    Write-Info "Log files (use 'Get-Content -Wait <path>' to monitor):"
    Write-Info "  [app]       $appLogFile"
    Write-Info "  [migration] $migLogFile"
    Write-Host ""
    
    # Run parallel builds
    $appJob = Start-Job -ScriptBlock $BuildScript -ArgumentList "app", $appRegistry, $appRepository, $appTag, $appSourceDir, $appLogFile
    $migJob = Start-Job -ScriptBlock $BuildScript -ArgumentList "migration", $migRegistry, $migRepository, $migTag, $migSourceDir, $migLogFile
    
    Write-Info "[app] Building... (JobId: $($appJob.Id))"
    Write-Info "[migration] Building... (JobId: $($migJob.Id))"
    Write-Host ""
    
    # Read log files with shared access to avoid file locking
    function Read-LastLine {
        param($Path)
        try {
            if (-not (Test-Path $Path)) { return $null }
            $stream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $reader = [System.IO.StreamReader]::new($stream)
            $content = $reader.ReadToEnd()
            $reader.Close()
            $stream.Close()
            $lines = $content -split "`r?`n" | Where-Object { $_ -ne "" }
            if ($lines.Count -gt 0) { return $lines[-1] }
            return $null
        } catch { return $null }
    }
    
    # Monitor progress while waiting
    Write-Info "Progress (updates every 3 seconds):"
    while (($appJob.State -eq "Running") -or ($migJob.State -eq "Running")) {
        $appStatus = "waiting..."
        $migStatus = "waiting..."
        
        $appLastLine = Read-LastLine $appLogFile
        if ($appLastLine) { $appStatus = $appLastLine.Substring(0, [Math]::Min(60, $appLastLine.Length)) }
        
        $migLastLine = Read-LastLine $migLogFile
        if ($migLastLine) { $migStatus = $migLastLine.Substring(0, [Math]::Min(60, $migLastLine.Length)) }
        
        $appState = if ($appJob.State -eq "Running") { "running" } else { "done" }
        $migState = if ($migJob.State -eq "Running") { "running" } else { "done" }
        
        Write-Host "  [app] ($appState) $appStatus" -ForegroundColor Blue
        Write-Host "  [mig] ($migState) $migStatus" -ForegroundColor Blue
        Write-Host ""
        
        Start-Sleep -Seconds 3
    }
    
    # Wait for jobs to complete and get exit codes
    $appJob, $migJob | Wait-Job | Out-Null
    $appExitCode = Receive-Job -Job $appJob
    $migExitCode = Receive-Job -Job $migJob
    Remove-Job -Job $appJob, $migJob
    
    # Check final result
    if ($appExitCode -ne 0 -or $migExitCode -ne 0) {
        Write-Err "=========================================="
        Write-Err "Some builds failed!"
        if ($appExitCode -ne 0) { Write-Err "  - app build failed" }
        if ($migExitCode -ne 0) { Write-Err "  - migration build failed" }
        Write-Err "=========================================="
        exit 1
    }
    
    Write-Success "=========================================="
    Write-Success "All images built and pushed successfully!"
    Write-Success "=========================================="
}

Main
