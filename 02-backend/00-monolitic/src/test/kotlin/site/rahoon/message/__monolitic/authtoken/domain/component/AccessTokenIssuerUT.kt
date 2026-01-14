package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.authtoken.domain.AccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenProperties
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * AccessTokenIssuer 단위 테스트
 * JWT 토큰 발급 로직을 검증합니다.
 */
class AccessTokenIssuerUT {

    private lateinit var properties: AuthTokenProperties
    private lateinit var accessTokenIssuer: AccessTokenIssuer

    @BeforeEach
    fun setUp() {
        properties = AuthTokenProperties(
            accessTokenSecret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600L
        )
        accessTokenIssuer = AccessTokenIssuer(properties)
    }

    @Test
    fun `AccessToken 발급 성공`() {
        // given
        val userId = "user123"
        val sessionId = "session123"

        // when
        val result = accessTokenIssuer.issue(userId, sessionId)

        // then
        assertNotNull(result)
        assertNotNull(result.token)
        assertTrue(result.token.isNotBlank())
        assertEquals(userId, result.userId)
        assertEquals(sessionId, result.sessionId)
        assertNotNull(result.expiresAt)
        
        // 만료 시간이 미래인지 확인
        assertTrue(result.expiresAt.isAfter(LocalDateTime.now()))
        
        // JWT 토큰 구조 검증
        val secretKey = Keys.hmacShaKeyFor(properties.accessTokenSecret.toByteArray())
        val claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(result.token)
            .payload

        assertEquals(properties.issuer, claims.issuer)
        assertEquals(userId, claims.subject)
        assertEquals("access", claims.get("typ", String::class.java))
        assertEquals(userId, claims.get("uid", String::class.java))
        assertEquals(sessionId, claims.get("sid", String::class.java))
        assertNotNull(claims.id) // jti
        assertNotNull(claims.issuedAt)
        assertNotNull(claims.expiration)
    }

    @Test
    fun `발급된 토큰의 만료 시간이 설정된 TTL과 일치하는지 확인`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val beforeIssue = Instant.now()

        // when
        val result = accessTokenIssuer.issue(userId, sessionId)
        val afterIssue = Instant.now()

        // then
        val expectedExpiresAtMin = beforeIssue.plusSeconds(properties.accessTokenTtlSeconds).truncatedTo(ChronoUnit.SECONDS)
        val expectedExpiresAtMax = afterIssue.plusSeconds(properties.accessTokenTtlSeconds).truncatedTo(ChronoUnit.SECONDS)
        val actualExpiresAt = result.expiresAt.atZone(ZoneId.systemDefault()).toInstant()

        assertTrue(actualExpiresAt.isAfter(expectedExpiresAtMin) || actualExpiresAt == expectedExpiresAtMin)
        assertTrue(actualExpiresAt.isBefore(expectedExpiresAtMax) || actualExpiresAt == expectedExpiresAtMax)
    }

    @Test
    fun `다른 사용자와 세션으로 발급된 토큰이 다른지 확인`() {
        // given
        val userId1 = "user123"
        val sessionId1 = "session123"
        val userId2 = "user456"
        val sessionId2 = "session456"

        // when
        val token1 = accessTokenIssuer.issue(userId1, sessionId1)
        val token2 = accessTokenIssuer.issue(userId2, sessionId2)

        // then
        assertNotEquals(token1.token, token2.token)
        assertNotEquals(token1.userId, token2.userId)
        assertNotEquals(token1.sessionId, token2.sessionId)
    }

    @Test
    fun `같은 사용자와 세션으로 발급된 토큰이 다른지 확인 (JTI가 다름)`() {
        // given
        val userId = "user123"
        val sessionId = "session123"

        // when
        val token1 = accessTokenIssuer.issue(userId, sessionId)
        val token2 = accessTokenIssuer.issue(userId, sessionId)

        // then
        // JTI가 다르므로 토큰 문자열도 달라야 함
        assertNotEquals(token1.token, token2.token)
        assertEquals(token1.userId, token2.userId)
        assertEquals(token1.sessionId, token2.sessionId)
    }
}

