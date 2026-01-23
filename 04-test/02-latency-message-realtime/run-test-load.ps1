# 동시 연결 부하 테스트 실행 스크립트 (PowerShell)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Concurrent Load Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 서버 URL (기본값: localhost:8080)
$BASE_URL = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  BASE_URL: $BASE_URL"
Write-Host "  CONNECTION_STEPS: 50, 100, 150, 200, 250, 300" -ForegroundColor Cyan
Write-Host ""

# 서버 연결 확인
Write-Host "Checking server connection..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/actuator/health" -TimeoutSec 5 -UseBasicParsing
    Write-Host "✓ Server is running" -ForegroundColor Green
} catch {
    Write-Host "✗ Server is not running at $BASE_URL" -ForegroundColor Red
    Write-Host "  Please start the backend server first." -ForegroundColor Red
    exit 1
}

Write-Host ""

# node_modules 확인
if (-not (Test-Path "node_modules")) {
    Write-Host "Installing dependencies..." -ForegroundColor Yellow
    npm install
    Write-Host ""
}

# 경고 메시지
Write-Host "⚠️  WARNING:" -ForegroundColor Yellow
Write-Host "  This test will create many connections and may take 10+ minutes." -ForegroundColor Yellow
Write-Host "  Make sure your server has sufficient resources." -ForegroundColor Yellow
Write-Host ""

# 테스트 실행
Write-Host "Starting concurrent load test..." -ForegroundColor Green
Write-Host ""

$env:BASE_URL = $BASE_URL

node test-concurrent-load.js

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " Test completed successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host " Test failed!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    exit 1
}
