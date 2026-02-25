package site.rahoon.message.monolithic.common.auth

import java.time.LocalDateTime

/**
 * 현재 인증된 사용자 정보를 담는 객체
 *
 * @param sessionId JWT sid(세션 식별자)
 * @param expiresAt JWT exp(만료 시각). WebSocket 세션 만료 검사용.
 * @param role 시스템 레벨 사용자 역할 (ADMIN, USER)
 */
data class CommonAuthInfo(
    val userId: String,
    val sessionId: String,
    val expiresAt: LocalDateTime,
    val role: CommonAuthRole,
)
