package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.TaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketConnectInterceptor
import site.rahoon.message.monolithic.common.websocket.config.exception.WebSocketExceptionStompSubProtocolErrorHandler
import site.rahoon.message.monolithic.common.websocket.config.outbound.WebSocketConnectedSessionHeaderInterceptor
import site.rahoon.message.monolithic.common.websocket.config.session.WebSocketSessionExpiryInterceptor
import site.rahoon.message.monolithic.common.websocket.config.subscribe.WebSocketTopicSubscribeInterceptor
import site.rahoon.message.monolithic.common.websocket.config.tracing.WebSocketTracingChannelInterceptor

/**
 * WebSocket(STOMP) 설정
 *
 * - 엔드포인트: /ws (SockJS fallback)
 * - Application destination: /app (SEND 수신, 예: /app/auth/refresh)
 * - Broker: /topic, /queue (구독 prefix. /queue는 reply-queue 등 user queue용)
 * - Handshake: 토큰만 세션에 저장. CONNECT 시 [WebSocketConnectInterceptor]에서 토큰 검증·Principal 설정
 * - 구독: WebSocketTopicSubscribeInterceptor로 /topic/user/{uuid}/... 본인 토픽만 허용
 * - 세션 만료: 인바운드(session.WebSocketSessionExpiryInterceptor) +
 *   Heartbeat 주기(session.WebSocketSessionExpiryHeartbeatTask)에서 만료 검사, 만료 시 ERROR 후 종료
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    @Value("\${websocket.heartbeat-interval-ms:10000}") private val heartbeatIntervalMs: Long,
    private val webSocketAuthHandshakeHandler: WebSocketAuthHandshakeHandler,
    private val webSocketConnectInterceptor: WebSocketConnectInterceptor,
    private val webSocketSessionExpiryInterceptor: WebSocketSessionExpiryInterceptor,
    private val webSocketTopicSubscribeInterceptor: WebSocketTopicSubscribeInterceptor,
    private val webSocketTracingChannelInterceptor: WebSocketTracingChannelInterceptor,
    private val webSocketExceptionStompSubProtocolErrorHandler: WebSocketExceptionStompSubProtocolErrorHandler,
    private val webSocketConnectedSessionHeaderInterceptor: WebSocketConnectedSessionHeaderInterceptor,
    private val webSocketBrokerTaskScheduler: TaskScheduler,
) : WebSocketMessageBrokerConfigurer {
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(
            webSocketTracingChannelInterceptor,
            webSocketConnectInterceptor,
            webSocketSessionExpiryInterceptor,
            webSocketTopicSubscribeInterceptor,
        )
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)

        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)
            .withSockJS()

        // 인바운드 채널·인터셉터 단계 예외만 전달됨. @MessageMapping 내부 예외는 WebSocketMessageExceptionAdvice에서 처리.
        registry.setErrorHandler(webSocketExceptionStompSubProtocolErrorHandler)
    }

    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        registration.interceptors(webSocketConnectedSessionHeaderInterceptor)
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")

        registry
            .enableSimpleBroker("/topic", "/queue")
            .setHeartbeatValue(longArrayOf(heartbeatIntervalMs, heartbeatIntervalMs))
            .setTaskScheduler(webSocketBrokerTaskScheduler)
    }
}
