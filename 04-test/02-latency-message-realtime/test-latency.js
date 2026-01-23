const axios = require('axios');
const { AuthClient } = require('./client-auth');
const { WebSocketClient } = require('./client-websocket');
const { SSEClient } = require('./client-sse');
const { LongPollingClient } = require('./client-longpolling');
const { MetricsCollector } = require('./metrics');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const NUM_SUBSCRIBERS = parseInt(process.env.NUM_SUBSCRIBERS || '10'); // N명의 구독자
const NUM_ITERATIONS = parseInt(process.env.NUM_ITERATIONS || '100'); // 100회 반복

/**
 * 메시지 전송
 */
async function sendMessage(accessToken, chatRoomId, content) {
  const res = await axios.post(
    `${BASE_URL}/messages`,
    {
      chatRoomId,
      content,
    },
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );
  return res.data;
}

/**
 * 특정 방식 테스트
 */
async function testMethod(methodName, ClientClass) {
  console.log(`\n========================================`);
  console.log(`Testing: ${methodName}`);
  console.log(`Subscribers: ${NUM_SUBSCRIBERS}`);
  console.log(`Iterations: ${NUM_ITERATIONS}`);
  console.log(`========================================\n`);

  const authClient = new AuthClient(BASE_URL);
  const metrics = new MetricsCollector();

  // 1. 송신자 준비
  const sender = await authClient.signUpAndLogin();
  const chatRoomId = await authClient.createChatRoom(sender.accessToken);

  console.log(`ChatRoom created: ${chatRoomId}`);
  console.log(`Setting up ${NUM_SUBSCRIBERS} subscribers...`);

  // 2. N명의 구독자 준비
  const subscribers = [];
  for (let i = 0; i < NUM_SUBSCRIBERS; i++) {
    const user = await authClient.signUpAndLogin();
    
    // 채팅방 멤버 추가 (필요한 경우)
    // await addChatRoomMember(chatRoomId, user.userId);
    
    const client = new ClientClass(BASE_URL, user.accessToken, chatRoomId);
    
    const receivePromise = new Promise((resolve) => {
      client.onReceive = (data) => {
        const latency = Date.now() - client.sendTime;
        metrics.record(latency);
        resolve();
      };
    });

    await client.connect((data) => {
      if (client.onReceive) {
        client.onReceive(data);
      }
    });

    subscribers.push({ client, receivePromise });
  }

  console.log(`All subscribers connected. Starting test...\n`);

  // 3. 메시지 전송 및 수신 측정
  for (let i = 0; i < NUM_ITERATIONS; i++) {
    // 송신 시간 기록
    const sendTime = Date.now();
    subscribers.forEach((sub) => {
      sub.client.sendTime = sendTime;
    });

    // 메시지 전송
    await sendMessage(
      sender.accessToken,
      chatRoomId,
      `Test message ${i + 1}`
    );

    // 모든 구독자가 수신할 때까지 대기 (최대 5초)
    await Promise.race([
      Promise.all(
        subscribers.map((sub) => {
          const p = new Promise((resolve) => {
            sub.client.onReceive = (data) => {
              const latency = Date.now() - sub.client.sendTime;
              metrics.record(latency);
              resolve();
            };
          });
          return p;
        })
      ),
      new Promise((resolve) => setTimeout(resolve, 5000)),
    ]);

    // 다음 메시지 전송 전 잠시 대기
    await new Promise((resolve) => setTimeout(resolve, 100));

    if ((i + 1) % 10 === 0) {
      console.log(`Progress: ${i + 1}/${NUM_ITERATIONS}`);
    }
  }

  // 4. 연결 해제
  console.log('\nDisconnecting subscribers...');
  for (const sub of subscribers) {
    await sub.client.disconnect();
  }

  // 5. 결과 출력
  metrics.print(methodName);

  return metrics;
}

/**
 * 메인 실행
 */
async function main() {
  console.log(`
╔══════════════════════════════════════════════════════════╗
║  Realtime Message Latency Comparison Test               ║
║                                                          ║
║  WebSocket vs SSE vs Long Polling                       ║
╚══════════════════════════════════════════════════════════╝
`);

  try {
    // WebSocket 테스트
    const wsMetrics = await testMethod('WebSocket (STOMP)', WebSocketClient);

    // 테스트 간 간격
    await new Promise((resolve) => setTimeout(resolve, 2000));

    // SSE 테스트
    const sseMetrics = await testMethod('SSE', SSEClient);

    // 테스트 간 간격
    await new Promise((resolve) => setTimeout(resolve, 2000));

    // Long Polling 테스트
    const lpMetrics = await testMethod('Long Polling', LongPollingClient);

    // 최종 비교 결과
    console.log(`\n
╔══════════════════════════════════════════════════════════╗
║  Final Comparison                                        ║
╚══════════════════════════════════════════════════════════╝
`);

    console.log('Method,Count,Min,Max,Avg,P50,P95,P99');
    console.log(wsMetrics.toCSV('WebSocket'));
    console.log(sseMetrics.toCSV('SSE'));
    console.log(lpMetrics.toCSV('Long Polling'));

    process.exit(0);
  } catch (error) {
    console.error('\n❌ Test failed:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

// 실행
main();
