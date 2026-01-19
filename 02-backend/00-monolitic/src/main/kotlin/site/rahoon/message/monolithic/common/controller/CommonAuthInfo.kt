package site.rahoon.message.monolithic.common.controller

/**
 * 현재 인증된 사용자 정보를 담는 객체
 */
data class CommonAuthInfo(
    val userId: String,
    val sessionId: String? = null,
)
