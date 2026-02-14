package site.rahoon.message.monolithic.common.global.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.global.SpanRunner

/**
 * @Scheduled 메서드 실행 시 SpanRunner로 감싸 traceId/spanId를 MDC에 설정한다.
 *
 * ThreadPoolTaskScheduler.scheduleAtFixedRate(Runnable) 등으로 직접 스케줄링한 작업은
 * @Scheduled가 아니므로 이 Aspect의 대상이 아니다. 해당 경우 SpanRunner.runWithSpan을 수동 적용한다.
 */
@Aspect
@Component
class ScheduledSpanAspect {
    @Around("@annotation(scheduled)")
    fun wrapWithSpan(
        joinPoint: ProceedingJoinPoint,
        @Suppress("UnusedParameter") scheduled: Scheduled,
    ): Any? {
        val spanName = "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
        return SpanRunner.supplyWithSpan(spanName) {
            joinPoint.proceed()
        }
    }
}
