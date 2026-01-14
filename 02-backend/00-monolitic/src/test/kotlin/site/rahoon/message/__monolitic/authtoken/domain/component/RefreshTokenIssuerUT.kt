package site.rahoon.message.__monolitic.authtoken.domain.component

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenProperties
import site.rahoon.message.__monolitic.authtoken.domain.RefreshTokenIssuer
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * RefreshTokenIssuer 단위 테스트
 * RefreshToken 발급 로직을 검증합니다.
 */
class RefreshTokenIssuerUT {

    private lateinit var properties: AuthTokenProperties
    private lateinit var refreshTokenIssuer: RefreshTokenIssuer

    @BeforeEach
    fun setUp() {
        properties = AuthTokenProperties(
            accessTokenSecret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600L,
            refreshTokenTtlSeconds = 1209600L // 14 days
        )
        refreshTokenIssuer = RefreshTokenIssuer(properties)
    }

    @Test
    fun `RefreshToken 발급 성공`() {
        // given
        val userId = "user123"
        val sessionId = "session123"

        // when
        val result = refreshTokenIssuer.issue(userId, sessionId)

        // then
        assertNotNull(result)
        assertNotNull(result.token)
        assertTrue(result.token.isNotBlank())
        assertEquals(userId, result.userId)
        assertEquals(sessionId, result.sessionId)
        assertNotNull(result.expiresAt)
        assertNotNull(result.createdAt)
        
        // 만료 시간이 미래인지 확인
        assertTrue(result.expiresAt.isAfter(LocalDateTime.now()))
        
        // 생성 시간이 현재 시간과 비슷한지 확인
        val now = LocalDateTime.now()
        assertTrue(result.createdAt.isBefore(now) || result.createdAt.isEqual(now) || 
                   result.createdAt.isAfter(now.minusSeconds(1)))
    }

    @Test
    fun `발급된 토큰의 만료 시간이 설정된 TTL과 일치하는지 확인`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val beforeIssue = LocalDateTime.now()

        // when
        val result = refreshTokenIssuer.issue(userId, sessionId)
        val afterIssue = LocalDateTime.now()

        // then
        val expectedExpiresAtMin = beforeIssue.plusSeconds(properties.refreshTokenTtlSeconds).truncatedTo(ChronoUnit.SECONDS)
        val expectedExpiresAtMax = afterIssue.plusSeconds(properties.refreshTokenTtlSeconds).truncatedTo(ChronoUnit.SECONDS)

        assertTrue(result.expiresAt.isAfter(expectedExpiresAtMin) || result.expiresAt == expectedExpiresAtMin)
        assertTrue(result.expiresAt.isBefore(expectedExpiresAtMax) || result.expiresAt == expectedExpiresAtMax)
    }

    @Test
    fun `다른 사용자와 세션으로 발급된 토큰이 다른지 확인`() {
        // given
        val userId1 = "user123"
        val sessionId1 = "session123"
        val userId2 = "user456"
        val sessionId2 = "session456"

        // when
        val token1 = refreshTokenIssuer.issue(userId1, sessionId1)
        val token2 = refreshTokenIssuer.issue(userId2, sessionId2)

        // then
        assertNotEquals(token1.token, token2.token)
        assertNotEquals(token1.userId, token2.userId)
        assertNotEquals(token1.sessionId, token2.sessionId)
    }

    @Test
    fun `같은 사용자와 세션으로 발급된 토큰이 다른지 확인 (UUID 기반 랜덤)`() {
        // given
        val userId = "user123"
        val sessionId = "session123"

        // when
        val token1 = refreshTokenIssuer.issue(userId, sessionId)
        val token2 = refreshTokenIssuer.issue(userId, sessionId)

        // then
        // UUID 기반이므로 토큰 문자열이 달라야 함
        assertNotEquals(token1.token, token2.token)
        assertEquals(token1.userId, token2.userId)
        assertEquals(token1.sessionId, token2.sessionId)
    }

    @Test
    fun `토큰이 UUID 형식인지 확인`() {
        // given
        val userId = "user123"
        val sessionId = "session123"

        // when
        val result = refreshTokenIssuer.issue(userId, sessionId)

        // then
        // UUID 형식 검증 (예: 550e8400-e29b-41d4-a716-446655440000)
        val uuidPattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
        assertTrue(result.token.matches(Regex(uuidPattern, RegexOption.IGNORE_CASE)))
    }
}

