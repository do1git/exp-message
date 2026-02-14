package site.rahoon.message.monolithic.common.websocket.config.session

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import java.time.LocalDateTime

/**
 * 인바운드 메시지 수신 시 세션(토큰) 만료 여부를 주기적으로 검사한다.
 *
 * - **검사 시점**: CONNECT를 제외한 모든 STOMP 메시지(SEND, SUBSCRIBE, DISCONNECT 등) 수신 시.
 *   (클라이언트 heartbeat가 인바운드로 오지 않는 환경이면 [WebSocketSessionExpiryHeartbeatTask]가 websocket.heartbeat-interval-ms 주기로 별도 검사.)
 * - **만료 기준**: 세션에 저장된 [CommonAuthInfo.expiresAt](JWT exp)이 현재 시각보다 이전이면 만료로 간주.
 * - **만료 시 처리**: [DomainException](CommonError.UNAUTHORIZED)를 던져 [WebSocketExceptionStompSubProtocolErrorHandler]가
 *   ERROR 프레임을 보낸 뒤 연결을 종료한다. 갱신 유도·유예 없이 즉시 종료.
 *
 * 만료 **임박** 처리(갱신 유도·유예 후 종료)는 별도로 구분하여 구현 예정.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // CONNECT 처리(WebSocketConnectInterceptor) 직후에 실행. 구독 검증보다는 나중에 실행되도록.
class WebSocketSessionExpiryInterceptor : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    @Suppress("ReturnCount")
    @Nullable
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.CONNECT) {
            return message
        }

        val authInfo = accessor.sessionAttributes
            ?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
            ?: return message

        if (authInfo.expiresAt.isBefore(LocalDateTime.now())) {
            log.warn("세션 만료로 연결 종료: userId={}, sessionId={}, sessionId(ws)={}", authInfo.userId, authInfo.sessionId, accessor.sessionId)
            throw DomainException(
                CommonError.UNAUTHORIZED,
                mapOf(
                    "reason" to "Session expired",
                    "expiresAt" to authInfo.expiresAt.toString(),
                ),
            )
        }
        return message
    }
}
