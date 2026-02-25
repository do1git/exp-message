package site.rahoon.message.monolithic.authtoken.domain

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.authtoken.domain.component.AccessTokenIssuer
import site.rahoon.message.monolithic.authtoken.domain.component.AccessTokenVerifier
import site.rahoon.message.monolithic.authtoken.domain.component.RefreshTokenIssuer
import site.rahoon.message.monolithic.common.domain.DomainException
import java.util.UUID

@Service
class AuthTokenDomainService(
    private val accessTokenIssuer: AccessTokenIssuer,
    private val accessTokenVerifier: AccessTokenVerifier,
    private val refreshTokenIssuer: RefreshTokenIssuer,
    private val authTokenRepository: AuthTokenRepository,
) {
    /**
     * @param role 애플리케이션 레이어에서 전달받은 사용자 역할 (ADMIN, USER)
     */
    fun issueToken(
        userId: String,
        role: String,
        prevSessionId: String? = null,
    ): AuthToken {
        val sessionId = prevSessionId ?: UUID.randomUUID().toString()
        val accessToken = accessTokenIssuer.issue(userId, sessionId, role)
        val refreshToken = refreshTokenIssuer.issue(userId, sessionId)
        authTokenRepository.saveRefreshToken(refreshToken)
        return AuthToken(accessToken, refreshToken)
    }

    fun verifyAccessToken(accessToken: String): AccessToken = accessTokenVerifier.verify(accessToken)

    /**
     * Refresh Token이 존재하면 반환하고, 없으면 null을 반환합니다.
     * 토큰을 소비하지 않습니다.
     */
    fun findRefreshToken(refreshTokenString: String): RefreshToken? = authTokenRepository.findRefreshToken(refreshTokenString)

    fun expireBySessionId(sessionId: String) {
        authTokenRepository.deleteRefreshTokenBySessionId(sessionId)
    }

    /**
     * Refresh Token으로 새 토큰을 발급합니다.
     *
     * @param refreshTokenString Refresh Token 문자열
     * @param role 애플리케이션 레이어에서 전달받은 사용자 역할 (ADMIN, USER)
     */
    fun refresh(
        refreshTokenString: String,
        role: String,
    ): AuthToken {
        val refreshToken =
            authTokenRepository.findRefreshToken(refreshTokenString)
                ?: throw DomainException(
                    error = AuthTokenError.INVALID_TOKEN,
                    details = mapOf("refreshToken" to refreshTokenString),
                )
        authTokenRepository.deleteRefreshToken(refreshToken.token)
        return issueToken(refreshToken.userId, role, refreshToken.sessionId)
    }
}
