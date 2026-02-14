package site.rahoon.message.monolithic.common.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.TestPropertySource
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import java.util.concurrent.TimeUnit

/**
 * WebSocket 세션 만료(WebSocketSessionExpiryInterceptor) 통합 테스트.
 *
 * - 액세스 토큰 TTL을 짧게(3초) 설정해, 연결 성공 후 토큰이 만료되면 서버가 다음 인바운드(heartbeat 등) 시 만료 검사 후 연결 종료.
 * - HEARTBEAT_INTERVAL_MS 이내에 연결이 끊기는지 검증.
 * - TTL은 CONNECT가 완료될 만큼만 여유 있게(3초). 1초면 로그인·연결 설정 중 만료되어 CONNECT 단계에서 검증 실패할 수 있음.
 */
@TestPropertySource(
    properties = [
        "authtoken.access-token-ttl-seconds=3",
        "websocket.heartbeat-interval-ms=500",
    ],
)
class WebSocketSessionExpiryIT(
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    companion object {
        /** websocket.heartbeat-interval-ms(500) + 여유. 이 시간 이내에 만료 세션 연결이 끊겨야 함. */
        private const val DISCONNECT_DEADLINE_MS = 2500L
    }

    @Test
    fun `짧은 TTL 토큰으로 연결 후 HEARTBEAT_INTERVAL_MS 이내에 연결이 끊긴다`() {
        // given: 3초 TTL 토큰으로 WebSocket 연결 (CONNECT가 완료될 만큼만 여유)
        val authResult = authApplicationITUtils.signUpAndLogin()
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"
        val stompClient = createStompClient()
        val session: StompSession =
            stompClient.connect(wsUrl, object : StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS)

        // when: 토큰 만료 대기(3초 + 여유) 후 서버가 다음 인바운드(heartbeat 등) 시 만료 검사 → ERROR 후 연결 종료
        Thread.sleep(4000)

        // then: heartbeat 주기 이내에 연결이 끊김
        val pollIntervalMs = 100L
        val deadlineMs = DISCONNECT_DEADLINE_MS
        var elapsedMs = 0L
        while (elapsedMs < deadlineMs) {
            if (!session.isConnected) break
            Thread.sleep(pollIntervalMs)
            elapsedMs += pollIntervalMs
        }

        session.isConnected shouldBe false
    }

    private fun createStompClient(): WebSocketStompClient {
        val transports = listOf(WebSocketTransport(StandardWebSocketClient()))
        val sockJsClient = SockJsClient(transports)
        val stompClient = WebSocketStompClient(sockJsClient)
        val converter = MappingJackson2MessageConverter()
        converter.objectMapper = objectMapper
        stompClient.setMessageConverter(converter)
        return stompClient
    }
}
