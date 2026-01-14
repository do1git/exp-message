# 패치노트 - 2026-01-14

## 목표했던 내용 1

- 로그인 실패 추적에 분산 락(Distributed Lock) 방식 도입
- day2-02에서 분석한 분산 락 방식 실제 구현 및 성능 검증

## 변경사항 1

### 분산 락 구현

- `LockRepository` 인터페이스 및 `LockRepositoryImpl` (Redis 구현체) 추가
  - `acquireLock(key: String, ttl: Duration): LockToken?` - 락 획득
  - `acquireLocks(keys: List<String>, ttl: Duration): LockToken?` - 다중 키 락 획득
  - `releaseLock(token: LockToken): Boolean` - 락 해제 (토큰 기반, 자신의 락만 해제 가능)
- `Lock` 유틸리티 클래스로 편리한 락 사용 지원
- Redis Lua 스크립트를 통한 원자적 락 획득/해제 구현
  - email, ipAddress 기반 이중 락 적용

### 로그인 플로우 수정

- `AuthTokenApplicationService.login()` 분산 락 적용
  1. `checkAndThrowIfLocked()`: 잠금 상태 확인
  2. `acquireLock()`: 락 획득 시도 (실패 시 즉시 차단)
  3. `checkAndThrowIfLocked()`: 락 획득 후 재확인
  4. `getUser()`: 사용자 조회
  5. (실패 시) `incrementFailureCount()`: 실패 카운트 증가
  6. (성공 시) `resetFailureCount()`: 실패 카운트 초기화
  7. `releaseLock()`: 락 해제

## 테스트 결과

- TODO: 성능 테스트 결과 작성

## 성능 비교

| 항목 | 재확인 방식 (기존) | 분산 락 방식 (신규) |
|------|-------------------|---------------------|
| BF 공격 차단 시간 | 40ms | 20ms |
| MySQL 조회 | 항상 수행 | 락 실패 시 생략 |
| 구현 복잡도 | 낮음 | 높음 |

## 피드백

### 잘한 점

- TODO

### 아쉬운 점

- TODO
