package site.rahoon.message.__monolitic.common.domain

import site.rahoon.message.__monolitic.common.global.ErrorType

/**
 * 공통 도메인 에러
 * 특정 도메인에 종속되지 않는 공통 에러를 정의합니다.
 */
enum class CommonError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    NOT_FOUND("COMMON_001", "Resource not found", ErrorType.NOT_FOUND),
    CONFLICT("COMMON_002", "Resource conflict", ErrorType.CONFLICT),
    CLIENT_ERROR("COMMON_003", "Invalid request", ErrorType.CLIENT_ERROR),
    UNAUTHORIZED("COMMON_005", "Unauthorized", ErrorType.UNAUTHORIZED),
    SERVER_ERROR("COMMON_004", "Internal server error", ErrorType.SERVER_ERROR),

    INVALID_PAGE_CURSOR("COMMON_006", "Invalid page cursor", ErrorType.CLIENT_ERROR),
    INVALID_PAGE_LIMIT("COMMON_007", "Invalid page limit", ErrorType.CLIENT_ERROR)
}

