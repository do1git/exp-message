package site.rahoon.message.monolithic.user.controller

import site.rahoon.message.monolithic.user.domain.UserInfo
import site.rahoon.message.monolithic.user.domain.UserRole
import java.time.LocalDateTime

/**
 * Admin User Controller 응답 DTO
 */
object AdminUserResponse {
    data class Detail(
        val id: String,
        val email: String,
        val nickname: String,
        val role: UserRole,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(userInfo: UserInfo.Detail): Detail =
                Detail(
                    id = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                    role = userInfo.role,
                    createdAt = userInfo.createdAt,
                    updatedAt = userInfo.updatedAt,
                )
        }
    }
}
