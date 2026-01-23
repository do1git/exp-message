const axios = require('axios');
const { AuthClient } = require('./client-auth');
const { WebSocketClient } = require('./client-websocket');
const { SSEClient } = require('./client-sse');
const { MetricsCollector } = require('./metrics');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';
const CONNECTION_STEPS = [50, 100, 150, 200, 250, 300]; // 연결 수 단계 (50개씩)

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
 * 동시 연결 부하 테스트
 */
async function testConcurrentLoad(methodName, ClientClass, numConnections) {
  console.log(`\n========================================`);
  console.log(`Testing: ${methodName}`);
  console.log(`Connections: ${numConnections}`);
  console.log(`========================================\n`);

  const authClient = new AuthClient(BASE_URL);
  const metrics = new MetricsCollector();

  try {
    // 1. 송신자 준비
    const sender = await authClient.signUpAndLogin();
    const chatRoomId = await authClient.createChatRoom(sender.accessToken);

    console.log(`ChatRoom created: ${chatRoomId}`);
    console.log(`Setting up ${numConnections} connections...`);

    // 2. N개의 연결 생성
    const subscribers = [];
    let successCount = 0;
    let failCount = 0;
    const errorStats = {};

    for (let i = 0; i < numConnections; i++) {
      try {
        const user = await authClient.signUpAndLogin();
        const client = new ClientClass(BASE_URL, user.accessToken, chatRoomId);

        client.receivedMessage = false;

        await client.connect((data) => {
          if (!client.receivedMessage) {
            client.receivedMessage = true;
            const latency = Date.now() - client.sendTime;
            metrics.record(latency);
          }
        });

        subscribers.push({ client, connected: true });
        successCount++;

        // 서버 부하 분산을 위한 짧은 지연 (10개당 50ms)
        if ((i + 1) % 10 === 0) {
          console.log(`Progress: ${i + 1}/${numConnections} connections`);
          await new Promise((resolve) => setTimeout(resolve, 50));
        }
      } catch (error) {
        const errorKey = error.response?.status
          ? `HTTP ${error.response.status}`
          : error.code || error.message.split(':')[0] || 'Unknown';
        errorStats[errorKey] = (errorStats[errorKey] || 0) + 1;

        console.error(`Failed to connect subscriber ${i + 1}: [${errorKey}] ${error.message}`);
        subscribers.push({ client: null, connected: false });
        failCount++;
      }
    }

    console.log(`\nConnections established: ${successCount}/${numConnections} (${failCount} failed)`);
    if (failCount > 0) {
      console.log('Error breakdown:');
      Object.entries(errorStats)
        .sort((a, b) => b[1] - a[1])
        .forEach(([error, count]) => {
          console.log(`  - ${error}: ${count}`);
        });
    }

    // 3. 메시지 전송 및 수신 대기
    console.log('Sending message...');
    const sendTime = Date.now();
    subscribers.forEach((sub) => {
      if (sub.connected) {
        sub.client.sendTime = sendTime;
      }
    });

    await sendMessage(sender.accessToken, chatRoomId, `Load test with ${numConnections} connections`);

    // 메시지 수신 대기 (최대 10초)
    await new Promise((resolve) => setTimeout(resolve, 10000));

    // 4. 결과 집계
    let receivedCount = 0;
    let messageNotReceivedCount = 0;
    subscribers.forEach((sub) => {
      if (sub.connected && sub.client.receivedMessage) {
        receivedCount++;
      } else if (sub.connected && !sub.client.receivedMessage) {
        messageNotReceivedCount++;
      }
    });

    const errorRate = ((numConnections - receivedCount) / numConnections) * 100;

    console.log(`\nReceived: ${receivedCount}/${numConnections} (${errorRate.toFixed(2)}% error rate)`);
    if (messageNotReceivedCount > 0) {
      console.log(`⚠️ Connected but message not received: ${messageNotReceivedCount}`);
      errorStats['Message not received'] = messageNotReceivedCount;
    }

    // 5. 연결 해제
    console.log('Disconnecting...');
    for (const sub of subscribers) {
      if (sub.connected) {
        try {
          await sub.client.disconnect();
        } catch (error) {
          // Ignore disconnect errors
        }
      }
    }

    // 6. 더미 메시지로 죽은 연결 정리 (SSE 전용)
    if (methodName === 'SSE') {
      console.log('Cleaning up dead SSE connections...');
      // EventSource의 재연결 시도가 완전히 멈출 때까지 대기 (3초)
      await new Promise((resolve) => setTimeout(resolve, 3000));

      // cleanup 메시지를 3번 전송해서 모든 죽은 연결 강제 제거
      for (let i = 0; i < 3; i++) {
        try {
          await sendMessage(sender.accessToken, chatRoomId, `[Cleanup-${i + 1}]`);
          await new Promise((resolve) => setTimeout(resolve, 500));
        } catch (error) {
          // Ignore cleanup message errors
        }
      }

      // 최종 정리 대기 (1초)
      await new Promise((resolve) => setTimeout(resolve, 1000));
    }

    return {
      numConnections,
      successConnections: successCount,
      failConnections: failCount,
      receivedCount,
      errorRate,
      errorStats,
      metrics,
    };
  } catch (error) {
    console.error('Test failed:', error.message);
    const errorKey = error.response?.status
      ? `HTTP ${error.response.status}`
      : error.code || error.message.split(':')[0] || 'Unknown';
    return {
      numConnections,
      successConnections: 0,
      failConnections: numConnections,
      receivedCount: 0,
      errorRate: 100,
      errorStats: { [errorKey]: numConnections },
      metrics: new MetricsCollector(),
    };
  }
}

