# 프로젝트 구조

## 전체 구조

```text
exp-message/
├── frontend/                    # 프론트엔드
│   └── (프레임워크 선정 후 구조 결정)
├── backend/                     # 백엔드 (마이크로서비스)
│   ├── api-gateway/            # API Gateway 서비스
│   ├── user-service/           # 사용자 서비스
│   ├── chat-room-service/      # 채팅방 서비스
│   ├── message-service/        # 메시지 서비스
│   ├── search-service/         # 검색 서비스
│   ├── notification-service/  # 알림 서비스
│   ├── websocket-service/      # WebSocket 서비스
│   └── shared/                 # 공유 라이브러리/유틸리티
│       ├── common/            # 공통 코드
│       ├── proto/             # gRPC 프로토콜 정의 (선택)
│       └── config/            # 공통 설정
├── test/                        # 시스템 레벨 테스트
│   ├── integration-test/      # 통합 테스트 (서비스 간)
│   ├── load-test/             # 부하 테스트
│   └── stress-test/           # 스트레스 테스트
├── infrastructure/              # 인프라 설정
│   ├── k8s/                   # Kubernetes 매니페스트
│   ├── docker/                # Docker 설정
│   ├── kafka/                 # Kafka 설정
│   ├── redis/                 # Redis 설정
│   ├── databases/             # 데이터베이스 설정 및 마이그레이션
│   └── monitoring/            # 모니터링 설정
├── scripts/                     # 스크립트
│   ├── setup.sh               # 초기 설정 스크립트
│   └── deploy.sh              # 배포 스크립트
├── docs/                        # 문서
│   ├── patch/                 # 패치노트
│   ├── architecture.md        # 아키텍처 문서
│   └── project-structure.md   # 프로젝트 구조 문서
└── README.md                    # 프로젝트 메인 README
```

## 프론트엔드 구조

```text
frontend/
├── src/                        # 소스 코드
│   ├── components/            # 컴포넌트
│   ├── pages/                 # 페이지
│   ├── services/              # API 서비스
│   └── utils/                 # 유틸리티
├── public/                     # 정적 파일
├── Dockerfile                  # Docker 이미지 빌드
└── README.md                  # 프론트엔드 README
```

## 백엔드 구조

### 서비스별 구조 (예시)

각 서비스는 독립적으로 배포 가능한 구조:

```text
backend/service-name/
├── src/                        # 소스 코드
│   ├── main.go                # 진입점 (Go 예시)
│   ├── handlers/              # HTTP 핸들러
│   ├── services/              # 비즈니스 로직
│   ├── repositories/          # 데이터 접근 계층
│   ├── models/                # 데이터 모델
│   └── config/                # 설정
├── tests/                      # 단위 테스트 (서비스 내부)
├── Dockerfile                  # Docker 이미지 빌드
├── .env.example               # 환경 변수 예시
└── README.md                  # 서비스별 README
```

### 공유 라이브러리

```text
backend/shared/
├── common/                    # 공통 코드
│   ├── errors/                # 에러 정의
│   ├── logger/                # 로깅 유틸리티
│   └── utils/                 # 유틸리티 함수
├── proto/                     # gRPC 프로토콜 정의 (선택)
└── config/                    # 공통 설정
```

## 테스트 구조

### 시스템 레벨 테스트 (test/)

전체 시스템을 대상으로 하는 테스트:

```text
test/
├── integration-test/         # 통합 테스트 (서비스 간 통신 검증)
│   └── scenarios/            # 통합 테스트 시나리오
├── load-test/                # 부하 테스트
│   ├── k6/                   # k6 스크립트
│   └── jmeter/               # JMeter 테스트 계획
└── stress-test/             # 스트레스 테스트
    └── scenarios/            # 스트레스 테스트 시나리오
```

### 서비스 레벨 테스트

각 서비스의 단위 테스트는 해당 서비스 디렉토리 내부에 위치:

