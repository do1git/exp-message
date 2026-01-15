package site.rahoon.message.__monolitic.common.global

/**
 * 에러 타입
 * 도메인 레이어에서 HTTP에 의존하지 않고 에러의 성격을 표현합니다.
 */
enum class ErrorType {
    CLIENT_ERROR,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    SERVER_ERROR
}

