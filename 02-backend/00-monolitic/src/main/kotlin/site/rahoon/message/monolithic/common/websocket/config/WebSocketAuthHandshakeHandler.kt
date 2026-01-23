package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.util.UriComponentsBuilder
import site.rahoon.message.monolithic.common.controller.filter.AuthTokenResolver
import java.security.Principal

/**
 * WebSocket Handshake 시 JWT를 검증하고 Principal(userId)을 설정합니다.
 *
 * - 쿼리: `access_token`
 * - 헤더: `Authorization` (Bearer)
 *
 * `AuthTokenResolver`로 검증 후 실패 시 Handshake가 거부됩니다.
 */
@Component
class WebSocketAuthHandshakeHandler(
    private val authTokenResolver: AuthTokenResolver,
) : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val tokenFromQuery =
            UriComponentsBuilder
                .fromUri(request.uri)
                .build()
                .queryParams
                .getFirst("access_token")
        val tokenFromHeader = request.headers.getFirst("Authorization")
        val token =
            tokenFromQuery?.takeIf { it.isNotBlank() } ?: tokenFromHeader?.takeIf { it.isNotBlank() }
        if (token.isNullOrBlank()) return null
        val authInfo = authTokenResolver.verify(token)
        return StompPrincipal(
            authInfo.userId,
            authInfo.sessionId,
        )
    }
}

/**
 * STOMP Principal. [getName]이 userId를 반환합니다.
 */
data class StompPrincipal(
    val userId: String,
    val sessionId: String?,
) : Principal {
    override fun getName(): String = userId
}
