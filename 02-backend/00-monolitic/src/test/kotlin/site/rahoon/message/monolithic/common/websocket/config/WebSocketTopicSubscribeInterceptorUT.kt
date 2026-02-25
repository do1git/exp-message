package site.rahoon.message.monolithic.common.websocket.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthRole
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.config.subscribe.WebSocketAnnotatedMethodInvoker
import site.rahoon.message.monolithic.common.websocket.config.subscribe.WebSocketTopicSubscribeInterceptor
import java.time.LocalDateTime

/**
 * WebSocketTopicSubscribeInterceptor 단위 테스트.
 *
 * - /queue/session/{sessionId}/auth: sessionId 일치 시 구독 허용, 불일치 시 거부
 */
class WebSocketTopicSubscribeInterceptorUT {
    private val mockInvoker = mockk<WebSocketAnnotatedMethodInvoker>(relaxed = true)
    private val interceptor = WebSocketTopicSubscribeInterceptor(mockInvoker)
    private val mockChannel = mockk<MessageChannel>(relaxed = true)

    @Test
    fun `auth 큐 구독 - sessionId 일치 시 허용`() {
        val authInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().plusHours(1), CommonAuthRole.USER)
        val message = subscribeMessage("/queue/session/ws-123/auth", "ws-123", authInfo)

        val result = interceptor.preSend(message, mockChannel)

        result shouldBe message
    }

    @Test
    fun `auth 큐 구독 - sessionId 불일치 시 IllegalStateException`() {
        val authInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().plusHours(1), CommonAuthRole.USER)
        val message = subscribeMessage("/queue/session/ws-123/auth", "ws-999", authInfo)

        val ex = shouldThrow<IllegalStateException> {
            interceptor.preSend(message, mockChannel)
        }

        ex.message shouldBe "Subscription denied: session queue sessionId mismatch"
    }

    @Test
    fun `auth 큐 구독 - sessionId 없으면 IllegalStateException`() {
        val authInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().plusHours(1), CommonAuthRole.USER)
        val message = subscribeMessage("/queue/session/ws-123/auth", null, authInfo)

        shouldThrow<IllegalStateException> {
            interceptor.preSend(message, mockChannel)
        }
    }

    private fun subscribeMessage(
        destination: String,
        sessionId: String?,
        authInfo: CommonAuthInfo,
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = destination
        accessor.sessionId = sessionId
        val sessionAttrs = mutableMapOf<String, Any>(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO to authInfo)
        accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttrs)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
