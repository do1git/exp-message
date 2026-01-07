# PowerShell script to generate docker-compose.yml from .env file
# Usage: .\generate-docker-compose.ps1

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir "..\.env"
$OutputFile = Join-Path $ScriptDir "docker-compose.yml"

if (-not (Test-Path $EnvFile)) {
    Write-Host "Error: .env file not found at $EnvFile" -ForegroundColor Red
    exit 1
}

Write-Host "Reading .env file from: $EnvFile" -ForegroundColor Green
Write-Host "Generating docker-compose.yml at: $OutputFile" -ForegroundColor Green

# Create required directories
$MysqlDir = Join-Path $ScriptDir "mysql"
$DataDir = Join-Path $MysqlDir "data"
$ConfDDir = Join-Path $MysqlDir "conf.d"
$InitDir = Join-Path $MysqlDir "init"

New-Item -ItemType Directory -Path $DataDir -Force | Out-Null
New-Item -ItemType Directory -Path $ConfDDir -Force | Out-Null
New-Item -ItemType Directory -Path $InitDir -Force | Out-Null

Write-Host "Created directories: $DataDir, $ConfDDir, $InitDir" -ForegroundColor Green

# Create default my.cnf file if it doesn't exist
$MyCnfFile = Join-Path $ConfDDir "my.cnf"
if (-not (Test-Path $MyCnfFile)) {
    $myCnfContent = @"
[mysqld]
# Binlog configuration
log-bin=mysql-bin
binlog_format=ROW
expire_logs_days=7
max_binlog_size=100M
binlog_cache_size=1M
max_binlog_cache_size=2M

# Server ID (required for replication)
server-id=1

# Performance settings
innodb_buffer_pool_size=256M
innodb_log_file_size=64M

# Character set configuration
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

[mysql]
default-character-set=utf8mb4

[client]
default-character-set=utf8mb4
"@
    $myCnfContent | Out-File -FilePath $MyCnfFile -Encoding UTF8 -NoNewline
    Write-Host "Created default my.cnf file: $MyCnfFile" -ForegroundColor Green
}

# Create .gitkeep file for init directory
$GitkeepFile = Join-Path $InitDir ".gitkeep"
if (-not (Test-Path $GitkeepFile)) {
    New-Item -ItemType File -Path $GitkeepFile -Force | Out-Null
    Write-Host "Created .gitkeep file: $GitkeepFile" -ForegroundColor Green
}

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

# Set default values
$mysqlImage = $envVars['MYSQL_IMAGE']
$containerName = $envVars['MYSQL_CONTAINER_NAME']
$rootPassword = $envVars['MYSQL_ROOT_PASSWORD']
$database = $envVars['MYSQL_DATABASE']
$user = $envVars['MYSQL_USER']
$password = $envVars['MYSQL_PASSWORD']
$hostPort = $envVars['MYSQL_HOST_PORT']
$port = $envVars['MYSQL_PORT']
$healthcheckInterval = $envVars['MYSQL_HEALTHCHECK_INTERVAL']
$healthcheckTimeout = $envVars['MYSQL_HEALTHCHECK_TIMEOUT']
$healthcheckRetries = $envVars['MYSQL_HEALTHCHECK_RETRIES']

# Generate docker-compose.yml
$dockerComposeContent = @"
services:
  mysql:
    image: `${MYSQL_IMAGE:-$mysqlImage}
    container_name: `${MYSQL_CONTAINER_NAME:-$containerName}
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: `${MYSQL_ROOT_PASSWORD:-$rootPassword}
      MYSQL_DATABASE: `${MYSQL_DATABASE:-$database}
      MYSQL_USER: `${MYSQL_USER:-$user}
      MYSQL_PASSWORD: `${MYSQL_PASSWORD:-$password}
    ports:
      - "`${MYSQL_HOST_PORT:-$hostPort}:`${MYSQL_PORT:-$port}"
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf.d:/etc/mysql/conf.d
      - ./mysql/init:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p`${MYSQL_ROOT_PASSWORD:-$rootPassword}"]
      interval: `${MYSQL_HEALTHCHECK_INTERVAL:-$healthcheckInterval}
      timeout: `${MYSQL_HEALTHCHECK_TIMEOUT:-$healthcheckTimeout}
      retries: `${MYSQL_HEALTHCHECK_RETRIES:-$healthcheckRetries}

"@

$dockerComposeContent | Out-File -FilePath $OutputFile -Encoding UTF8 -NoNewline

Write-Host "docker-compose.yml generated successfully!" -ForegroundColor Green

