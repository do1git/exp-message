package site.rahoon.message.monolithic.common.global

import io.micrometer.tracing.Tracer
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * trace context가 없는 스레드에서 실행 시 span을 생성해 MDC(traceId/spanId)를 설정하는 헬퍼.
 *
 * @Scheduled, ThreadPoolTaskScheduler 등 요청 context가 없는 작업에서 사용.
 * 실행마다 새 trace가 생성되어 해당 실행 내 로그를 traceId로 상관시킬 수 있다.
 */
@Component
class SpanRunner(
    private val tracer: Tracer,
) {
    companion object {
        @Volatile
        lateinit var inst: SpanRunner
            private set

        /**
         * 새 span을 생성해 scope에 넣은 뒤 action을 실행한다.
         * action 완료 후 scope·span을 정리한다.
         */
        fun runWithSpan(
            spanName: String,
            action: () -> Unit,
        ) {
            inst.runWithSpan(spanName, action)
        }

        /**
         * 새 span을 생성해 scope에 넣은 뒤 action을 실행하고 결과를 반환한다.
         * action 완료 후 scope·span을 정리한다.
         */
        fun <T> supplyWithSpan(
            spanName: String,
            action: () -> T,
        ): T = inst.supplyWithSpan(spanName, action)
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    fun runWithSpan(
        spanName: String,
        action: () -> Unit,
    ) {
        val span = tracer.nextSpan().name(spanName).start()
        val scope = tracer.withSpan(span)
        try {
            action()
        } finally {
            scope.close()
            span.end()
        }
    }

    fun <T> supplyWithSpan(
        spanName: String,
        action: () -> T,
    ): T {
        val span = tracer.nextSpan().name(spanName).start()
        val scope = tracer.withSpan(span)
        try {
            return action()
        } finally {
            scope.close()
            span.end()
        }
    }
}
