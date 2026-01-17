package site.rahoon.message.monolithic.authtoken.domain

import java.time.LocalDateTime

/** JWT 기반 stateless access token */
data class AccessToken(
    val token: String,
    val expiresAt: LocalDateTime,
    val userId: String,
    val sessionId: String,
)

/** DB 저장 stateful refresh token */
data class RefreshToken(
    val token: String,
    val expiresAt: LocalDateTime, // TTL
    val userId: String,
    val sessionId: String, // Unique
    val createdAt: LocalDateTime,
)

/** AccessToken과 RefreshToken을 조합한 인증 결과 */
data class AuthToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken?,
)
