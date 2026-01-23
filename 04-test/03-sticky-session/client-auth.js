const axios = require('axios');

/**
 * 인증 클라이언트
 */
class AuthClient {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.axios = axios.create({ baseURL });
  }

  /**
   * 회원가입 + 로그인
   * @returns {Promise<{accessToken: string, userId: string}>}
   */
  async signUpAndLogin() {
    const timestamp = Date.now();
    const random = Math.floor(Math.random() * 10000);
    const email = `test-${timestamp}-${random}@example.com`;
    const password = 'password123'; // 8자 이상 100자 이하
    const nickname = `user${random}`; // 2자 이상 20자 이하

    // 회원가입 (POST /users)
    const signUpRes = await this.axios.post('/users', {
      email,
      password,
      nickname,
    });

    // 로그인 (POST /auth/login)
    const loginRes = await this.axios.post('/auth/login', {
      email,
      password,
    });

    // CommonApiResponse 형식: { success: true, data: {...}, ... }
    const loginData = loginRes.data.data;

    return {
      accessToken: loginData.accessToken,
      userId: loginData.userId,
      email,
    };
  }

  /**
   * 채팅방 생성
   * @param {string} accessToken 
   * @returns {Promise<string>} chatRoomId
   */
  async createChatRoom(accessToken) {
    const res = await this.axios.post(
      '/chat-rooms',
      {
        name: `TestRoom-${Date.now()}`,
      },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );
    // CommonApiResponse 형식: { success: true, data: {...}, ... }
    return res.data.data.id;
  }
}

module.exports = { AuthClient };
