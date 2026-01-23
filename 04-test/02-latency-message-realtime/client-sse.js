const EventSource = require('eventsource');

/**
 * SSE 클라이언트
 */
class SSEClient {
  constructor(baseURL, accessToken, chatRoomId) {
    this.baseURL = baseURL;
    this.accessToken = accessToken;
    this.chatRoomId = chatRoomId;
    this.eventSource = null;
  }

  /**
   * 연결 및 구독
   * @param {Function} onMessage - 메시지 수신 콜백
   * @returns {Promise<void>}
   */
  connect(onMessage) {
    return new Promise((resolve, reject) => {
      const url = `${this.baseURL}/sse/chat-rooms/${this.chatRoomId}/messages`;
      this.eventSource = new EventSource(url, {
        headers: {
          Authorization: `Bearer ${this.accessToken}`,
        },
      });

      let connected = false;

      // 연결 완료 이벤트
      this.eventSource.addEventListener('connected', () => {
        connected = true;
        resolve();
      });

      // 메시지 수신
      this.eventSource.addEventListener('message', (event) => {
        try {
          const data = JSON.parse(event.data);
          onMessage(data);
        } catch (e) {
          console.error('Failed to parse SSE message:', e);
        }
      });

      // 에러 처리
      this.eventSource.onerror = (error) => {
        // 연결 완료 전에만 에러를 reject
        if (!connected) {
          if (this.eventSource.readyState === EventSource.CLOSED) {
            reject(new Error('SSE connection closed'));
          }
          // CONNECTING 상태의 에러는 무시 (자동 재연결 시도 중)
        }
        // 연결 완료 후 에러는 무시 (EventSource의 자동 재연결)
      };

      // 타임아웃 (5초 내 연결 안되면 실패)
      setTimeout(() => {
        if (this.eventSource && this.eventSource.readyState === EventSource.CONNECTING && !connected) {
          this.eventSource.close();
          reject(new Error('SSE connection timeout'));
        }
      }, 5000);
    });
  }

  /**
   * 연결 해제
   */
  disconnect() {
    return new Promise((resolve) => {
      if (this.eventSource) {
        this.eventSource.close();
      }
      resolve();
    });
  }
}

module.exports = { SSEClient };
