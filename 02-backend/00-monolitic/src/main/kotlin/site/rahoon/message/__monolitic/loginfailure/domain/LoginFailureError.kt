package site.rahoon.message.__monolitic.loginfailure.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

/**
 * 로그인 실패 도메인 에러
 */
enum class LoginFailureError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    ACCOUNT_LOCKED("LOGIN_FAILURE_001", "Account is temporarily locked due to multiple failed login attempts", ErrorType.CLIENT_ERROR)
}
