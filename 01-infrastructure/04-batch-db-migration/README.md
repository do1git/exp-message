# DB Migration Batch Job Helm Chart

Spring Batch + Flyway 기반 DB 마이그레이션 Job을 실행하는 Helm 차트입니다.

## 개요

- **목적**: 스키마 변경을 Git으로 버전 관리하고, 배포 시점에 예측 가능하게 반영
- **실행 방식**: Kubernetes Job으로 1회 실행
- **마이그레이션 도구**: Flyway (SQL 기반)

## Helm Hook 동작

기본적으로 Helm hook으로 설정되어 있어서 `helm install` 또는 `helm upgrade` 시 **앱 배포 전에** 자동 실행됩니다.

```
helm install → [Migration Job 실행] → [앱 Deployment 배포]
```

### Hook 비활성화

수동으로 Job을 실행하려면 `job.hookEnabled: false`로 설정:

```yaml
job:
  hookEnabled: false
```

## 설치

### 단독 설치

```bash
helm install my-migration ./04-batch-db-migration \
  --set database.host=mysql \
  --set database.name=message_db \
  --set database.username=message_user \
  --set database.password=message_password
```

### 상위 차트에서 의존성으로 사용

```yaml
# Chart.yaml
dependencies:
  - name: batch-db-migration
    version: 0.1.0
    repository: file://../01-infrastructure/04-batch-db-migration
    condition: batch-db-migration.enabled
```

## 주요 설정

| 항목 | 기본값 | 설명 |
|------|--------|------|
| `job.hookEnabled` | `true` | Helm hook으로 실행 여부 |
| `job.backoffLimit` | `3` | 재시도 횟수 |
| `job.activeDeadlineSeconds` | `600` | 최대 실행 시간 (10분) |
| `flyway.baselineOnMigrate` | `true` | 기존 DB에 baseline 적용 |
| `flyway.baselineVersion` | `20260113.00` | Baseline 버전 |

## Flyway 설정

기존 운영 DB가 있는 경우 `baselineOnMigrate=true`를 설정하여 현재 스키마를 baseline으로 지정합니다.

```yaml
flyway:
  baselineOnMigrate: true
  baselineVersion: "20260113.00"
  locations: "classpath:db/migration"
```

## 롤백 정책

운영 환경에서는 **forward-only** 정책을 권장합니다:
- 문제 발생 시 "되돌리는 새 마이그레이션"으로 복구
- Flyway `undo` 명령은 사용하지 않음
