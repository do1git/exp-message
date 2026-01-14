#!/bin/bash

# ============================================================
# 로그인 성능 비교 테스트 실행 스크립트 (Bash)
# ============================================================

# 기본값 설정
BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_SCRIPT="${1:-login-race-condition-compare.js}"
SAVE_JSON="${SAVE_JSON:-false}"

echo ""
echo "============================================================"
echo "  k6 로그인 성능 비교 테스트"
echo "============================================================"
echo ""

# k6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "❌ k6가 설치되어 있지 않습니다."
    echo ""
    echo "설치 방법:"
    echo "  Mac:   brew install k6"
    echo "  Linux: sudo apt-get install k6"
    echo ""
    exit 1
fi

echo "✅ k6 버전: $(k6 version)"
echo ""

# 테스트 설정 출력
echo "📋 테스트 설정:"
echo "  Base URL: $BASE_URL"
echo "  Test Script: $TEST_SCRIPT"
echo ""

# 결과 파일명 생성
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
RESULT_FILE="results-$TIMESTAMP.json"

# k6 실행
echo "🚀 테스트 시작..."
echo ""

if [ "$SAVE_JSON" = "true" ]; then
    echo "📁 결과 저장: $RESULT_FILE"
    k6 run --out json=$RESULT_FILE -e BASE_URL=$BASE_URL $TEST_SCRIPT
else
    k6 run -e BASE_URL=$BASE_URL $TEST_SCRIPT
fi

echo ""
echo "============================================================"
echo "  테스트 완료"
echo "============================================================"
echo ""

if [ "$SAVE_JSON" = "true" ] && [ -f "$RESULT_FILE" ]; then
    echo "📊 결과 파일: $RESULT_FILE"
fi
