package site.rahoon.message.monolithic.common.auth

import java.time.LocalDateTime

/**
 * ADMIN 역할이 검증된 인증 정보.
 *
 * [CommonAuthInfoArgumentResolver]에서 [CommonAdminAuthInfo] 파라미터를 주입할 때
 * role이 ADMIN인 경우에만 반환하며, 그렇지 않으면 FORBIDDEN을 던집니다.
 *
 * @param value 검증된 [CommonAuthInfo]
 */
data class CommonAdminAuthInfo(
    val value: CommonAuthInfo,
) {
    val userId: String get() = value.userId
    val sessionId: String get() = value.sessionId
    val expiresAt: LocalDateTime get() = value.expiresAt
    val role: CommonAuthRole get() = value.role
}
