package site.rahoon.message.monolithic.user.application

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.user.domain.UserCommand

/**
 * User Application Layer 입력 DTO
 */
object UserCriteria {
    data class Register(
        @field:NotBlank(message = "이메일은 필수입니다")
        @field:Email(message = "올바른 이메일 형식이 아닙니다")
        val email: String,
        @field:NotBlank(message = "비밀번호는 필수입니다")
        @field:Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        val password: String,
        @field:NotBlank(message = "닉네임은 필수입니다")
        @field:Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
        val nickname: String,
    ) {
        /**
         * UserCommand.Create로 변환합니다.
         */
        fun toCommand(): UserCommand.Create =
            UserCommand.Create(
                email = this.email,
                password = this.password,
                nickname = this.nickname,
            )
    }

    /** Default admin: create or update always */
    data class CreateOrUpdateAdmin(
        val email: String,
        val password: String,
        val nickname: String,
    )

    /** Default admin: create only if not exists */
    data class CreateAdminIfNotExists(
        val email: String,
        val password: String,
        val nickname: String,
    )
}
