# Flyway 마이그레이션 추가 (2026-01-14)

## 📋 주요 작업

1. Flyway 도입
2. 테스트코드 개선 - Testcontainers, MockK 도입, Suffix 컨벤션, 테스트 분리
3. Flyway 인프라 구축
4. Deploy 스크립트 작성 및 개선

---

## 🗄️ 1. Flyway 도입

**DB 마이그레이션 도구 Flyway를 도입하여 스키마 버전 관리를 시작했습니다.**

- `01-db-migrations` 모듈 신규 생성
- 마이그레이션 파일 네이밍: `V{날짜}_{순번}__{설명}.sql`
- Spring Boot + Flyway 자동 마이그레이션 실행

```sql
-- 예시: V20260114_01__create_flyway_test_table.sql
CREATE TABLE flyway_test (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
```

---

## 🧪 2. 테스트코드 개선

### Testcontainers 도입
- **MySQL Container**: 실제 DB 환경에서 통합 테스트
- **Redis Container**: 캐시/세션 테스트
- **Singleton Container Pattern**: 테스트 간 컨테이너 재사용으로 속도 향상
- 마이그레이션 컨테이너 자동 실행 (통합 테스트 시)

### MockK 도입
- Kotlin 친화적 Mocking 프레임워크
- `mockk()`, `every {}`, `verify {}` 활용

### 테스트 Suffix 컨벤션 도입

| Suffix | 설명 | 실행 명령 |
|--------|------|-----------|
| `*UT.kt` | Unit Test (단위 테스트) | `./gradlew unitTest` |
| `*IT.kt` | Integration Test (통합 테스트) | `./gradlew integrationTest` |

### 테스트 인프라 클래스
- `@IntegrationTest`: 통합 테스트 마커 어노테이션
- `IntegrationTestBase`: Testcontainers + 마이그레이션 자동 설정

---

## 🐳 3. Flyway 인프라 구축

### Docker 이미지
- `01-db-migrations/Dockerfile`: 멀티스테이지 빌드
- SQL 파일 변경만 있을 시 캐시 활용 최적화
- Helm chart `batch-db-migration` 추가

### Kubernetes Job
- 앱 배포 전 마이그레이션 Job 실행
- DB 스키마 자동 동기화

---

## 🚀 4. Deploy 스크립트 작성 및 개선

### `docker-build-n-push.ps1`
- App + Migration 이미지 **병렬 빌드**
- 로그 파일 분리 (`.log/docker-build-n-push-*.log`)
- 실시간 진행 상황 모니터링

### `helm-deploy.ps1`

| 명령 | 설명 |
|------|------|
| `.\helm-deploy.ps1 c` | Install (신규 배포) |
| `.\helm-deploy.ps1 u` | Upgrade (업그레이드) |
| `.\helm-deploy.ps1 d` | Uninstall (삭제) |
| `.\helm-deploy.ps1 la` | App 로그 보기 |
| `.\helm-deploy.ps1 lm` | Migration 로그 보기 |
| `.\helm-deploy.ps1 mm` | MySQL 셸 접속 |
| `.\helm-deploy.ps1 kubectl [args]` | kubeconfig 자동 적용 kubectl |

---

## 📁 변경된 파일 구조

```
02-backend/
├── 00-monolitic/
│   └── src/test/kotlin/
│       └── common/test/
│           ├── TestAnnotations.kt     # @IntegrationTest
│           ├── IntegrationTestBase.kt # Testcontainers 설정
│           └── TestUtils.kt
│       └── **/*IT.kt                  # 통합 테스트
│       └── **/*UT.kt                  # 단위 테스트
└── 01-db-migrations/                  # 신규 모듈
    ├── Dockerfile
    └── src/main/resources/db/migration/
        └── V*.sql

05-scripts/02-deploy-monolitic/
├── docker-build-n-push.ps1            # 병렬 빌드
├── helm-deploy.ps1                    # 통합 배포 CLI
└── charts/
    └── batch-db-migration-0.1.0.tgz   # 마이그레이션 차트
```
