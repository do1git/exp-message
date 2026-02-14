package site.rahoon.message.monolithic.common.global.config

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.test.context.ContextConfiguration
import site.rahoon.message.monolithic.common.test.IntegrationTestBase

@ContextConfiguration(classes = [ScheduledSpanAspectIT.TestScheduledConfig::class])
class ScheduledSpanAspectIT(
    private val testScheduledComponent: ScheduledSpanAspectIT.TestScheduledComponent,
) : IntegrationTestBase() {
    @Test
    fun `@Scheduled 메서드 실행 시 SpanRunner로 감싸져 MDC에 traceId와 spanId 설정`() {
        testScheduledComponent.scheduledMethod()

        testScheduledComponent.capturedTraceId.shouldNotBeBlank()
        testScheduledComponent.capturedSpanId.shouldNotBeBlank()
        testScheduledComponent.capturedSpanName shouldBe "TestScheduledComponent.scheduledMethod"
    }

    @Test
    fun `@Scheduled 메서드 실행 후 MDC 정리`() {
        testScheduledComponent.scheduledMethod()

        MDC.get("traceId").shouldBeNull()
        MDC.get("spanId").shouldBeNull()
    }

    @TestConfiguration
    @EnableScheduling
    class TestScheduledConfig {
        @Bean
        fun testScheduledComponent() = TestScheduledComponent()
    }

    @Component
    class TestScheduledComponent {
        var capturedTraceId: String? = null
        var capturedSpanId: String? = null
        var capturedSpanName: String? = null

        @Scheduled(fixedRate = 86400000L, initialDelay = 86400000L) // 24h - 테스트 중 스케줄러 실행 방지
        fun scheduledMethod() {
            capturedTraceId = MDC.get("traceId")
            capturedSpanId = MDC.get("spanId")
            capturedSpanName = "TestScheduledComponent.scheduledMethod"
        }
    }
}
