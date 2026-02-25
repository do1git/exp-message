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
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionExpiryInterceptor
import java.time.LocalDateTime

/**
 * WebSocketSessionExpiryInterceptor 단위 테스트.
 *
 * - CONNECT는 만료 검사 없이 통과
 * - authInfo 없으면 통과
 * - expiresAt이 미래면 통과
 * - expiresAt이 과거면 DomainException(UNAUTHORIZED) 발생
 */
class WebSocketSessionExpiryInterceptorUT {
    private val interceptor = WebSocketSessionExpiryInterceptor()
    private val mockChannel = mockk<MessageChannel>(relaxed = true)

    @Test
    fun `CONNECT면 만료 검사 없이 통과`() {
        val expiredAuthInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().minusSeconds(1), CommonAuthRole.USER)
        val message = messageWithSessionAuth(StompCommand.CONNECT, expiredAuthInfo)

        val result = interceptor.preSend(message, mockChannel)

        result shouldBe message
    }

    @Test
    fun `authInfo 없으면 통과`() {
        val message = messageWithSessionAuth(StompCommand.SUBSCRIBE, null)

        val result = interceptor.preSend(message, mockChannel)

        result shouldBe message
    }

    @Test
    fun `expiresAt이 미래면 통과`() {
        val futureAuthInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().plusHours(1), CommonAuthRole.USER)
        val message = messageWithSessionAuth(StompCommand.SUBSCRIBE, futureAuthInfo)

        val result = interceptor.preSend(message, mockChannel)

        result shouldBe message
    }

    @Test
    fun `expiresAt이 과거면 DomainException UNAUTHORIZED 발생`() {
        val expiredAuthInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().minusSeconds(1), CommonAuthRole.USER)
        val message = messageWithSessionAuth(StompCommand.SUBSCRIBE, expiredAuthInfo)

        val ex = shouldThrow<DomainException> {
            interceptor.preSend(message, mockChannel)
        }

        ex.error.code shouldBe CommonError.UNAUTHORIZED.code
        ex.error.message shouldBe "Unauthorized"
        ex.details?.get("reason") shouldBe "Session expired"
        ex.details?.get("expiresAt") shouldBe expiredAuthInfo.expiresAt.toString()
    }

    @Test
    fun `SEND 시 만료면 DomainException 발생`() {
        val expiredAuthInfo = CommonAuthInfo("u1", "s1", LocalDateTime.now().minusMinutes(1), CommonAuthRole.USER)
        val message = messageWithSessionAuth(StompCommand.SEND, expiredAuthInfo)

        shouldThrow<DomainException> {
            interceptor.preSend(message, mockChannel)
        }
    }

    private fun messageWithSessionAuth(
        command: StompCommand,
        authInfo: CommonAuthInfo?,
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(command)
        accessor.sessionId = "ws-session-1"
        val sessionAttrs = mutableMapOf<String, Any>()
        if (authInfo != null) {
            sessionAttrs[WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO] = authInfo
        }
        accessor.setHeader(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES, sessionAttrs)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }
}
