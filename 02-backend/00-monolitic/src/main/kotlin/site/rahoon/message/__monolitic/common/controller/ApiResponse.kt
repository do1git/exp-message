package site.rahoon.message.__monolitic.common.controller

import java.time.ZonedDateTime

/**
 * 공통 API 응답 템플릿
 * success 플래그로 성공/실패를 구분하며, 성공 시 data, 실패 시 error를 포함합니다.
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorInfo? = null
) {
    /**
     * 에러 정보
     */
    data class ErrorInfo(
        val code: String,
        val message: String,
        val details: Map<String, Any>? = null,
        val occurredAt: ZonedDateTime? = null,
        val path: String? = null
    )

    companion object {
        /**
         * 성공 응답 생성
         */
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                error = null
            )
        }

        /**
         * 에러 응답 생성
         */
        fun <T> error(
            code: String,
            message: String,
            details: Map<String, Any>? = null,
            occurredAt: ZonedDateTime? = null,
            path: String? = null
        ): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                error = ErrorInfo(code, message, details, occurredAt, path)
            )
        }
    }
}

