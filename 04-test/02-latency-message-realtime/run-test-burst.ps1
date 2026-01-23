# Burst Latency 측정 테스트 실행 스크립트 (PowerShell)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Realtime Message Burst Latency Test" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 서버 URL (기본값: localhost:8080)
$BASE_URL = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8080" }

# 구독자 수 (기본값: 10)
$NUM_SUBSCRIBERS = if ($env:NUM_SUBSCRIBERS) { $env:NUM_SUBSCRIBERS } else { "10" }

# 반복 횟수 (기본값: 20)
$NUM_ITERATIONS = if ($env:NUM_ITERATIONS) { $env:NUM_ITERATIONS } else { "20" }

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  BASE_URL: $BASE_URL"
Write-Host "  NUM_SUBSCRIBERS: $NUM_SUBSCRIBERS"
Write-Host "  NUM_ITERATIONS: $NUM_ITERATIONS"
Write-Host "  MESSAGES_PER_BURST: 3" -ForegroundColor Cyan
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

# 테스트 실행
Write-Host "Starting burst latency test..." -ForegroundColor Green
Write-Host "  (Measuring time to receive 3 consecutive messages)" -ForegroundColor Gray
Write-Host ""

$env:BASE_URL = $BASE_URL
$env:NUM_SUBSCRIBERS = $NUM_SUBSCRIBERS
$env:NUM_ITERATIONS = $NUM_ITERATIONS

node test-latency-burst.js

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
