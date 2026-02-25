package site.rahoon.message.monolithic.authtoken.domain

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.rahoon.message.monolithic.authtoken.domain.component.AccessTokenIssuer
import site.rahoon.message.monolithic.authtoken.domain.component.AccessTokenVerifier
import site.rahoon.message.monolithic.authtoken.domain.component.RefreshTokenIssuer
import site.rahoon.message.monolithic.common.domain.DomainException
import java.time.LocalDateTime
import java.util.UUID

/**
 * AuthTokenDomainService 단위 테스트
 * 의존성을 Mock으로 처리하여 도메인 로직만 검증합니다.
 */
class AuthTokenDomainServiceUT {
    private lateinit var accessTokenIssuer: AccessTokenIssuer
    private lateinit var accessTokenVerifier: AccessTokenVerifier
    private lateinit var refreshTokenIssuer: RefreshTokenIssuer
    private lateinit var authTokenRepository: AuthTokenRepository
    private lateinit var authTokenDomainService: AuthTokenDomainService

    @BeforeEach
    fun setUp() {
        accessTokenIssuer = mockk()
        accessTokenVerifier = mockk()
        refreshTokenIssuer = mockk()
        authTokenRepository = mockk()
        authTokenDomainService =
            AuthTokenDomainService(
                accessTokenIssuer,
                accessTokenVerifier,
                refreshTokenIssuer,
                authTokenRepository,
            )
    }

    @Test
    fun `토큰 발급 성공 - 새로운 세션 ID 생성`() {
        // given
        val userId = "user123"
        val sessionId = UUID.randomUUID().toString()
        val accessToken =
            AccessToken(
                token = "access-token",
                expiresAt = LocalDateTime.now().plusHours(1),
                userId = userId,
                sessionId = sessionId,
                role = "USER",
            )
        val refreshToken =
            RefreshToken(
                token = "refresh-token",
                expiresAt = LocalDateTime.now().plusDays(14),
                userId = userId,
                sessionId = sessionId,
                createdAt = LocalDateTime.now(),
            )

        every { accessTokenIssuer.issue(any(), any(), any()) } returns accessToken
        every { refreshTokenIssuer.issue(any(), any()) } returns refreshToken
        every { authTokenRepository.saveRefreshToken(any<RefreshToken>()) } returns refreshToken

        // when
        val result = authTokenDomainService.issueToken(userId, "USER")

        // then
        result.shouldNotBeNull()
        result.accessToken shouldBe accessToken
        result.refreshToken shouldBe refreshToken
        result.accessToken.userId shouldBe userId
        result.refreshToken?.userId shouldBe userId

        verify { accessTokenIssuer.issue(any(), any(), any()) }
        verify { refreshTokenIssuer.issue(any(), any()) }
        verify { authTokenRepository.saveRefreshToken(any<RefreshToken>()) }
    }

    @Test
    fun `토큰 발급 성공 - 기존 세션 ID 사용`() {
        // given
        val userId = "user123"
        val prevSessionId = "existing-session-id"
        val accessToken =
            AccessToken(
                token = "access-token",
                expiresAt = LocalDateTime.now().plusHours(1),
                userId = userId,
                sessionId = prevSessionId,
                role = "USER",
            )
        val refreshToken =
            RefreshToken(
                token = "refresh-token",
                expiresAt = LocalDateTime.now().plusDays(14),
                userId = userId,
                sessionId = prevSessionId,
                createdAt = LocalDateTime.now(),
            )

        every { accessTokenIssuer.issue(userId, prevSessionId, any()) } returns accessToken
        every { refreshTokenIssuer.issue(userId, prevSessionId) } returns refreshToken
        every { authTokenRepository.saveRefreshToken(refreshToken) } returns refreshToken

        // when
        val result = authTokenDomainService.issueToken(userId, "USER", prevSessionId)

        // then
        result.shouldNotBeNull()
        result.accessToken shouldBe accessToken
        result.refreshToken shouldBe refreshToken
        result.accessToken.sessionId shouldBe prevSessionId
        result.refreshToken?.sessionId shouldBe prevSessionId

        verify { accessTokenIssuer.issue(userId, prevSessionId, any()) }
        verify { refreshTokenIssuer.issue(userId, prevSessionId) }
        verify { authTokenRepository.saveRefreshToken(refreshToken) }
    }

