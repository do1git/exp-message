# PowerShell script to generate Helm values.yaml from .env file
# Usage: .\generate-values.ps1 [env-file] [output-file]
#   env-file: Path to .env file (default: ..\..\default.env)
#   output-file: Path to output values.yaml (default: values.yaml)

param(
    [string]$EnvFile = "",
    [string]$OutputFile = ""
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if ([string]::IsNullOrEmpty($EnvFile)) {
    $EnvFile = Join-Path $ScriptDir "..\..\default.env"
}

if ([string]::IsNullOrEmpty($OutputFile)) {
    $OutputFile = Join-Path $ScriptDir "values.yaml"
}

if (-not (Test-Path $EnvFile)) {
    Write-Host "Error: .env file not found at $EnvFile" -ForegroundColor Red
    exit 1
}

Write-Host "Reading .env file from: $EnvFile" -ForegroundColor Green
Write-Host "Generating values.yaml at: $OutputFile" -ForegroundColor Green

# Read .env file
$envVars = @{}
Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim()
        if ($key -and $value) {
            $envVars[$key] = $value
        }
    }
}

# Extract image repository and tag
$mysqlImage = $envVars['MYSQL_IMAGE']
$imageParts = $mysqlImage -split ':'
$imageRepo = $imageParts[0]
$imageTag = if ($imageParts.Length -gt 1) { $imageParts[1] } else { "latest" }

# Calculate hostPath: absolute path to mysql-data directory inside all-local
$mysqlDataDir = Join-Path $ScriptDir "mysql-data"
if (-not (Test-Path $mysqlDataDir)) {
    New-Item -ItemType Directory -Path $mysqlDataDir -Force | Out-Null
    Write-Host "Created mysql-data directory at: $mysqlDataDir" -ForegroundColor Green
}
# Convert Windows path to WSL2/Docker Desktop Linux path format
# C:\Users\... -> /host_mnt/c/Users/... (Docker Desktop format)
$absolutePath = (Resolve-Path $mysqlDataDir).Path
if ($absolutePath -match '^([A-Z]):\\(.*)$') {
    $drive = $matches[1].ToLower()
    $path = $matches[2] -replace '\\', '/'
    $hostPath = "/host_mnt/$drive/$path"
} else {
    # Fallback: just convert backslashes to forward slashes
    $hostPath = $absolutePath -replace '\\', '/'
}

# Generate values.yaml
$valuesContent = @"
# Global settings applied to all subcharts
global:
  namespace: default
  # Common labels for all resources
  labels: {}
  # Common annotations for all resources
  annotations: {}

# MySQL subchart configuration
mysql:
  enabled: true
  # Override default values from mysql/values.yaml
  image:
    repository: $imageRepo
    tag: "$imageTag"
    pullPolicy: IfNotPresent
  
  mysql:
    rootPassword: $($envVars['MYSQL_ROOT_PASSWORD'])
    database: $($envVars['MYSQL_DATABASE'])
    user: $($envVars['MYSQL_USER'])
    password: $($envVars['MYSQL_PASSWORD'])
    port: $($envVars['MYSQL_PORT'])
  
  binlog:
    enabled: true
    format: $($envVars['MYSQL_BINLOG_FORMAT'])
    expireLogsDays: $($envVars['MYSQL_BINLOG_EXPIRE_DAYS'])
    maxBinlogSize: $($envVars['MYSQL_BINLOG_MAX_SIZE'])
  
  performance:
    innodbBufferPoolSize: $($envVars['MYSQL_INNODB_BUFFER_POOL_SIZE'])
    innodbLogFileSize: $($envVars['MYSQL_INNODB_LOG_FILE_SIZE'])
  
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  
  persistence:
    enabled: true
    type: "storageClass"
    storageClass: "standard"
    accessMode: ReadWriteOnce
    size: 10Gi
  
  service:
    type: ClusterIP  # 기본값: 클러스터 내부 접근만 허용 (안전)
    port: $($envVars['MYSQL_PORT'])
  
  healthCheck:
    liveness:
      initialDelaySeconds: 30
      periodSeconds: 10
      timeoutSeconds: 5
      failureThreshold: 3
    readiness:
      initialDelaySeconds: 10
      periodSeconds: 5
      timeoutSeconds: 3
      failureThreshold: 3
  
  replicas: 1
  namespace: default

# Redis subchart configuration (to be added)
# redis:
#   enabled: false
#   # Redis configuration here

# Application subchart configuration (to be added)
# application:
#   enabled: false
#   # Application configuration here
"@

$valuesContent | Out-File -FilePath $OutputFile -Encoding UTF8 -NoNewline

Write-Host "values.yaml generated successfully!" -ForegroundColor Green

