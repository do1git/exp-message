# Infrastructure

인프라 설정 및 구성 파일을 관리하는 디렉토리입니다.

## 구조

대부분의 리소스는 Helm 차트로 작성됩니다. (예외: `00-compose-all`은 Docker Compose 사용)

### 디렉토리 구조

- `00-compose-all`: Docker Compose 기반 로컬 통합 환경 설정 
  - Helm 차트가 아닌 docker-compose.yml을 사용
  - 로컬 개발 환경에서 모든 서비스를 통합 실행하기 위한 설정
- `00-stack-all`: 통합 스택 Helm Chart
  - 개별 서비스 Chart들을 통합하는 상위 Chart
  - Ingress 설정 포함
  - 배포 스크립트는 `05-scripts/02-deploy-stack-all`에서 실행
- `01-mysql-mono`: MySQL 서비스 Helm Chart
- `02-app-monolitic`: 모놀리틱 애플리케이션 Helm Chart
- `03-redis-mono`: Redis 서비스 Helm Chart
- `04-batch-db-migration`: DB 마이그레이션 Job Helm Chart
- `05-web-landing`: 랜딩 페이지 Helm Chart (소스: `03-frontend/00-landing`)
