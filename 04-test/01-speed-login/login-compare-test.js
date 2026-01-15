import http from 'k6/http';
import { check, group } from 'k6';
import { Counter, Trend } from 'k6/metrics';

// ============================================================
// 환경 변수 설정
// ============================================================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_EMAIL = __ENV.TEST_EMAIL || 'compare-test@example.com';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'wrongpassword';
const TEST_IP = __ENV.TEST_IP || '10.0.0.';

// ============================================================
// 커스텀 메트릭 - /login-without-lock (락 없음)
// ============================================================
const loginNoLock_USER001 = new Counter('login_no_lock_USER001');
const loginNoLock_LOCKED = new Counter('login_no_lock_LOCKED');
const loginNoLock_OTHER = new Counter('login_no_lock_OTHER');
const loginNoLock_Duration = new Trend('login_no_lock_duration', true);

// ============================================================
// 커스텀 메트릭 - /login (락 있음)
// ============================================================
const loginWithLock_USER001 = new Counter('login_with_lock_USER001');
const loginWithLock_LOCKED = new Counter('login_with_lock_LOCKED');
const loginWithLock_OTHER = new Counter('login_with_lock_OTHER');
const loginWithLock_Duration = new Trend('login_with_lock_duration', true);

// ============================================================
// 테스트 옵션 - 두 엔드포인트 순차 비교
// ============================================================
export const options = {
  scenarios: {
    // 1단계: /login-without-lock (락 없음) 테스트
    login_no_lock: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      exec: 'testLoginNoLock',
      tags: { endpoint: 'login' },
    },
    // 2단계: /login-with-lock (락 있음) 테스트
    login_with_lock: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      startTime: '35s', // 1단계 완료 후 시작
      exec: 'testLoginWithLock',
      tags: { endpoint: 'login-with-lock' },
    },
  },
  thresholds: {
    'login_no_lock_duration': ['p(95)<5000'],
    'login_with_lock_duration': ['p(95)<5000'],
  },
};

// ============================================================
// Setup - 테스트 시작 전 설정
// ============================================================
export function setup() {
  console.log(`\n${'='.repeat(70)}`);
  console.log('🔬 로그인 성능 비교 테스트: /login-without-lock vs /login');
  console.log(`${'='.repeat(70)}`);
  console.log(`📍 Base URL: ${BASE_URL}`);
  console.log(`📧 Test Email: ${TEST_EMAIL}`);
  console.log(`👥 VUs: 20, Iterations: 20 (각 시나리오)`);
  console.log(`${'='.repeat(70)}`);
  console.log('\n📋 테스트 순서:');
  console.log('  1. /auth/login-without-lock (락 없음) - 20 VU x 20 iterations');
  console.log('  2. 5초 대기');
  console.log('  3. /auth/login (락 있음) - 20 VU x 20 iterations');
  console.log(`${'='.repeat(70)}\n`);

  return {
    baseUrl: BASE_URL,
    email: TEST_EMAIL,
    password: TEST_PASSWORD,
    ipBase: TEST_IP,
  };
}

// ============================================================
// 테스트 1: /login-without-lock (락 없음)
// ============================================================
export function testLoginNoLock(data) {
  const uniqueId = `${Date.now()}-${__VU}-${__ITER}`;
  const email = `no-lock-${uniqueId}@test.com`;
  const ipAddress = `${data.ipBase}${__VU}`;

  const payload = JSON.stringify({
    email: email,
    password: data.password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': ipAddress,
    },
  };

  const startTime = Date.now();
  const response = http.post(`${data.baseUrl}/auth/login-without-lock`, payload, params);
  const duration = Date.now() - startTime;

  loginNoLock_Duration.add(duration);

  const errorCode = extractErrorCode(response);
  recordMetrics(errorCode, loginNoLock_USER001, loginNoLock_LOCKED, loginNoLock_OTHER);

  console.log(`[NO-LOCK] VU=${__VU}, duration=${duration}ms, code=${errorCode}`);

  check(response, {
    '[NO-LOCK] response is valid': (r) => r.status >= 200 && r.status < 500,
  });
}

// ============================================================
// 테스트 2: /login (락 있음)
// ============================================================
export function testLoginWithLock(data) {
  const uniqueId = `${Date.now()}-${__VU}-${__ITER}`;
  const email = `with-lock-${uniqueId}@test.com`;
  const ipAddress = `${data.ipBase}${__VU + 100}`;

  const payload = JSON.stringify({
    email: email,
    password: data.password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': ipAddress,
    },
  };

  const startTime = Date.now();
  const response = http.post(`${data.baseUrl}/auth/login`, payload, params);
  const duration = Date.now() - startTime;

  loginWithLock_Duration.add(duration);

  const errorCode = extractErrorCode(response);
  recordMetrics(errorCode, loginWithLock_USER001, loginWithLock_LOCKED, loginWithLock_OTHER);

  console.log(`[WITH-LOCK] VU=${__VU}, duration=${duration}ms, code=${errorCode}`);

  check(response, {
    '[WITH-LOCK] response is valid': (r) => r.status >= 200 && r.status < 500,
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
// Teardown - 결과 요약
// ============================================================
export function teardown(data) {
  console.log(`\n${'='.repeat(70)}`);
  console.log('📊 성능 비교 테스트 완료');
  console.log(`${'='.repeat(70)}`);
  console.log('\n📈 결과 해석 가이드:');
  console.log('  - login_no_lock_duration: /login-without-lock 응답 시간');
  console.log('  - login_with_lock_duration: /login 응답 시간');
  console.log('  - *_USER001: 로그인 실패 (비밀번호 오류) 횟수');
  console.log('  - *_LOCKED: 계정 잠금 또는 락 획득 실패 횟수');
  console.log(`${'='.repeat(70)}\n`);
}
