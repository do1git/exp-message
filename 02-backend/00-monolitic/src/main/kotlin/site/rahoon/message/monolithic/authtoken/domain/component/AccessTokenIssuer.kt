package site.rahoon.message.monolithic.authtoken.domain.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.authtoken.domain.AccessToken
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenProperties
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

/**
 * Access Token 발급 컴포넌트
 * JWT 기반 stateless access token을 생성합니다.
 */
@Component
class AccessTokenIssuer(
    private val properties: AuthTokenProperties,
) {
    private val secretKey = Keys.hmacShaKeyFor(properties.accessTokenSecret.toByteArray())

    /**
     * Access Token을 발급합니다.
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @return AccessToken (JWT 토큰 문자열과 만료 시간 포함)
     */
    fun issue(
        userId: String,
        sessionId: String,
    ): AccessToken {
        val now = Instant.now()
        val expiresAt =
            now
                .plusSeconds(properties.accessTokenTtlSeconds)
                .truncatedTo(ChronoUnit.SECONDS) // 밀리초 제거
        val jti = UUID.randomUUID().toString() // JWT ID (토큰 인스턴스 추적용)

        val token =
            Jwts
                .builder()
                .issuer(properties.issuer) // iss
                .subject(userId) // sub
                .issuedAt(Date.from(now)) // iat
                .expiration(Date.from(expiresAt)) // exp
                .id(jti) // jti
                .claim("typ", "access") // 커스텀 클레임: 토큰 타입
                .claim("uid", userId) // 커스텀 클레임: 사용자 ID
                .claim("sid", sessionId) // 커스텀 클레임: 세션 ID
                .signWith(secretKey) // HS256 서명
                .compact()

        return AccessToken(
            token = token,
            expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()),
            userId = userId,
            sessionId = sessionId,
        )
    }
}
