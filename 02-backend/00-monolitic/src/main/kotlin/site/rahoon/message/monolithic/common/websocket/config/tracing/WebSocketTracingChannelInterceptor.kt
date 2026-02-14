package site.rahoon.message.monolithic.common.websocket.config.tracing

import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.global.nanoToMs
import site.rahoon.message.monolithic.common.observation.MdcKeys
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import java.time.OffsetDateTime

/**
 * clientInboundChannel용 ChannelInterceptor.
 *
 * 유저 요청(CONNECT, SUBSCRIBE, SEND)에 대해:
 * - traceId/spanId를 MDC에 설정
 * - websocket.command, websocket.session_id, websocket.destination, websocket.start_time,
 *   websocket.end_time, websocket.duration_ms, user_id, auth_session_id를 MDC에 설정
 *
 * afterSendCompletion에서 scope·span·MDC를 정리한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class WebSocketTracingChannelInterceptor(
    private val tracer: Tracer,
) : ChannelInterceptor {
    private val logger = KotlinLogging.logger {}
    private val spanHolder = ThreadLocal<Span?>()
    private val scopeHolder = ThreadLocal<Tracer.SpanInScope?>()
    private val startTimeHolder = ThreadLocal.withInitial { 0L }

    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        startTimeHolder.set(System.nanoTime())
        val span = tracer.nextSpan().name("websocket-inbound").start()
        spanHolder.set(span)
        scopeHolder.set(tracer.withSpan(span))

        putWebSocketMdc(message)
        MDC.put(MdcKeys.WEBSOCKET_START_TIME, OffsetDateTime.now().toString())
        return message
    }

    override fun afterSendCompletion(
        message: Message<*>,
        channel: MessageChannel,
        sent: Boolean,
        ex: Exception?,
    ) {
        try {
            scopeHolder.get()?.close()
            spanHolder.get()?.end()
        } finally {
            val durationMs = (System.nanoTime() - startTimeHolder.get()).nanoToMs()
            MDC.put(MdcKeys.WEBSOCKET_END_TIME, OffsetDateTime.now().toString())
            MDC.put(MdcKeys.WEBSOCKET_DURATION_MS, durationMs.toString())
            val completionLog =
                "WebSocket message completed: websocket.command=${MDC.get(MdcKeys.WEBSOCKET_COMMAND) ?: "-"}, " +
                    "websocket.session_id=${MDC.get(MdcKeys.WEBSOCKET_SESSION_ID) ?: "-"}, " +
                    "websocket.destination=${MDC.get(MdcKeys.WEBSOCKET_DESTINATION) ?: "-"}, " +
                    "websocket.duration_ms=$durationMs, " +
                    "websocket.start_time=${MDC.get(MdcKeys.WEBSOCKET_START_TIME) ?: "-"}, " +
                    "websocket.end_time=${MDC.get(MdcKeys.WEBSOCKET_END_TIME) ?: "-"}, " +
                    "user_id=${MDC.get(MdcKeys.USER_ID) ?: "-"}, " +
                    "auth_session_id=${MDC.get(MdcKeys.AUTH_SESSION_ID) ?: "-"}"
            if (StompHeaderAccessor.wrap(message).command != null) {
                logger.info { completionLog }
            } else {
                logger.debug { completionLog }
            }
            removeWebSocketMdc()
            startTimeHolder.remove()
            scopeHolder.remove()
            spanHolder.remove()
        }
    }

    private fun putWebSocketMdc(message: Message<*>) {
        val accessor = StompHeaderAccessor.wrap(message)
        accessor.command?.name?.let { MDC.put(MdcKeys.WEBSOCKET_COMMAND, it) }
        accessor.sessionId?.let { MDC.put(MdcKeys.WEBSOCKET_SESSION_ID, it) }
        accessor.destination?.takeIf { it.isNotBlank() }?.let { MDC.put(MdcKeys.WEBSOCKET_DESTINATION, it) }
        val authInfo = accessor.sessionAttributes
            ?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
        authInfo?.let {
            MDC.put(MdcKeys.USER_ID, it.userId)
            MDC.put(MdcKeys.AUTH_SESSION_ID, it.sessionId)
        }
    }

    private fun removeWebSocketMdc() {
        MDC.remove(MdcKeys.WEBSOCKET_COMMAND)
        MDC.remove(MdcKeys.WEBSOCKET_SESSION_ID)
        MDC.remove(MdcKeys.WEBSOCKET_DESTINATION)
        MDC.remove(MdcKeys.WEBSOCKET_START_TIME)
        MDC.remove(MdcKeys.WEBSOCKET_END_TIME)
        MDC.remove(MdcKeys.WEBSOCKET_DURATION_MS)
        MDC.remove(MdcKeys.USER_ID)
        MDC.remove(MdcKeys.AUTH_SESSION_ID)
    }
}
