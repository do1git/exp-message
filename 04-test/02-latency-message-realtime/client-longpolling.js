const axios = require('axios');

/**
 * Long Polling 클라이언트
 */
class LongPollingClient {
  constructor(baseURL, accessToken, chatRoomId) {
    this.baseURL = baseURL;
    this.accessToken = accessToken;
    this.chatRoomId = chatRoomId;
    this.polling = false;
    this.axios = axios.create({ baseURL });
  }

  /**
   * 연결 및 폴링 시작
   * @param {Function} onMessage - 메시지 수신 콜백
   * @returns {Promise<void>}
   */
  connect(onMessage) {
    return new Promise((resolve) => {
      this.polling = true;
      this.startPolling(onMessage);
      // 폴링 시작 직후 resolve
      setTimeout(resolve, 100);
    });
  }

  /**
   * 폴링 시작
   * @param {Function} onMessage
   */
  async startPolling(onMessage) {
    while (this.polling) {
      try {
        const response = await this.axios.get(
          `/long-polling/chat-rooms/${this.chatRoomId}/messages`,
          {
            headers: {
              Authorization: `Bearer ${this.accessToken}`,
            },
            timeout: 35000, // 서버 타임아웃(30초) + 여유
          }
        );

        // 메시지가 있으면 콜백 호출
        if (response.data && response.data.length > 0) {
          response.data.forEach((msg) => onMessage(msg));
        }
      } catch (error) {
        if (error.code !== 'ECONNABORTED') {
          console.error('Long polling error:', error.message);
        }
      }
    }
  }

  /**
   * 연결 해제
   */
  disconnect() {
    return new Promise((resolve) => {
      this.polling = false;
      resolve();
    });
  }
}

module.exports = { LongPollingClient };
