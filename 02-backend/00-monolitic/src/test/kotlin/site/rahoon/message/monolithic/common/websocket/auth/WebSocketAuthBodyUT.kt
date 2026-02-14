package site.rahoon.message.monolithic.common.websocket.auth

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

/**
 * WebSocketAuthBody 단위 테스트.
 */
class WebSocketAuthBodyUT {
    @Test
    fun `EVENT_TOKEN_EXPIRING_SOON 상수`() {
        WebSocketAuthBody.EVENT_TOKEN_EXPIRING_SOON shouldBe "token_expiring_soon"
    }

    @Test
    fun `정형 body 생성`() {
        val body = WebSocketAuthBody(
            event = WebSocketAuthBody.EVENT_TOKEN_EXPIRING_SOON,
            expiresAt = "2026-02-12T14:30:00+09:00[Asia/Seoul]",
            websocketSessionId = "ws-session-123",
            occurredAt = ZonedDateTime.parse("2026-02-12T14:28:00+09:00[Asia/Seoul]"),
        )

        body.event shouldBe "token_expiring_soon"
        body.expiresAt shouldBe "2026-02-12T14:30:00+09:00[Asia/Seoul]"
        body.websocketSessionId shouldBe "ws-session-123"
        body.occurredAt.shouldNotBeNull()
    }

    @Test
    fun `occurredAt nullable`() {
        val body = WebSocketAuthBody(
            event = WebSocketAuthBody.EVENT_TOKEN_EXPIRING_SOON,
            expiresAt = "2026-02-12T14:30:00Z",
            websocketSessionId = "ws-1",
            occurredAt = null,
        )

        body.occurredAt shouldBe null
    }
}
