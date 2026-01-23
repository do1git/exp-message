const SockJS = require('sockjs-client');
const Stomp = require('webstomp-client');

/**
 * WebSocket (STOMP) 클라이언트
 */
class WebSocketClient {
  constructor(baseURL, accessToken, chatRoomId) {
    this.baseURL = baseURL;
    this.accessToken = accessToken;
    this.chatRoomId = chatRoomId;
    this.stompClient = null;
    this.subscription = null;
  }

  /**
   * 연결 및 구독
   * @param {Function} onMessage - 메시지 수신 콜백
   * @returns {Promise<void>}
   */
  connect(onMessage) {
    return new Promise((resolve, reject) => {
      const wsURL = `${this.baseURL}/ws?access_token=${this.accessToken}`;
      const socket = new SockJS(wsURL);
      this.stompClient = Stomp.over(socket);

      // 로그 비활성화
      this.stompClient.debug = () => {};

      this.stompClient.connect(
        {},
        (frame) => {
          // 구독
          this.subscription = this.stompClient.subscribe(
            `/topic/chat-rooms/${this.chatRoomId}/messages`,
            (message) => {
              const data = JSON.parse(message.body);
              onMessage(data);
            }
          );
          resolve();
        },
        (error) => {
          reject(new Error(`WebSocket connection failed: ${error}`));
        }
      );
    });
  }

  /**
   * 연결 해제
   */
  disconnect() {
    return new Promise((resolve) => {
      if (this.subscription) {
        this.subscription.unsubscribe();
      }
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.disconnect(() => {
          resolve();
        });
      } else {
        resolve();
      }
    });
  }
}

module.exports = { WebSocketClient };