    @Test
    fun `AccessToken 검증 성공`() {
        // given
        val tokenString = "valid-access-token"
        val accessToken =
            AccessToken(
                token = tokenString,
                expiresAt = LocalDateTime.now().plusHours(1),
                userId = "user123",
                sessionId = "session123",
                role = "USER",
            )

        every { accessTokenVerifier.verify(tokenString) } returns accessToken

        // when
        val result = authTokenDomainService.verifyAccessToken(tokenString)

        // then
        result.shouldNotBeNull()
        result shouldBe accessToken
        verify { accessTokenVerifier.verify(tokenString) }
    }

    @Test
    fun `세션 ID로 토큰 만료 성공`() {
        // given
        val sessionId = "session123"

        every { authTokenRepository.deleteRefreshTokenBySessionId(sessionId) } returns Unit

        // when
        authTokenDomainService.expireBySessionId(sessionId)

        // then
        verify { authTokenRepository.deleteRefreshTokenBySessionId(sessionId) }
        confirmVerified(authTokenRepository)
    }

    @Test
    fun `리프레시 토큰으로 새 토큰 발급 성공`() {
        // given
        val refreshTokenString = "valid-refresh-token"
        val userId = "user123"
        val sessionId = "session123"
        val oldRefreshToken =
            RefreshToken(
                token = refreshTokenString,
                expiresAt = LocalDateTime.now().plusDays(14),
                userId = userId,
                sessionId = sessionId,
                createdAt = LocalDateTime.now().minusDays(1),
            )
        val newAccessToken =
            AccessToken(
                token = "new-access-token",
                expiresAt = LocalDateTime.now().plusHours(1),
                userId = userId,
                sessionId = sessionId,
                role = "USER",
            )
        val newRefreshToken =
            RefreshToken(
                token = "new-refresh-token",
                expiresAt = LocalDateTime.now().plusDays(14),
                userId = userId,
                sessionId = sessionId,
                createdAt = LocalDateTime.now(),
            )

        every { authTokenRepository.findRefreshToken(refreshTokenString) } returns oldRefreshToken
        every { authTokenRepository.deleteRefreshToken(refreshTokenString) } returns Unit
        every { accessTokenIssuer.issue(userId, sessionId, any()) } returns newAccessToken
        every { refreshTokenIssuer.issue(userId, sessionId) } returns newRefreshToken
        every { authTokenRepository.saveRefreshToken(newRefreshToken) } returns newRefreshToken

        // when
        val result = authTokenDomainService.refresh(refreshTokenString, "USER")

        // then
        result.shouldNotBeNull()
        result.accessToken shouldBe newAccessToken
        result.refreshToken shouldBe newRefreshToken
        result.accessToken.userId shouldBe userId
        result.accessToken.sessionId shouldBe sessionId

        verify { authTokenRepository.findRefreshToken(refreshTokenString) }
        verify { authTokenRepository.deleteRefreshToken(refreshTokenString) }
        verify { accessTokenIssuer.issue(userId, sessionId, any()) }
        verify { refreshTokenIssuer.issue(userId, sessionId) }
        verify { authTokenRepository.saveRefreshToken(newRefreshToken) }
    }

    @Test
    fun `리프레시 토큰이 존재하지 않을 때 예외 발생`() {
        // given
        val invalidRefreshToken = "invalid-refresh-token"
        every { authTokenRepository.findRefreshToken(invalidRefreshToken) } returns null

        // when & then
        val exception =
            assertThrows<DomainException> {
                authTokenDomainService.refresh(invalidRefreshToken, "USER")
            }

        exception.error shouldBe AuthTokenError.INVALID_TOKEN
        exception.details?.containsKey("refreshToken") shouldBe true
        exception.details?.get("refreshToken") shouldBe invalidRefreshToken

        verify { authTokenRepository.findRefreshToken(invalidRefreshToken) }
        verify(exactly = 0) { authTokenRepository.deleteRefreshToken(any()) }
        verify(exactly = 0) { authTokenRepository.saveRefreshToken(any<RefreshToken>()) }
        verify(exactly = 0) { accessTokenIssuer.issue(any(), any(), any()) }
        verify(exactly = 0) { refreshTokenIssuer.issue(any(), any()) }
    }
}