/**
 * 메인 실행
 */
async function main() {
  console.log(`
╔══════════════════════════════════════════════════════════╗
║  Concurrent Load Test                                   ║
║                                                          ║
║  WebSocket vs SSE                                       ║
║  (Progressive connection scaling)                       ║
╚══════════════════════════════════════════════════════════╝
`);

  const results = {
    WebSocket: [],
    SSE: [],
  };

  try {
    // 각 단계마다 WebSocket과 SSE를 번갈아 테스트
    for (const numConnections of CONNECTION_STEPS) {
      // WebSocket 테스트
      console.log('\n\n=== WebSocket Load Test ===\n');
      const wsResult = await testConcurrentLoad('WebSocket', WebSocketClient, numConnections);
      results.WebSocket.push(wsResult);

      // 에러율이 5%를 넘으면 경고 (계속 진행)
      if (wsResult.errorRate > 5) {
        console.log(`\n⚠️ WebSocket error rate: ${wsResult.errorRate.toFixed(2)}% at ${numConnections} connections`);
      }

      // 서버 리소스 정리 대기 (20초) - SSE 재연결 완전 종료 대기
      console.log('\nWaiting for server to clean up (20 seconds)...');
      await new Promise((resolve) => setTimeout(resolve, 20000));

      // SSE 테스트
      console.log('\n\n=== SSE Load Test ===\n');
      const sseResult = await testConcurrentLoad('SSE', SSEClient, numConnections);
      results.SSE.push(sseResult);

      // 에러율이 5%를 넘으면 경고 (계속 진행)
      if (sseResult.errorRate > 5) {
        console.log(`\n⚠️ SSE error rate: ${sseResult.errorRate.toFixed(2)}% at ${numConnections} connections`);
      }

      // 두 방식 모두 에러율이 5%를 넘으면 중단
      if (wsResult.errorRate > 5 && sseResult.errorRate > 5) {
        console.log(`\n⚠️ Both methods exceeded 5% error rate at ${numConnections} connections. Stopping test.`);
        break;
      }

      // 다음 단계 전 대기 (20초)
      if (numConnections < CONNECTION_STEPS[CONNECTION_STEPS.length - 1]) {
        console.log('\nWaiting before next step (20 seconds)...');
        await new Promise((resolve) => setTimeout(resolve, 20000));
      }
    }

    // 최종 결과 출력
    console.log(`\n
╔══════════════════════════════════════════════════════════╗
║  Final Results                                          ║
╚══════════════════════════════════════════════════════════╝
`);

    console.log('\n=== WebSocket ===');
    console.log('Connections,Success,Fail,Received,ErrorRate(%),P50(ms),P95(ms),P99(ms)');
    results.WebSocket.forEach((r) => {
      const stats = r.metrics.calculate();
      console.log(
        `${r.numConnections},${r.successConnections},${r.failConnections},${r.receivedCount},${r.errorRate.toFixed(2)},${stats.p50.toFixed(0)},${stats.p95.toFixed(0)},${stats.p99.toFixed(0)}`
      );
      if (r.failConnections > 0 && Object.keys(r.errorStats).length > 0) {
        console.log('  Errors:', JSON.stringify(r.errorStats));
      }
    });

    console.log('\n=== SSE ===');
    console.log('Connections,Success,Fail,Received,ErrorRate(%),P50(ms),P95(ms),P99(ms)');
    results.SSE.forEach((r) => {
      const stats = r.metrics.calculate();
      console.log(
        `${r.numConnections},${r.successConnections},${r.failConnections},${r.receivedCount},${r.errorRate.toFixed(2)},${stats.p50.toFixed(0)},${stats.p95.toFixed(0)},${stats.p99.toFixed(0)}`
      );
      if (r.failConnections > 0 && Object.keys(r.errorStats).length > 0) {
        console.log('  Errors:', JSON.stringify(r.errorStats));
      }
    });

    process.exit(0);
  } catch (error) {
    console.error('\n❌ Test failed:', error.message);
    console.error(error.stack);
    process.exit(1);
  }
}

// 실행
main();
