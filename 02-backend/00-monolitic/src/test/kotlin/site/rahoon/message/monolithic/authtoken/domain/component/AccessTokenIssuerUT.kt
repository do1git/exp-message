package site.rahoon.message.monolithic.authtoken.domain.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenProperties
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
        properties =
            AuthTokenProperties(
                accessTokenSecret = "please-change-me-please-change-me-please-change-me-32bytes",
                issuer = "site.rahoon.message.test",
                accessTokenTtlSeconds = 3600L,
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
        result.shouldNotBeNull()
        result.token.shouldNotBeBlank()
        result.userId shouldBe userId
        result.sessionId shouldBe sessionId
        result.expiresAt.shouldNotBeNull()

        // 만료 시간이 미래인지 확인
        result.expiresAt.shouldBeAfter(LocalDateTime.now())

        // JWT 토큰 구조 검증
        val secretKey = Keys.hmacShaKeyFor(properties.accessTokenSecret.toByteArray())
        val claims =
            Jwts
                .parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(result.token)
                .payload

        claims.issuer shouldBe properties.issuer
        claims.subject shouldBe userId
        claims.get("typ", String::class.java) shouldBe "access"
        claims.get("uid", String::class.java) shouldBe userId
        claims.get("sid", String::class.java) shouldBe sessionId
        claims.id.shouldNotBeNull() // jti
        claims.issuedAt.shouldNotBeNull()
        claims.expiration.shouldNotBeNull()
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

        Assertions.assertTrue(actualExpiresAt.isAfter(expectedExpiresAtMin) || actualExpiresAt == expectedExpiresAtMin)
        Assertions.assertTrue(actualExpiresAt.isBefore(expectedExpiresAtMax) || actualExpiresAt == expectedExpiresAtMax)
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
        Assertions.assertNotEquals(token1.token, token2.token)
        Assertions.assertNotEquals(token1.userId, token2.userId)
        Assertions.assertNotEquals(token1.sessionId, token2.sessionId)
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
        Assertions.assertNotEquals(token1.token, token2.token)
        Assertions.assertEquals(token1.userId, token2.userId)
        Assertions.assertEquals(token1.sessionId, token2.sessionId)
    }
}
