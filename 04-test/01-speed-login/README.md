# 로그인 성능 비교 테스트 (k6)

## 개요

`/auth/login` (락 없음)과 `/auth/login-with-lock` (분산 락) 두 엔드포인트의 성능을 비교하는 k6 부하 테스트입니다.

## 사전 요구사항

### k6 설치

**Windows (Chocolatey):**
```powershell
choco install k6
```

**Windows (winget):**
```powershell
winget install k6
```

**Mac (Homebrew):**
```bash
brew install k6
```

## 테스트 시나리오

### 1. `login-compare-test.js` - 기본 성능 비교

각 VU가 **고유한 계정**으로 로그인 실패를 시도합니다. 순수 엔드포인트 성능을 비교합니다.

```
시나리오 1: /auth/login (20 VU x 20 iterations)
      ↓ 5초 대기
시나리오 2: /auth/login-with-lock (20 VU x 20 iterations)
```

### 2. `login-race-condition-compare.js` - Race Condition 비교 ⭐

20개 VU가 **동일한 계정**으로 동시에 로그인 실패를 시도합니다. Race Condition 방지 효과를 검증합니다.

```
시나리오 1: /auth/login - 동일 계정으로 20개 동시 요청
      ↓ 5초 대기
시나리오 2: /auth/login-with-lock - 동일 계정으로 20개 동시 요청
```

**예상 결과:**

| 항목 | /login (락 없음) | /login-with-lock (락 있음) |
|------|------------------|---------------------------|
| USER_001 (로그인 실패) | 여러 개 (Race Condition) | 최대 1개 |
| LOCKED (잠금) | 적음 | 다수 |

## 실행 방법

### 1. 서버 실행 확인

```bash
# 백엔드 서버가 실행 중인지 확인
curl http://localhost:8080/health
```

### 2. 테스트 실행

**기본 성능 비교:**
```powershell
k6 run -e BASE_URL=http://localhost:8080 login-compare-test.js
```

**Race Condition 비교 (권장):**
```powershell
k6 run -e BASE_URL=http://localhost:8080 login-race-condition-compare.js
```

**결과를 JSON으로 저장:**
```powershell
k6 run --out json=results.json -e BASE_URL=http://localhost:8080 login-race-condition-compare.js
```

### 3. 실행 스크립트 사용

```powershell
.\run-test.ps1 -BaseUrl "http://localhost:8080" -TestScript "login-race-condition-compare.js"
```

## 테스트 결과 해석

### 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `no_lock_duration` | /login 응답 시간 |
| `with_lock_duration` | /login-with-lock 응답 시간 |
| `no_lock_USER001` | 락 없이 로그인 실패 처리 횟수 |
| `with_lock_USER001` | 락 있을 때 로그인 실패 처리 횟수 |
| `*_LOCKED` | 계정 잠금 또는 락 획득 실패 횟수 |

### 결과 예시

**Race Condition 발생 시 (/login):**
```
no_lock_USER001......: 5     ← 5번 실패 처리됨 (중복!)
no_lock_LOCKED.......: 15
```

**Race Condition 방지 시 (/login-with-lock):**
```
with_lock_USER001....: 1     ← 1번만 실패 처리됨 ✓
with_lock_LOCKED.....: 19
```

## 응답 코드 설명

| 코드 | 설명 |
|------|------|
| `USER_001` | 로그인 실패 (비밀번호 오류) |
| `LOGIN_FAILURE_001` | 계정 잠금됨 (5회 실패 초과) |
| `COMMON_001` | 락 획득 실패 (동시 요청 시) |

## 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `BASE_URL` | `http://localhost:8080` | API 서버 주소 |
| `TEST_PASSWORD` | `wrongpassword` | 테스트용 비밀번호 |

## 참고 자료

- [k6 공식 문서](https://k6.io/docs/)
- [k6 Scenarios](https://k6.io/docs/using-k6/scenarios/)
- [k6 Metrics](https://k6.io/docs/using-k6/metrics/)
