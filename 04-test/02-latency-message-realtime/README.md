# Realtime Message Latency Comparison Test

WebSocket, SSE, Long Polling 세 가지 실시간 통신 방식의 메시지 전달 지연시간(Latency)을 측정하고 비교합니다.

## 테스트 시나리오

1. N명의 사용자가 채팅방 구독
2. 1명이 메시지 전송
3. N-1명이 수신하는 시간 측정
4. 100회 반복 후 P50/P95/P99 계산

## 설치

```bash
npm install
```

## 실행

### 기본 실행 (10명 구독자, 100회 반복)

```bash
npm test
```

### PowerShell에서 실행

```powershell
node test-latency.js
```

### 환경변수로 설정 변경

```bash
# 구독자 수 변경
$env:NUM_SUBSCRIBERS=20; node test-latency.js

# 반복 횟수 변경
$env:NUM_ITERATIONS=50; node test-latency.js

# 서버 URL 변경
$env:BASE_URL="http://localhost:8080"; node test-latency.js

# 모두 함께
$env:BASE_URL="http://localhost:8080"; $env:NUM_SUBSCRIBERS=20; $env:NUM_ITERATIONS=50; node test-latency.js
```

## 출력 예시

```
╔══════════════════════════════════════════════════════════╗
║  Realtime Message Latency Comparison Test               ║
║                                                          ║
║  WebSocket vs SSE vs Long Polling                       ║
╚══════════════════════════════════════════════════════════╝

========================================
Testing: WebSocket (STOMP)
Subscribers: 10
Iterations: 100
========================================

ChatRoom created: xxx-xxx-xxx
Setting up 10 subscribers...
All subscribers connected. Starting test...

Progress: 10/100
Progress: 20/100
...

=== WebSocket (STOMP) ===
Count: 1000
Min: 5.23ms
Max: 45.67ms
Avg: 12.34ms
P50: 11.20ms
P95: 18.90ms
P99: 25.30ms

[SSE, Long Polling 결과...]

╔══════════════════════════════════════════════════════════╗
║  Final Comparison                                        ║
╚══════════════════════════════════════════════════════════╝

Method,Count,Min,Max,Avg,P50,P95,P99
WebSocket,1000,5.23,45.67,12.34,11.20,18.90,25.30
SSE,1000,6.10,48.20,13.50,12.00,20.10,28.40
Long Polling,1000,35.20,120.50,58.70,55.30,85.40,105.20
```

## 주의사항

1. 테스트 실행 전 백엔드 서버가 실행 중이어야 합니다.
2. 각 테스트는 순차적으로 실행되며, 테스트 간 2초 간격이 있습니다.
3. 구독자 수를 너무 많이 설정하면 서버 리소스 부족으로 테스트가 실패할 수 있습니다.
4. Long Polling은 구조적으로 지연이 클 수 있습니다 (재요청 오버헤드).

## 파일 구조

```
02-latency-message-realtime/
├── package.json              # 의존성 정의
├── README.md                 # 이 문서
├── test-latency.js          # 메인 테스트 스크립트
├── client-auth.js           # 인증/채팅방 생성
├── client-websocket.js      # WebSocket 클라이언트
├── client-sse.js            # SSE 클라이언트
├── client-longpolling.js    # Long Polling 클라이언트
└── metrics.js               # 메트릭 수집 및 계산
```

## 측정 지표

- **Count**: 측정된 샘플 수
- **Min**: 최소 지연시간
- **Max**: 최대 지연시간
- **Avg**: 평균 지연시간
- **P50**: 50% 백분위수 (중앙값)
- **P95**: 95% 백분위수
- **P99**: 99% 백분위수

## 예상 결과

일반적으로 다음과 같은 순서로 성능이 좋습니다:

1. **WebSocket**: 가장 낮은 지연시간 (~10ms)
2. **SSE**: WebSocket과 유사 (~12ms)
3. **Long Polling**: 상대적으로 높은 지연시간 (~50ms+)

Long Polling은 요청-응답-재요청 사이클로 인해 구조적으로 지연이 높습니다.
