#!/bin/bash

# 모놀리틱 서버 빌드 및 레지스트리 푸시 스크립트

set -e

# 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# .env 파일 로드 (존재하는 경우)
if [ -f "$SCRIPT_DIR/.env" ]; then
    echo "환경 변수 파일 로드: $SCRIPT_DIR/.env"
    set -a
    # Remove CRLF line endings for Windows compatibility
    source <(tr -d '\r' < "$SCRIPT_DIR/.env")
    set +a
elif [ -f "$SCRIPT_DIR/default.env" ]; then
    echo "기본 환경 변수 파일 로드: $SCRIPT_DIR/default.env"
    set -a
    # Remove CRLF line endings for Windows compatibility
    source <(tr -d '\r' < "$SCRIPT_DIR/default.env")
    set +a
fi

# 기본값 설정 (환경 변수 또는 기본값)
REGISTRY_HOST="${REGISTRY_HOST:-localhost}"
REGISTRY_PORT="${REGISTRY_PORT:-5000}"
IMAGE_NAME="${IMAGE_NAME:-00-monolitic}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

# 프로젝트 루트 디렉토리로 이동
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
MONOLITIC_DIR="$PROJECT_ROOT/02-backend/00-monolitic"

echo "=========================================="
echo "모놀리틱 서버 빌드 및 푸시"
echo "=========================================="
echo "레지스트리: $REGISTRY_HOST:$REGISTRY_PORT"
echo "이미지: $IMAGE_NAME:$IMAGE_TAG"
echo "디렉토리: $MONOLITIC_DIR"
echo "=========================================="

# 디렉토리 확인
if [ ! -d "$MONOLITIC_DIR" ]; then
    echo "오류: 모놀리틱 디렉토리를 찾을 수 없습니다: $MONOLITIC_DIR"
    exit 1
fi

# Dockerfile 확인
if [ ! -f "$MONOLITIC_DIR/Dockerfile" ]; then
    echo "오류: Dockerfile을 찾을 수 없습니다: $MONOLITIC_DIR/Dockerfile"
    exit 1
fi

# 이미지 빌드
echo ""
echo "1. Docker 이미지 빌드 중..."
cd "$MONOLITIC_DIR"
docker build -t "$IMAGE_NAME:$IMAGE_TAG" .

# 레지스트리 주소로 태그 지정
REGISTRY_IMAGE="$REGISTRY_HOST:$REGISTRY_PORT/$IMAGE_NAME:$IMAGE_TAG"
echo ""
echo "2. 레지스트리 태그 지정: $REGISTRY_IMAGE"
docker tag "$IMAGE_NAME:$IMAGE_TAG" "$REGISTRY_IMAGE"

# 레지스트리에 푸시
echo ""
echo "3. 레지스트리에 푸시 중..."
docker push "$REGISTRY_IMAGE"

echo ""
echo "=========================================="
echo "완료!"
echo "이미지: $REGISTRY_IMAGE"
echo "=========================================="

