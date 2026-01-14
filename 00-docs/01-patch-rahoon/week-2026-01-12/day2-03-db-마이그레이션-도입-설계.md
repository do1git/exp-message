# DB 마이그레이션 도입 설계 - 2026-01-13

## 핵심 결정

- **마이그레이션은 `02-backend/01-db-migrations`로 분리**
- `01-db-migrations`는 **Spring Batch 기반 실행 프로젝트**로 만들고, 그 안에서 **Flyway(SQL)** 로 스키마 버전 관리
  - 배포 단계에서 Job/파이프라인으로 **"1회 실행" 통제**
  - 대규모 데이터 백필은 **Batch Job(청크/재시도/관측)** 로 별도 운영

## 배경 / 목표

- 배경: `ddl-auto=update` 기반은 drift/추적 불가/운영 리스크가 큼
- 목표: 스키마 변경을 Git으로 버전 관리하고, 배포 시점에 예측 가능하게 반영 (`ddl-auto`는 최종적으로 `validate(or none)`)

## 작업 범위

- 서비스: `02-backend/00-monolitic`
- DB: MySQL(운영), H2(테스트)
- 대상: 테이블/인덱스/제약 + (필요 시) 데이터 마이그레이션/백필

## 저장 위치 / 네이밍(Flyway)

- 위치: `02-backend/01-db-migrations/src/main/resources/db/migration/`
- 규칙:
  - `V<YYYYMMDD>_<순번>__<설명>.sql` (예: `V20260113_01__init.sql`)
  - `R__<설명>.sql` (필요 시)
- 가이드: 파일 1개=목적 1개, 대용량 변경은 단계적으로(추가→백필→전환→제거)

## 실행 모델(운영)

- 권장: **배포 단계에서 "마이그레이션 Job" 1회 실행**
  - Helm(훅 Job) 또는 CI/CD가 Job을 생성/실행
  - 앱 부팅/트래픽과 분리되어 안정적
  - 실패 시 배포 단계에서 차단 가능

## 초기(베이스라인)

- **운영 DB가 이미 존재** → `baselineOnMigrate=true` + `baseline-version=20260113.00` 설정 필요
- 현재 스키마를 덤프해서 `V20260113_00__init.sql`로 고정 → 이후부터 Flyway로만 변경

## 설정(목표 상태)

- 현재: `spring.jpa.hibernate.ddl-auto=${DB_DDL_AUTO:update}`
- 목표:
  - 운영/스테이징: `validate(or none)`
  - 로컬: 가급적 `validate`

## 테스트 전략

- PR에서 최소 보장: "빈 DB에 마이그레이션 적용 + 앱 부팅(or JPA validate) 통과"
- 권장: Testcontainers(MySQL) 기반
- `00-monolitic` 테스트 변경:
  - H2 → Testcontainers(MySQL)로 전환
  - `01-db-migrations`의 Flyway 마이그레이션을 참조하여 DB 구축
  - 운영과 동일한 스키마로 테스트 보장

## 롤백 정책

- 운영은 **forward-only**: 문제 발생 시 "되돌리는 새 마이그레이션"으로 복구

## 도입 순서(실행 계획)

1. `01-db-migrations` Spring Batch 프로젝트 생성
2. Flyway 의존성 추가
3. `V20260113_00__init.sql` 생성 (현재 스키마 덤프) + `baseline-version=20260113.00` 설정
4. `00-monolitic` 테스트: H2 → Testcontainers(MySQL) 전환 + `01-db-migrations` 참조
5. 운영/스테이징 `ddl-auto` → `validate`
6. Helm Flyway Job 연동
7. 규칙: 스키마 변경은 "마이그레이션으로만"

## 추후 고려사항

### 백필 배치 작성시 고려할 사항

- **청크 처리**: chunk size 튜닝, cursor vs paging, 메모리 관리
- **재시도/복구**: retry policy, skip policy, 실패 레코드 격리
- **재개(restartability)**: JobRepository 기반 재시작, ExecutionContext 체크포인트
- **병렬 처리**: partitioning, multi-threaded step, async ItemProcessor
- **모니터링**: Spring Batch Admin, 진행률 로깅, 슬랙/알림 연동
- **락/경합 최소화**: 배치 전용 커넥션 풀, 트랜잭션 범위 최소화, 인덱스 비활성화(대량 INSERT 시)
- **롤백 전략**: 부분 커밋, savepoint, 보상 트랜잭션
