package site.rahoon.message.__monolitic.common.domain

/**
 * 에러 타입
 * 도메인 레이어에서 HTTP에 의존하지 않고 에러의 성격을 표현합니다.
 */
enum class ErrorType {
    /**
     * 클라이언트 요청 오류 (4xx)
     * 잘못된 요청, 유효성 검증 실패 등
     */
    CLIENT_ERROR,

    /**
     * 리소스를 찾을 수 없음 (404)
     */
    NOT_FOUND,

    /**
     * 리소스 충돌 (409)
     * 중복 생성, 동시 수정 등
     */
    CONFLICT,

    /**
     * 서버 오류 (5xx)
     * 내부 서버 오류, 예상치 못한 오류 등
     */
    SERVER_ERROR
}

interface DomainError {
    val code: String
    val message: String
    val type: ErrorType
}

