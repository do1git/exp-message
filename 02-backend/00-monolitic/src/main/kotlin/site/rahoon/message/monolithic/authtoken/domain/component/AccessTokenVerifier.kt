package site.rahoon.message.monolithic.authtoken.domain

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.IncorrectClaimException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SecurityException
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.domain.DomainException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Access Token 검증 컴포넌트
 * JWT 기반 access token을 검증하고 클레임을 추출합니다.
 */
@Component
class AccessTokenVerifier(
    private val properties: AuthTokenProperties,
) {
    private val secretKey = Keys.hmacShaKeyFor(properties.accessTokenSecret.toByteArray())

    /**
     * Access Token을 검증하고 AccessToken 도메인 객체로 변환합니다.
     *
     * @param token JWT 토큰 문자열 (Bearer 접두사가 있으면 자동으로 제거됨)
     * @return AccessToken (검증된 토큰 정보)
     * @throws DomainException 토큰이 유효하지 않거나 만료된 경우
     */
    fun verify(token: String): AccessToken {
        // Bearer 접두사 제거
        val cleanToken = token.trim().removePrefix("Bearer ").removePrefix("bearer ")

        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.issuer)
                    .require("typ", "access")
                    .build()
                    .parseSignedClaims(cleanToken)
                    .payload

            val userId = claims.subject ?: throw DomainException(AuthTokenError.INVALID_TOKEN)
            val sessionId =
                claims.get("sid", String::class.java)
                    ?: throw DomainException(AuthTokenError.INVALID_TOKEN)

            val expiresAt =
                claims.expiration?.toInstant()
                    ?: throw DomainException(AuthTokenError.INVALID_TOKEN)

            return AccessToken(
                token = cleanToken,
                expiresAt =
                    LocalDateTime
                        .ofInstant(expiresAt, ZoneId.systemDefault())
                        .truncatedTo(ChronoUnit.SECONDS),
                // 밀리초 제거
                userId = userId,
                sessionId = sessionId,
            )
        } catch (e: ExpiredJwtException) {
            throw DomainException(AuthTokenError.TOKEN_EXPIRED)
        } catch (e: MalformedJwtException) {
            throw DomainException(AuthTokenError.INVALID_TOKEN)
        } catch (e: IncorrectClaimException) {
            throw DomainException(
                error = AuthTokenError.INVALID_TOKEN,
                details = mapOf("message" to (e.message ?: "Invalid claim")),
            )
        } catch (e: SecurityException) {
            throw DomainException(AuthTokenError.INVALID_TOKEN)
        } catch (e: IllegalArgumentException) {
            throw DomainException(AuthTokenError.INVALID_TOKEN)
        } catch (e: DomainException) {
            throw e
        }
    }
}
