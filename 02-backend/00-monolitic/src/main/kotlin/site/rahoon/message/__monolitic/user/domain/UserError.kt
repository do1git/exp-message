package site.rahoon.message.__monolitic.user.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

enum class UserError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    USER_NOT_FOUND("USER_001", "User not found", ErrorType.NOT_FOUND),
    EMAIL_ALREADY_EXISTS("USER_002", "Email already exists", ErrorType.CONFLICT),
    INVALID_EMAIL("USER_003", "Invalid email format", ErrorType.CLIENT_ERROR),
    INVALID_PASSWORD("USER_004", "Invalid password", ErrorType.CLIENT_ERROR),
    INVALID_NICKNAME("USER_005", "Invalid nickname", ErrorType.CLIENT_ERROR)
}

