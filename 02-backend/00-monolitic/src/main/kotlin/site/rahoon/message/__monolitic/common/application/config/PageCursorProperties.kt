package site.rahoon.message.__monolitic.common.application.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "page-cursor")
data class PageCursorProperties(
    /** Cursor HMAC 서명에 사용할 시크릿 키 (최소 16바이트 권장) */
    val secret: String = "",
    /** Cursor 서명 활성화 여부 (기본값: false, secret이 비어있으면 자동으로 false) */
    val signatureEnabled: Boolean = false
) {
    /**
     * 서명이 실제로 활성화되어 있는지 확인
     * (secret이 설정되어 있고 signatureEnabled가 true일 때만)
     */
    fun isSignatureActive(): Boolean {
        return signatureEnabled && secret.isNotBlank()
    }
}
