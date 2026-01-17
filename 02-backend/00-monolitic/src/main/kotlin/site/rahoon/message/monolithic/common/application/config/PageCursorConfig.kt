package site.rahoon.message.monolithic.common.application.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import site.rahoon.message.monolithic.common.application.CommonPageCursor

/**
 * Cursor 서명 설정 초기화
 */
@Configuration
@EnableConfigurationProperties(PageCursorProperties::class)
class PageCursorConfig(
    private val cursorProperties: PageCursorProperties,
) {
    @PostConstruct
    fun initializeCursorSignature() {
        CommonPageCursor.initializeSignature(cursorProperties)
    }
}
