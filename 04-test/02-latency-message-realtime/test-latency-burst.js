const axios = require('axios');
const { AuthClient } = require('./client-auth');
const { WebSocketClient } = require('./client-websocket');
const { SSEClient } = require('./client-sse');
const { LongPollingClient } = require('./client-longpolling');
const { MetricsCollector } = require('./metrics');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const NUM_SUBSCRIBERS = parseInt(process.env.NUM_SUBSCRIBERS || '10'); // N명의 구독자
const NUM_ITERATIONS = parseInt(process.env.NUM_ITERATIONS || '100'); // 100회 반복
const MESSAGES_PER_BURST = 3; // 한 번에 보낼 메시지 수

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
 * 특정 방식 테스트 (연속 메시지 3개)
 */
async function testMethod(methodName, ClientClass) {
  console.log(`\n========================================`);
  console.log(`Testing: ${methodName}`);
  console.log(`Subscribers: ${NUM_SUBSCRIBERS}`);
  console.log(`Iterations: ${NUM_ITERATIONS}`);
  console.log(`Messages per burst: ${MESSAGES_PER_BURST}`);
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
    const client = new ClientClass(BASE_URL, user.accessToken, chatRoomId);
    
    // 각 구독자별 수신 카운터
    client.receivedMessages = [];
    
    await client.connect((data) => {
      client.receivedMessages.push({
        content: data.content,
        receivedAt: Date.now(),
      });
    });

    subscribers.push({ client });
  }

  console.log(`All subscribers connected. Starting test...\n`);

  // 3. 메시지 버스트 전송 및 수신 측정
  for (let i = 0; i < NUM_ITERATIONS; i++) {
    // 각 구독자의 수신 메시지 초기화
    subscribers.forEach((sub) => {
      sub.client.receivedMessages = [];
    });

    const burstStartTime = Date.now();

    // 메시지 3개 연속 전송 (50ms 간격)
    for (let j = 0; j < MESSAGES_PER_BURST; j++) {
      await sendMessage(
        sender.accessToken,
        chatRoomId,
        `Burst ${i + 1} - Message ${j + 1}`
      );
      // 메시지 사이에 짧은 간격
      if (j < MESSAGES_PER_BURST - 1) {
        await new Promise((resolve) => setTimeout(resolve, 50));
      }
    }

    // 모든 구독자가 3개 메시지를 모두 받을 때까지 대기 (최대 10초)
    await Promise.race([
      Promise.all(
        subscribers.map((sub) => {
          return new Promise((resolve) => {
            const checkInterval = setInterval(() => {
              if (sub.client.receivedMessages.length >= MESSAGES_PER_BURST) {
                clearInterval(checkInterval);
                
                // 마지막 메시지 수신 시간
                const lastMessageTime = sub.client.receivedMessages[MESSAGES_PER_BURST - 1].receivedAt;
                const totalLatency = lastMessageTime - burstStartTime;
                metrics.record(totalLatency);
                
                resolve();
              }
            }, 10);
          });
        })
      ),
      new Promise((resolve) => setTimeout(resolve, 10000)),
    ]);

    // 다음 버스트 전 대기 (Long Polling이 재연결할 시간)
    await new Promise((resolve) => setTimeout(resolve, 200));

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
  metrics.print(`${methodName} (${MESSAGES_PER_BURST} messages burst)`);

  return metrics;
}

/**
 * 메인 실행
 */
async function main() {
  console.log(`
╔══════════════════════════════════════════════════════════╗
║  Realtime Message Burst Latency Test                    ║
║                                                          ║
║  WebSocket vs SSE vs Long Polling                       ║
║  (3 consecutive messages)                               ║
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
║  Final Comparison (3 messages burst)                    ║
╚══════════════════════════════════════════════════════════╝
`);

    console.log('Method,Count,Min,Max,Avg,P50,P95,P99');
    console.log(wsMetrics.toCSV('WebSocket'));
    console.log(sseMetrics.toCSV('SSE'));
    console.log(lpMetrics.toCSV('Long Polling'));

    console.log('\n📊 Analysis:');
    const wsAvg = wsMetrics.calculate().avg;
    const sseAvg = sseMetrics.calculate().avg;
    const lpAvg = lpMetrics.calculate().avg;
    
    console.log(`  WebSocket:    ${wsAvg.toFixed(2)}ms (baseline)`);
    console.log(`  SSE:          ${sseAvg.toFixed(2)}ms (+${(sseAvg - wsAvg).toFixed(2)}ms)`);
    console.log(`  Long Polling: ${lpAvg.toFixed(2)}ms (+${(lpAvg - wsAvg).toFixed(2)}ms) ← reconnection overhead`);

    process.exit(0);
  } catch (error) {
    console.error('\n❌ Test failed:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

// 실행
main();
