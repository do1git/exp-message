package site.rahoon.message.monolithic.authtoken.controller

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Auth Controller 요청 DTO
 */
object AuthRequest {
    /**
     * 로그인 요청
     */
    data class Login(
        @field:NotBlank(message = "이메일은 필수입니다")
        @field:Email(message = "올바른 이메일 형식이 아닙니다")
        val email: String,
        @field:NotBlank(message = "비밀번호는 필수입니다")
        val password: String,
    )

    /**
     * 토큰 갱신 요청
     */
    data class Refresh(
        @field:NotBlank(message = "리프레시 토큰은 필수입니다")
        val refreshToken: String,
    )
}
