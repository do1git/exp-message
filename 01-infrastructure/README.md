# Infrastructure

인프라 설정 및 구성 파일을 관리하는 디렉토리입니다.

## 디렉토리 구조

### `01-service-resources/`

서비스별 독립적인 인프라 리소스 설정

각 서비스별로 디렉토리를 구성하여 관리합니다:

- **00-monolitic/**: 모놀리식 서비스 리소스 (MySQL 등)
- **01-api-gateway/**: API Gateway 서비스 리소스
- **02-user-service/**: 사용자 서비스 리소스
- **03-chat-room-service/**: 채팅방 서비스 리소스
- **04-message-service/**: 메시지 서비스 리소스
- **05-search-service/**: 검색 서비스 리소스
- **06-notification-service/**: 알림 서비스 리소스
- **07-websocket-service/**: WebSocket 서비스 리소스

각 서비스 디렉토리 내부 구조는 필요에 따라 서비스별로 정의합니다.

### `02-inter-service-resources/`

서비스 간 통신(Pub/Sub)을 위한 공유 인프라 리소스 설정

- **kafka/**: Kafka 설정 파일
  - Kafka 브로커 설정
  - Topic 정의
  - Consumer/Producer 설정
  - 서비스 간 이벤트 기반 통신 및 메시지 브로커

### `03-deployment/`

배포 및 오케스트레이션 설정

- **docker-compose/**: Docker Compose 설정 파일
  - docker-compose.yml (로컬 개발 환경)
  - 환경별 docker-compose 파일 (필요 시)
- **k8s/**: Kubernetes 매니페스트 파일
  - Helm 차트를 사용하여 배포 관리
  - 각 서비스별 Helm 차트 정의
  - 환경별 values 파일 관리 (dev, staging, prod)
  - Deployment, Service, ConfigMap, Secret 등
  - 네임스페이스별 구성
  - ArgoCD를 통한 GitOps 자동화 배포는 추후 고려 예정

### `04-monitoring/`

모니터링 설정

- Prometheus 설정
- Grafana 대시보드 및 데이터 소스 설정
- Loki 설정
- AlertManager 설정 (필요 시)

## 구조 설계 원칙

1. **서비스별 리소스 분리**: 각 서비스가 독립적으로 사용하는 리소스(Database, Redis 등)를 서비스별 디렉토리로 관리
   - `01-service-resources/{service-name}/` 구조로 각 서비스의 인프라 리소스를 독립적으로 관리
   - Database per Service 패턴에 따라 각 서비스의 데이터베이스는 해당 서비스 디렉토리에 포함
2. **서비스 간 통신 리소스 분리**: 서비스 간 Pub/Sub 통신을 위한 공유 인프라(Kafka)를 별도로 관리
3. **배포 스크립트 통합**: 배포 관련 설정(docker-compose, k8s)을 한 곳에서 관리
4. **모니터링 독립**: 모니터링 설정을 별도로 분리하여 관리

> **참고**:
>
> - 프로덕션 환경의 데이터베이스 설정은 Kubernetes ConfigMap/Secret 또는 별도의 인프라 관리 도구를 통해 관리합니다.
> - Dockerfile은 애플리케이션 이미지 빌드용이며, DB 초기화는 docker-compose의 volume mount로 처리합니다.

## 사용 방법

각 디렉토리의 설정 파일을 참고하여 인프라를 구성하세요.
