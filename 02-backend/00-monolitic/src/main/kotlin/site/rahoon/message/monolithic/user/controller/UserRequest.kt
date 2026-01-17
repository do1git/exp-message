package site.rahoon.message.monolithic.user.controller

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.user.application.UserCriteria

/**
 * User Controller 요청 DTO
 */
object UserRequest {
    /**
     * 회원가입 요청
     */
    data class SignUp(
        @field:NotBlank(message = "이메일은 필수입니다")
        @field:Email(message = "올바른 이메일 형식이 아닙니다")
        val email: String,
        @field:NotBlank(message = "비밀번호는 필수입니다")
        @field:Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
        val password: String,
        @field:NotBlank(message = "닉네임은 필수입니다")
        @field:Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
        val nickname: String,
    ) {
        /**
         * UserCriteria.Register로 변환합니다.
         */
        fun toCriteria(): UserCriteria.Register =
            UserCriteria.Register(
                email = this.email,
                password = this.password,
                nickname = this.nickname,
            )
    }
}
