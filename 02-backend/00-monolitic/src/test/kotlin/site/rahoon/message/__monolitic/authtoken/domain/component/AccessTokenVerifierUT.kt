package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.rahoon.message.__monolitic.authtoken.domain.AccessToken
import site.rahoon.message.__monolitic.authtoken.domain.AccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.AccessTokenVerifier
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenProperties
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * AccessTokenVerifier 단위 테스트
 * JWT 토큰 검증 로직을 검증합니다.
 */
class AccessTokenVerifierUT {

    private lateinit var properties: AuthTokenProperties
    private lateinit var accessTokenIssuer: AccessTokenIssuer
    private lateinit var accessTokenVerifier: AccessTokenVerifier
    private val secretKey: javax.crypto.SecretKey

    init {
        val secret = "please-change-me-please-change-me-please-change-me-32bytes"
        secretKey = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    @BeforeEach
    fun setUp() {
        properties = AuthTokenProperties(
            accessTokenSecret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600L
        )
        accessTokenIssuer = AccessTokenIssuer(properties)
        accessTokenVerifier = AccessTokenVerifier(properties)
    }

    @Test
    fun `유효한 AccessToken 검증 성공`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val issuedToken = accessTokenIssuer.issue(userId, sessionId)

        // when
        val result = accessTokenVerifier.verify(issuedToken.token)

        // then
        assertNotNull(result)
        assertEquals(issuedToken.token, result.token)
        assertEquals(userId, result.userId)
        assertEquals(sessionId, result.sessionId)
        assertEquals(issuedToken.expiresAt, result.expiresAt)
    }

    @Test
    fun `Bearer 접두사가 있는 토큰 검증 성공`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val issuedToken = accessTokenIssuer.issue(userId, sessionId)
        val tokenWithBearer = "Bearer ${issuedToken.token}"

        // when
        val result = accessTokenVerifier.verify(tokenWithBearer)

        // then
        assertNotNull(result)
        assertEquals(issuedToken.token, result.token)
        assertEquals(userId, result.userId)
        assertEquals(sessionId, result.sessionId)
    }

    @Test
    fun `bearer 소문자 접두사가 있는 토큰 검증 성공`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val issuedToken = accessTokenIssuer.issue(userId, sessionId)
        val tokenWithBearer = "bearer ${issuedToken.token}"

        // when
        val result = accessTokenVerifier.verify(tokenWithBearer)

        // then
        assertNotNull(result)
        assertEquals(issuedToken.token, result.token)
        assertEquals(userId, result.userId)
        assertEquals(sessionId, result.sessionId)
    }

    @Test
    fun `만료된 토큰 검증 시 예외 발생`() {
        // given
        val userId = "user123"
        val sessionId = "session123"
        val expiredToken = createExpiredToken(userId, sessionId)

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(expiredToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.TOKEN_EXPIRED, exception.error)
    }

    @Test
    fun `잘못된 서명의 토큰 검증 시 예외 발생`() {
        // given
        val wrongSecret = "wrong-secret-key-wrong-secret-key-wrong-secret-key-32"
        val wrongKey = Keys.hmacShaKeyFor(wrongSecret.toByteArray())
        val invalidToken = Jwts.builder()
            .issuer(properties.issuer)
            .subject("user123")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .claim("typ", "access")
            .claim("uid", "user123")
            .claim("sid", "session123")
            .signWith(wrongKey)
            .compact()

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(invalidToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `잘못된 issuer의 토큰 검증 시 예외 발생`() {
        // given
        val invalidToken = Jwts.builder()
            .issuer("wrong-issuer")
            .subject("user123")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .claim("typ", "access")
            .claim("uid", "user123")
            .claim("sid", "session123")
            .signWith(secretKey)
            .compact()

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(invalidToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `잘못된 typ 클레임의 토큰 검증 시 예외 발생`() {
        // given
        val invalidToken = Jwts.builder()
            .issuer(properties.issuer)
            .subject("user123")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(3600)))
            .claim("typ", "refresh") // 잘못된 타입
            .claim("uid", "user123")
            .claim("sid", "session123")
            .signWith(secretKey)
            .compact()

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(invalidToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `형식이 잘못된 토큰 검증 시 예외 발생`() {
        // given
        val malformedToken = "not.a.valid.jwt.token"

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(malformedToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.INVALID_TOKEN, exception.error)
    }

    @Test
    fun `빈 토큰 검증 시 예외 발생`() {
        // given
        val emptyToken = ""

        // when & then
        val exception = assertThrows<DomainException> {
            accessTokenVerifier.verify(emptyToken)
        }
        
        assertEquals(site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError.INVALID_TOKEN, exception.error)
    }

    private fun createExpiredToken(userId: String, sessionId: String): String {
        return Jwts.builder()
            .issuer(properties.issuer)
            .subject(userId)
            .issuedAt(Date.from(Instant.now().minusSeconds(7200))) // 2시간 전
            .expiration(Date.from(Instant.now().minusSeconds(3600))) // 1시간 전에 만료
            .claim("typ", "access")
            .claim("uid", userId)
            .claim("sid", sessionId)
            .signWith(secretKey)
            .compact()
    }
}

