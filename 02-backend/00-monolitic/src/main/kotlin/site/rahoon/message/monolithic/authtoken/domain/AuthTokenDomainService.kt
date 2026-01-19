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
    fun issueToken(
        userId: String,
        prevSessionId: String? = null,
    ): AuthToken {
        val sessionId = prevSessionId ?: UUID.randomUUID().toString()
        val accessToken = accessTokenIssuer.issue(userId, sessionId)
        val refreshToken = refreshTokenIssuer.issue(userId, sessionId)
        authTokenRepository.saveRefreshToken(refreshToken)
        return AuthToken(accessToken, refreshToken)
    }

    fun verifyAccessToken(accessToken: String): AccessToken = accessTokenVerifier.verify(accessToken)

    fun expireBySessionId(sessionId: String) {
        authTokenRepository.deleteRefreshTokenBySessionId(sessionId)
    }

    fun refresh(refreshTokenString: String): AuthToken {
        val refreshToken =
            authTokenRepository.findRefreshToken(refreshTokenString)
                ?: throw DomainException(
                    error = AuthTokenError.INVALID_TOKEN,
                    details = mapOf("refreshToken" to refreshTokenString),
                )
        authTokenRepository.deleteRefreshToken(refreshToken.token)
        return issueToken(refreshToken.userId, refreshToken.sessionId)
    }
}
