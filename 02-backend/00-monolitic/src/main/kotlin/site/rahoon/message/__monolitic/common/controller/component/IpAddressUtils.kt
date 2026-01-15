package site.rahoon.message.__monolitic.common.controller.component

import jakarta.servlet.http.HttpServletRequest

/**
 * IP 주소 추출 유틸리티
 */
object IpAddressUtils {
    /**
     * HttpServletRequest에서 클라이언트 IP 주소를 추출합니다.
     * X-Forwarded-For 헤더를 우선 확인하고, 없으면 X-Real-IP, 마지막으로 remoteAddr를 사용합니다.
     *
     * @param request HttpServletRequest
     * @return 클라이언트 IP 주소
     */
    fun getClientIpAddress(request: HttpServletRequest): String {
        // X-Forwarded-For 헤더 확인 (프록시/로드밸런서 뒤에서 사용)
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (xForwardedFor != null && xForwardedFor.isNotBlank()) {
            // X-Forwarded-For는 여러 IP를 쉼표로 구분할 수 있으므로 첫 번째 IP를 사용
            return xForwardedFor.split(",").firstOrNull()?.trim() ?: getFallbackIp(request)
        }

        // X-Real-IP 헤더 확인 (nginx 등에서 설정)
        val xRealIp = request.getHeader("X-Real-IP")
        if (xRealIp != null && xRealIp.isNotBlank()) {
            return xRealIp.trim()
        }

        // 기본 remoteAddr 사용
        return getFallbackIp(request)
    }

    private fun getFallbackIp(request: HttpServletRequest): String {
        val remoteAddr = request.remoteAddr
        return if (remoteAddr.isNotBlank()) {
            remoteAddr
        } else {
            "unknown"
        }
    }
}
