# ============================================================
# Login Performance Comparison Test Script (PowerShell)
# ============================================================

param(
    [string]$BaseUrl = "https://message.rahoon.site/api",
    [string]$TestScript = "login-race-condition-compare.js",
    [switch]$SaveJson,
    [switch]$Verbose
)

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  k6 Login Performance Comparison Test" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Check k6 installation
if (!(Get-Command k6 -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] k6 is not installed." -ForegroundColor Red
    Write-Host ""
    Write-Host "Install:" -ForegroundColor Yellow
    Write-Host "  choco install k6" -ForegroundColor White
    Write-Host "  or" -ForegroundColor Gray
    Write-Host "  winget install k6" -ForegroundColor White
    Write-Host ""
    exit 1
}

Write-Host "[OK] k6 version: $(k6 version)" -ForegroundColor Green
Write-Host ""

# Display test settings
Write-Host "[Config]" -ForegroundColor Yellow
Write-Host "  Base URL: $BaseUrl" -ForegroundColor White
Write-Host "  Test Script: $TestScript" -ForegroundColor White
Write-Host ""

# Generate result filename
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultFile = "results-$timestamp.json"

# Run k6
Write-Host "[Start] Running test..." -ForegroundColor Green
Write-Host ""

if ($SaveJson) {
    Write-Host "[Save] Result file: $resultFile" -ForegroundColor Yellow
    k6 run --out json=$resultFile -e BASE_URL=$BaseUrl $TestScript
} else {
    k6 run -e BASE_URL=$BaseUrl $TestScript
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Test Complete" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

if ($SaveJson -and (Test-Path $resultFile)) {
    Write-Host "[Result] File: $resultFile" -ForegroundColor Green
}
