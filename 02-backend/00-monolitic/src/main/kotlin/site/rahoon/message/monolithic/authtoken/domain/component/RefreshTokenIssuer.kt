package site.rahoon.message.monolithic.authtoken.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenProperties
import site.rahoon.message.monolithic.authtoken.domain.RefreshToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Refresh Token 발급 컴포넌트
 * DB 저장용 stateful refresh token을 생성합니다.
 */
@Component
class RefreshTokenIssuer(
    private val properties: AuthTokenProperties,
) {
    /**
     * Refresh Token을 발급합니다.
     *
     * @param userId 사용자 ID
     * @param sessionId 세션 ID
     * @return RefreshToken (랜덤 토큰 문자열과 만료 시간 포함)
     */
    fun issue(
        userId: String,
        sessionId: String,
    ): RefreshToken {
        val now = Instant.now()
        val expiresAt =
            now
                .plusSeconds(properties.refreshTokenTtlSeconds)
                .truncatedTo(ChronoUnit.SECONDS) // 밀리초 제거

        // 랜덤 토큰 생성 (UUID 기반)
        val token = UUID.randomUUID().toString()

        return RefreshToken(
            token = token,
            expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()),
            userId = userId,
            sessionId = sessionId,
            createdAt = LocalDateTime.ofInstant(now, ZoneId.systemDefault()),
        )
    }
}
