# Development Environment / 개발 환경

Run MySQL and Redis for local development. / 로컬 개발을 위한 MySQL과 Redis 실행.

## Quick Start / 빠른 시작

```bash
# Linux/Mac
./dev-compose.sh

# Windows
.\dev-compose.ps1
```

## Setup / 설정

**First time only / 최초 1회:**

```bash
# Copy default.env to .env (auto-created on first run)
# default.env를 .env로 복사 (첫 실행 시 자동 생성됨)
cp default.env .env
```

Edit `.env` if needed. / 필요시 `.env` 수정.

## Usage / 사용법

### Linux/Mac

```bash
./dev-compose.sh           # Start / 시작
./dev-compose.sh down      # Stop / 중지
./dev-compose.sh ps        # Status / 상태
./dev-compose.sh logs      # Logs / 로그
./dev-compose.sh logs mysql # MySQL logs / MySQL 로그
```

### Windows

```powershell
.\dev-compose.ps1           # Start / 시작
.\dev-compose.ps1 down      # Stop / 중지
.\dev-compose.ps1 ps        # Status / 상태
.\dev-compose.ps1 logs      # Logs / 로그
.\dev-compose.ps1 logs mysql # MySQL logs / MySQL 로그
```

## Services / 서비스

- **MySQL**: `localhost:3306`
- **Redis**: `localhost:6379`

## Run Application / 애플리케이션 실행

After starting services / 서비스 시작 후:

```bash
cd ../../02-backend/00-monolitic
./gradlew bootRun
```

Application connects to Docker MySQL/Redis. / 애플리케이션이 Docker MySQL/Redis에 연결됨.
