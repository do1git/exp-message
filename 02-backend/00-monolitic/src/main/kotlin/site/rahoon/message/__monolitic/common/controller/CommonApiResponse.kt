package site.rahoon.message.__monolitic.common.controller

import java.time.ZonedDateTime

/**
 * 공통 API 응답 템플릿
 * success 플래그로 성공/실패를 구분하며, 성공 시 data, 실패 시 error를 포함합니다.
 */
open class CommonApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorInfo? = null
) {
    /**
     * 커서 기반 페이징 목록 응답 템플릿
     *
     * JSON 형태:
     * - `success`: true
     * - `data`: 리스트(배열)
     * - `pageInfo.nextCursor`: 다음 페이지 커서 (`null`이면 마지막 페이지)
     * - `pageInfo.limit`: 실제 적용된 limit (기본값/최대값 clamp 결과)
     * - `error`: null
     *
     * 사용 예:
     * - `ApiResponse.Page.success(data, nextCursor, limit)`
     */
    class Page<T>(
        data: List<T>,
        val pageInfo: PageInfo
    ) : CommonApiResponse<List<T>>(
        success = true,
        data = data,
        error = null
    ) {
        data class PageInfo(
            val nextCursor: String?,
            val limit: Int
        )

        companion object {
            fun <T> success(
                data: List<T>,
                nextCursor: String?,
                limit: Int
            ): Page<T> {
                return Page(
                    data = data,
                    pageInfo = PageInfo(nextCursor = nextCursor, limit = limit)
                )
            }
        }
    }

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
        fun <T> success(data: T): CommonApiResponse<T> {
            return CommonApiResponse(
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
        ): CommonApiResponse<T> {
            return CommonApiResponse(
                success = false,
                data = null,
                error = ErrorInfo(code, message, details, occurredAt, path)
            )
        }
    }
}

