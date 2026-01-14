import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ============================================================
// 환경 변수 설정
// ============================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'wrongpassword';

// ============================================================
// 커스텀 메트릭 - /login (락 없음)
// ============================================================
const noLock_USER001 = new Counter('no_lock_USER001');          // 로그인 실패
const noLock_LOCKED = new Counter('no_lock_LOCKED');            // 계정 잠금
const noLock_OTHER = new Counter('no_lock_OTHER');              // 기타
const noLock_Duration = new Trend('no_lock_duration', true);

// ============================================================
// 커스텀 메트릭 - /login-with-lock (락 있음)
// ============================================================
const withLock_USER001 = new Counter('with_lock_USER001');      // 로그인 실패
const withLock_LOCKED = new Counter('with_lock_LOCKED');        // 계정 잠금 또는 락 획득 실패
const withLock_OTHER = new Counter('with_lock_OTHER');          // 기타
const withLock_Duration = new Trend('with_lock_duration', true);

// ============================================================
// 테스트 옵션 - 동일 계정 Race Condition 테스트
// ============================================================
export const options = {
  scenarios: {
    // 시나리오 1: /login (락 없음) - 동일 계정으로 동시 공격
    race_no_lock: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      exec: 'testRaceNoLock',
      tags: { scenario: 'no_lock' },
    },
    // 시나리오 2: /login-with-lock (락 있음) - 동일 계정으로 동시 공격
    race_with_lock: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      startTime: '35s',
      exec: 'testRaceWithLock',
      tags: { scenario: 'with_lock' },
    },
  },
  thresholds: {
    'no_lock_duration': ['p(95)<5000'],
    'with_lock_duration': ['p(95)<5000'],
  },
};

// ============================================================
// Setup
// ============================================================
export function setup() {
  // 각 시나리오마다 고유한 이메일/IP 생성
  const timestamp = Date.now();
  const noLockEmail = `race-no-lock-${timestamp}@test.com`;
  const noLockIp = `10.1.1.${timestamp % 255}`;
  const withLockEmail = `race-with-lock-${timestamp}@test.com`;
  const withLockIp = `10.2.2.${timestamp % 255}`;

  console.log(`\n${'='.repeat(70)}`);
  console.log('🏁 Race Condition 비교 테스트: /login vs /login-with-lock');
  console.log(`${'='.repeat(70)}`);
  console.log(`📍 Base URL: ${BASE_URL}`);
  console.log(`\n📋 시나리오 1 - /login (락 없음):`);
  console.log(`   Email: ${noLockEmail}`);
  console.log(`   IP: ${noLockIp}`);
  console.log(`\n📋 시나리오 2 - /login-with-lock (락 있음):`);
  console.log(`   Email: ${withLockEmail}`);
  console.log(`   IP: ${withLockIp}`);
  console.log(`\n👥 각 시나리오: 20 VU가 동일 계정으로 동시 공격`);
  console.log(`${'='.repeat(70)}\n`);

  return {
    baseUrl: BASE_URL,
    password: TEST_PASSWORD,
    noLockEmail: noLockEmail,
    noLockIp: noLockIp,
    withLockEmail: withLockEmail,
    withLockIp: withLockIp,
  };
}

// ============================================================
// 시나리오 1: /login (락 없음) - 동일 계정 동시 공격
// ============================================================
export function testRaceNoLock(data) {
  const payload = JSON.stringify({
    email: data.noLockEmail,
    password: data.password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': data.noLockIp,
    },
  };

  const startTime = Date.now();
  const response = http.post(`${data.baseUrl}/auth/login`, payload, params);
  const duration = Date.now() - startTime;

  noLock_Duration.add(duration);

  const errorCode = extractErrorCode(response);
  recordMetrics(errorCode, noLock_USER001, noLock_LOCKED, noLock_OTHER);

  console.log(`[NO-LOCK] VU=${__VU}, iter=${__ITER}, duration=${duration}ms, code=${errorCode}`);

  check(response, {
    '[NO-LOCK] valid response': (r) => r.status >= 200 && r.status < 500,
  });
}

// ============================================================
// 시나리오 2: /login-with-lock (락 있음) - 동일 계정 동시 공격
// ============================================================
export function testRaceWithLock(data) {
  const payload = JSON.stringify({
    email: data.withLockEmail,
    password: data.password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': data.withLockIp,
    },
  };

  const startTime = Date.now();
  const response = http.post(`${data.baseUrl}/auth/login-with-lock`, payload, params);
  const duration = Date.now() - startTime;

  withLock_Duration.add(duration);

  const errorCode = extractErrorCode(response);
  recordMetrics(errorCode, withLock_USER001, withLock_LOCKED, withLock_OTHER);

  console.log(`[WITH-LOCK] VU=${__VU}, iter=${__ITER}, duration=${duration}ms, code=${errorCode}`);

  check(response, {
    '[WITH-LOCK] valid response': (r) => r.status >= 200 && r.status < 500,
  });
}

// ============================================================
// 유틸리티 함수
// ============================================================
function extractErrorCode(response) {
  try {
    const body = JSON.parse(response.body);
    if (body.success === true) return 'SUCCESS';
    return body.error?.code || 'UNKNOWN';
  } catch (e) {
    return 'PARSE_ERROR';
  }
}

function recordMetrics(errorCode, user001Counter, lockedCounter, otherCounter) {
  switch (errorCode) {
    case 'USER_001':
      user001Counter.add(1);
      break;
    case 'LOGIN_FAILURE_001':
    case 'COMMON_001':
      lockedCounter.add(1);
      break;
    default:
      otherCounter.add(1);
  }
}

// ============================================================
// Teardown - 결과 요약 및 비교
// ============================================================
export function teardown(data) {
  console.log(`\n${'='.repeat(70)}`);
  console.log('📊 Race Condition 비교 테스트 완료');
  console.log(`${'='.repeat(70)}`);
  console.log('\n🔍 결과 비교 포인트:');
  console.log('');
  console.log('  1. 응답 시간 비교:');
  console.log('     - no_lock_duration: /login 응답 시간 분포');
  console.log('     - with_lock_duration: /login-with-lock 응답 시간 분포');
  console.log('');
  console.log('  2. Race Condition 방지 효과:');
  console.log('     - no_lock_USER001: 락 없이 로그인 실패 처리된 횟수');
  console.log('       → 여러 개면 Race Condition으로 중복 처리됨');
  console.log('     - with_lock_USER001: 락 있을 때 로그인 실패 처리된 횟수');
  console.log('       → 1개여야 정상 (나머지는 락 대기 후 잠금 상태)');
  console.log('');
  console.log('  3. 잠금 처리:');
  console.log('     - *_LOCKED: 계정 잠금(LOGIN_FAILURE_001) 또는 락 획득 실패(COMMON_001)');
  console.log(`${'='.repeat(70)}\n`);
}
