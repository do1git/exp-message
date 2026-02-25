package site.rahoon.message.monolithic.user.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import site.rahoon.message.monolithic.user.domain.UserRole

/**
 * Admin User Controller 요청 DTO
 */
object AdminUserRequest {
    data class UpdateRole(
        @field:NotBlank(message = "역할은 필수입니다")
        @field:Pattern(regexp = "^(ADMIN|USER)$", message = "역할은 ADMIN 또는 USER만 가능합니다")
        val role: String,
    ) {
        fun toUserRole(): UserRole = UserRole.fromCode(role)
    }
}
