package site.rahoon.message.monolithic.common.global

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import site.rahoon.message.monolithic.common.test.IntegrationTestBase

class SpanRunnerIT(
    private val spanRunner: SpanRunner,
) : IntegrationTestBase() {
    @Test
    fun `runWithSpan - action 실행`() {
        var executed = false
        SpanRunner.runWithSpan("test-run") {
            executed = true
        }
        executed shouldBe true
    }

    @Test
    fun `runWithSpan - 실행 중 MDC에 traceId와 spanId 설정`() {
        var traceId: String? = null
        var spanId: String? = null
        SpanRunner.runWithSpan("test-mdc") {
            traceId = MDC.get("traceId")
            spanId = MDC.get("spanId")
        }
        traceId.shouldNotBeBlank()
        spanId.shouldNotBeBlank()
    }

    @Test
    fun `runWithSpan - 실행 후 MDC 정리`() {
        SpanRunner.runWithSpan("test-cleanup") {
            MDC.get("traceId").shouldNotBeBlank()
        }
        MDC.get("traceId").shouldBeNull()
        MDC.get("spanId").shouldBeNull()
    }

    @Test
    fun `runWithSpan - action 예외 시 예외 전파`() {
        shouldThrow<RuntimeException> {
            SpanRunner.runWithSpan("test-exception") {
                throw RuntimeException("의도된 예외")
            }
        }.message shouldBe "의도된 예외"
    }

    @Test
    fun `runWithSpan - action 예외 시에도 MDC 정리`() {
        shouldThrow<RuntimeException> {
            SpanRunner.runWithSpan("test-exception-cleanup") {
                throw RuntimeException("의도된 예외")
            }
        }
        MDC.get("traceId").shouldBeNull()
        MDC.get("spanId").shouldBeNull()
    }

    @Test
    fun `supplyWithSpan - 반환값`() {
        val result = SpanRunner.supplyWithSpan("test-supply") {
            42
        }
        result shouldBe 42
    }

    @Test
    fun `supplyWithSpan - 실행 중 MDC에 traceId와 spanId 설정`() {
        val result = SpanRunner.supplyWithSpan("test-supply-mdc") {
            MDC.get("traceId") to MDC.get("spanId")
        }
        result.first.shouldNotBeBlank()
        result.second.shouldNotBeBlank()
    }

    @Test
    fun `supplyWithSpan - action 예외 시 예외 전파`() {
        shouldThrow<IllegalStateException> {
            SpanRunner.supplyWithSpan("test-supply-exception") {
                throw IllegalStateException("supply 예외")
            }
        }.message shouldBe "supply 예외"
    }

    @Test
    fun `runWithSpan 중첩 - parent-child traceId 동일, spanId 상이`() {
        var parentTraceId: String? = null
        var parentSpanId: String? = null
        var childTraceId: String? = null
        var childSpanId: String? = null

        SpanRunner.runWithSpan("parent") {
            parentTraceId = MDC.get("traceId")
            parentSpanId = MDC.get("spanId")

            SpanRunner.runWithSpan("child") {
                childTraceId = MDC.get("traceId")
                childSpanId = MDC.get("spanId")
            }
        }

        parentTraceId.shouldNotBeBlank()
        parentSpanId.shouldNotBeBlank()
        childTraceId.shouldNotBeBlank()
        childSpanId.shouldNotBeBlank()
        parentTraceId shouldBe childTraceId
        parentSpanId shouldNotBe childSpanId
    }

    @Test
    fun `runWithSpan 중첩 - 완료 후 MDC 정리`() {
        SpanRunner.runWithSpan("outer") {
            SpanRunner.runWithSpan("inner") {
                MDC.get("traceId").shouldNotBeBlank()
            }
        }
        MDC.get("traceId").shouldBeNull()
        MDC.get("spanId").shouldBeNull()
    }
}
