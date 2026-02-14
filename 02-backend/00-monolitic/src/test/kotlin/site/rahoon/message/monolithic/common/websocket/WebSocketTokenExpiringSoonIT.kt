package site.rahoon.message.monolithic.common.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.TestPropertySource
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.websocket.auth.WebSocketAuthBody
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * 만료 임박 시 갱신 유도 MESSAGE 전송 통합 테스트.
 *
 * - TTL을 짧게(25초), imminent-threshold를 크게(22초) 설정해 연결 후 3초부터 임박 구간에 진입.
 * - websocket.heartbeat-interval-ms(500) 주기로 heartbeat가 실행되므로 최대 4초 이내에 token_expiring_soon 수신.
 * - `/queue/session/{sessionId}/auth` 구독 후 MESSAGE 수신 검증.
 */
@TestPropertySource(
    properties = [
        "authtoken.access-token-ttl-seconds=25",
        "websocket.imminent-threshold-seconds=22",
        "websocket.heartbeat-interval-ms=500",
    ],
)
class WebSocketTokenExpiringSoonIT(
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    companion object {
        /** 만료 임박 메시지 대기 최대 시간. (임박 진입 ~3초 + heartbeat 500ms + 여유 2초) */
        private const val AUTH_MESSAGE_TIMEOUT_MS = 6_000L
    }

    @Test
    fun `만료 임박 구간에 진입하면 token_expiring_soon MESSAGE를 수신한다`() {
        // given: 25초 TTL 토큰으로 WebSocket 연결 (만료 22초 전 = 3초 후부터 임박)
        val authResult = authApplicationITUtils.signUpAndLogin()
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"
        val stompClient = createStompClient()
        val sessionIdHolder = ArrayBlockingQueue<String>(1)
        val session: StompSession =
            stompClient
                .connect(
                    wsUrl,
                    object : StompSessionHandlerAdapter() {
                        override fun afterConnected(
                            session: StompSession,
                            connectedHeaders: StompHeaders,
                        ) {
                            // StompSession.getSessionId()와 서버 session이 다를 수 있음.
                            // CONNECTED 프레임의 session 헤더(WebSocketConnectedSessionHeaderInterceptor가 주입) 사용.
                            connectedHeaders.getFirst("session")?.let { sessionIdHolder.offer(it) }
                        }
                    },
                ).get(5, TimeUnit.SECONDS)

        val authMessages = ArrayBlockingQueue<WebSocketAuthBody>(2)
        val sessionId = sessionIdHolder.poll(2, TimeUnit.SECONDS).shouldNotBeNull()

        // when: auth 큐 구독 후 대기 (heartbeat 주기마다 임박 세션에 MESSAGE 전송)
        session.subscribe(
            "/queue/session/$sessionId/auth",
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): java.lang.reflect.Type = WebSocketAuthBody::class.java

                override fun handleFrame(
                    headers: StompHeaders,
                    payload: Any?,
                ) {
                    if (payload is WebSocketAuthBody) authMessages.offer(payload)
                }
            },
        )

        // then: AUTH_MESSAGE_TIMEOUT_MS 이내에 token_expiring_soon 수신
        val received = authMessages.poll(AUTH_MESSAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS).shouldNotBeNull()
        received.event shouldBe WebSocketAuthBody.EVENT_TOKEN_EXPIRING_SOON
        received.websocketSessionId shouldBe sessionId
        received.expiresAt.shouldNotBeNull()
        received.occurredAt.shouldNotBeNull()
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
