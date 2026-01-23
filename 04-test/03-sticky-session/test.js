const { AuthClient } = require('./client-auth');
const { MetricsCollector } = require('./metrics');
const { WebSocketClient } = require('./client-websocket');
const axios = require('axios');

const BASE_URL = 'http://127.0.0.1/api';
const NUM_SUBSCRIBERS = parseInt(process.env.NUM_SUBSCRIBERS || '10');


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
 * 특정 방식 테스트 (Sticky Session 대응 버전)
 */
async function testMethod(methodName, ClientClass) {
  
    const authClient = new AuthClient(BASE_URL);
    const metrics = new MetricsCollector();
  
    // 1. 송신자 준비
    const sender = await authClient.signUpAndLogin();
    const chatRoomId = await authClient.createChatRoom(sender.accessToken);
  
    console.log(`Setting up ${NUM_SUBSCRIBERS} subscribers with unique IPs...`);
  
    // 2. N명의 구독자 준비
    const subscribers = [];
    for (let i = 0; i < NUM_SUBSCRIBERS; i++) {
      // 💡 테스트 포인트 1: 각 구독자에게 고유한 가상 IP 할당 (예: 10.0.0.1, 10.0.0.2 ...)
      const virtualIP = `10.${i+1}.${i+1}.${i + 1}`;
      
      // 유저 생성 및 로그인 (토큰 생성 포함)
      const user = await authClient.signUpAndLogin();

      console.log(`User created: ${user.email} with IP: ${virtualIP}`);
      
      // 클라이언트 생성
      const client = new ClientClass(BASE_URL, user.accessToken, chatRoomId);

      console.log(`Client created with IP: ${virtualIP}`);
      
      const receivePromise = new Promise((resolve) => {
        client.onReceive = (data) => {
        //   const latency = Date.now() - client.sendTime;
        //   metrics.record(latency);
        console.log(`Message received: ${data} from IP: ${virtualIP}`);
          resolve();
        };
      });
  
      // 💡 테스트 포인트 2: connect 시 IP 전달 (이전 답변에서 수정한 WebSocketClient 기준)
      // Nginx의 ip_hash가 이 IP를 보고 서로 다른 백엔드로 보냅니다.
      await client.connect((data) => {  if (client.onReceive) {client.onReceive(data);}}, virtualIP); 
      subscribers.push({ client, receivePromise, virtualIP });
      
      console.log(`Client connected with IP: ${virtualIP}`);

    }
    console.log(`Sending message to chat room: ${chatRoomId}`);
    await sendMessage(sender.accessToken, chatRoomId, `Test message`);

    await Promise.race([
        Promise.all(
          subscribers.map((sub) => {
            const p = new Promise((resolve) => {
              sub.client.onReceive = (data) => {
                console.log(`Message received: ${data} from IP: ${sub.virtualIP}`);
                resolve();
              };
            });
            return p;
          })
        ),
        new Promise((resolve) => setTimeout(resolve, 5000)),
      ]);


    console.log('\nDisconnecting subscribers...');
    for (const sub of subscribers) {
      await sub.client.disconnect();
    }

    console.log('Test completed successfully');
}

async function main() {
  await testMethod('WebSocket', WebSocketClient);
}

main();