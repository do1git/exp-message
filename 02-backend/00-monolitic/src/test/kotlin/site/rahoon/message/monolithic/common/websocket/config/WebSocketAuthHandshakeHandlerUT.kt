package site.rahoon.message.monolithic.common.websocket.config

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import site.rahoon.message.monolithic.common.controller.CommonAuthInfo
import site.rahoon.message.monolithic.common.controller.filter.AuthTokenResolver
import java.net.URI

/**
 * WebSocketAuthHandshakeHandler 단위 테스트
 * determineUser(protected)는 테스트용 서브클래스로 노출하여 검증
 */
class WebSocketAuthHandshakeHandlerUT {
    private fun createHandler(authTokenResolver: AuthTokenResolver): TestableWebSocketAuthHandshakeHandler =
        TestableWebSocketAuthHandshakeHandler(authTokenResolver)

    @Test
    fun `access_token 쿼리가 있으면 AuthTokenResolver 검증 후 StompPrincipal 반환`() {
        // given
        val userId = "user-1"
        val authTokenResolver = mockk<AuthTokenResolver>()
        every { authTokenResolver.verify("token-query") } returns CommonAuthInfo(userId = userId, sessionId = null)

        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws?access_token=token-query")
        every { request.headers } returns HttpHeaders()

        val handler = createHandler(authTokenResolver)
        val wsHandler = mockk<WebSocketHandler>(relaxed = true)
        val attributes = mutableMapOf<String, Any>()

        // when
        val principal = handler.determineUserPublic(request, wsHandler, attributes)

        // then
        principal shouldBe StompPrincipal(userId, null)
        (principal as StompPrincipal).getName() shouldBe userId
    }

    @Test
    fun `Authorization 헤더가 있으면 AuthTokenResolver 검증 후 StompPrincipal 반환`() {
        // given
        val userId = "user-2"
        val authTokenResolver = mockk<AuthTokenResolver>()
        every { authTokenResolver.verify("Bearer token-header") } returns CommonAuthInfo(userId = userId, sessionId = null)

        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws")
        every { request.headers } returns HttpHeaders().apply { set("Authorization", "Bearer token-header") }

        val handler = createHandler(authTokenResolver)
        val wsHandler = mockk<WebSocketHandler>(relaxed = true)
        val attributes = mutableMapOf<String, Any>()

        // when
        val principal = handler.determineUserPublic(request, wsHandler, attributes)

        // then
        principal shouldBe StompPrincipal(userId, null)
    }

    @Test
    fun `쿼리 우선 - access_token과 Authorization 둘 다 있으면 access_token 사용`() {
        // given
        val authTokenResolver = mockk<AuthTokenResolver>()
        every { authTokenResolver.verify("from-query") } returns CommonAuthInfo(userId = "q", sessionId = null)

        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws?access_token=from-query")
        every { request.headers } returns HttpHeaders().apply { set("Authorization", "Bearer from-header") }

        val handler = createHandler(authTokenResolver)
        val wsHandler = mockk<WebSocketHandler>(relaxed = true)
        val attributes = mutableMapOf<String, Any>()

        // when
        val principal = handler.determineUserPublic(request, wsHandler, attributes)

        // then
        (principal as StompPrincipal).userId shouldBe "q"
    }

    @Test
    fun `토큰이 없으면 null 반환`() {
        // given
        val authTokenResolver = mockk<AuthTokenResolver>(relaxed = true)

        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws")
        every { request.headers } returns HttpHeaders()

        val handler = createHandler(authTokenResolver)
        val wsHandler = mockk<WebSocketHandler>(relaxed = true)
        val attributes = mutableMapOf<String, Any>()

        // when
        val principal = handler.determineUserPublic(request, wsHandler, attributes)

        // then
        principal.shouldBeNull()
        verify(exactly = 0) { authTokenResolver.verify(any()) }
    }

    /**
     * protected determineUser를 테스트에서 호출하기 위한 서브클래스
     */
    private class TestableWebSocketAuthHandshakeHandler(
        authTokenResolver: AuthTokenResolver,
    ) : WebSocketAuthHandshakeHandler(authTokenResolver) {
        fun determineUserPublic(
            request: ServerHttpRequest,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>,
        ): java.security.Principal? = super.determineUser(request, wsHandler, attributes)
    }
}