- `backend/service-name/tests/`: 각 서비스의 단위 테스트
- `frontend/tests/`: 프론트엔드 단위 테스트

## 인프라 구조

```text
infrastructure/
├── k8s/                      # Kubernetes 매니페스트 (중앙 집중식)
│   ├── frontend/             # 프론트엔드 배포
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── backend/              # 백엔드 서비스 배포
│   │   ├── api-gateway/
│   │   ├── user-service/
│   │   ├── chat-room-service/
│   │   ├── message-service/
│   │   ├── search-service/
│   │   ├── notification-service/
│   │   └── websocket-service/
│   └── monitoring/           # 모니터링 배포
├── docker/                   # Docker 설정
│   └── docker-compose.yml    # 로컬 개발용
├── kafka/                    # Kafka 설정
├── redis/                    # Redis 설정
├── databases/                # 데이터베이스 설정
│   ├── user-db/              # User Service DB 마이그레이션
│   ├── chat-room-db/         # Chat Room Service DB 마이그레이션
│   ├── message-db/          # Message Service DB 마이그레이션 (샤딩)
│   └── search-index/         # Elasticsearch 인덱스 설정
└── monitoring/               # 모니터링 설정
    ├── prometheus/           # Prometheus 설정
    └── grafana/              # Grafana 설정
```

## 구조 채택 이유

이 프로젝트 구조는 **명확한 역할 분리**와 **관리의 편의성**을 위해 다음과 같은 원칙에 따라 설계되었습니다.

### 1. 역할 기반 분리

- **`frontend/`**: 프론트엔드 애플리케이션을 독립적으로 관리
- **`backend/`**: 모든 마이크로서비스를 한 곳에서 관리하여 서비스 간 관계를 명확히 파악
- **`test/`**: 시스템 레벨 테스트(통합, 부하, 스트레스)를 분리하여 관리
  - 단위 테스트는 각 서비스 내부에 위치하여 서비스 독립성 유지
- **`infrastructure/`**: 인프라 설정을 중앙 집중식으로 관리하여 일관성 유지
- **`scripts/`**: 공통 스크립트를 한 곳에서 관리

### 2. 중앙 집중식 인프라 관리

- **Kubernetes 매니페스트 통합**: 모든 k8s 설정을 `infrastructure/k8s/`에 모아 배포 전략을 한눈에 파악
- **인프라 설정 통합**: Kafka, Redis, 데이터베이스, 모니터링 설정을 한 곳에서 관리하여 환경별 설정 관리 용이

### 3. 마이크로서비스 독립성

- 각 서비스는 독립적인 디렉토리 구조를 가지며, Dockerfile과 환경 설정을 포함
- `backend/shared/`를 통해 공통 코드를 재사용하면서도 서비스 간 결합도 최소화
- 서비스별로 독립적인 배포 및 스케일링 가능

### 4. 확장성과 유지보수성

- 새로운 서비스 추가 시 `backend/` 하위에 디렉토리만 추가하면 됨
- 테스트 전략 확장 시 `test/` 하위에 새로운 테스트 유형 추가
- 인프라 변경 시 `infrastructure/`에서만 수정하면 전체 시스템에 반영

### 5. 개발자 경험 개선

- 프로젝트 루트에서 전체 구조를 한눈에 파악 가능
- 각 디렉토리의 역할이 명확하여 새로운 팀원의 온보딩 용이
- 관련 파일들이 논리적으로 그룹화되어 찾기 쉬움

### 6. 배포 및 운영 효율성

- 인프라 설정이 중앙 집중식으로 관리되어 배포 파이프라인 구축 용이
- 환경별 설정 관리가 체계적
- 모니터링 및 로깅 설정을 한 곳에서 관리

이러한 구조는 **"아주 튼튼한 채팅 서버"**를 구축하기 위한 확장 가능하고 유지보수하기 쉬운 기반을 제공합니다.
