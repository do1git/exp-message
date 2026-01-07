package site.rahoon.message.__monolitic.common.domain

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
    SERVER_ERROR("COMMON_004", "Internal server error", ErrorType.SERVER_ERROR)
}

