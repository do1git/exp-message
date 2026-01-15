package site.rahoon.message.__monolitic.authtoken.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

enum class AuthTokenError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    INVALID_CREDENTIALS("AUTH_001", "Invalid email or password", ErrorType.CLIENT_ERROR),
    TOKEN_EXPIRED("AUTH_002", "Token has expired", ErrorType.CLIENT_ERROR),
    INVALID_TOKEN("AUTH_003", "Invalid token", ErrorType.CLIENT_ERROR),
    TOKEN_NOT_FOUND("AUTH_004", "Token not found", ErrorType.NOT_FOUND),
    UNAUTHORIZED("AUTH_005", "Unauthorized", ErrorType.CLIENT_ERROR)
}

