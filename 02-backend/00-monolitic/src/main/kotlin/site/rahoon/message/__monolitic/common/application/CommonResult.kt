package site.rahoon.message.__monolitic.common.application

/**
 * Application 레이어에서 사용하는 공통 결과 타입
 */
object CommonResult {
    /**
     * 커서 기반 페이징 결과
     *
     * - items: 실제 응답 데이터 목록
     * - nextCursor: 다음 페이지 커서(없으면 null)
     * - limit: 실제 적용된 limit(기본값/최대값 clamp 반영)
     */
    data class Page<T>(
        val items: List<T>,
        val nextCursor: String?,
        val limit: Int
    )
}

