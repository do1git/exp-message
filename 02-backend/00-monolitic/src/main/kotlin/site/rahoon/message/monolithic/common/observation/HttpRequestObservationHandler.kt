package site.rahoon.message.monolithic.common.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.global.nanoToMs
import java.time.OffsetDateTime

/**
 * HTTP 요청 Observation(http.server.requests)의 method, path, status, duration을 MDC에 주입하여
 * 구조화 로깅 시 JSON 필드로 포함되도록 한다.
 *
 * Spring Boot가 자동 구성하는 ServerHttpObservationFilter의 Observation에 연결되어
 * traceId/spanId와 함께 로그에 출력된다.
 */
@Component
class HttpRequestObservationHandler : ObservationHandler<ServerRequestObservationContext> {
    companion object {
        private val log = LoggerFactory.getLogger(HttpRequestObservationHandler::class.java)
        private val startTime = ThreadLocal.withInitial { 0L }
    }

    override fun supportsContext(context: Observation.Context): Boolean = context is ServerRequestObservationContext

    override fun onStart(context: ServerRequestObservationContext) {
        startTime.set(System.nanoTime())
    }

    override fun onScopeOpened(context: ServerRequestObservationContext) {
        val request = context.carrier
        MDC.put(MdcKeys.HTTP_METHOD, request.method)
        MDC.put(MdcKeys.HTTP_PATH, request.requestURI)
        MDC.put(MdcKeys.HTTP_START_TIME, OffsetDateTime.now().toString())
        MDC.put(MdcKeys.CLIENT_IP, resolveClientIp(request))
        request.getHeader("User-Agent")?.takeIf { it.isNotBlank() }?.let { MDC.put(MdcKeys.USER_AGENT, it) }
    }

    private fun resolveClientIp(request: jakarta.servlet.http.HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) {
            forwardedFor.split(",").firstOrNull()?.trim() ?: request.remoteAddr
        } else {
            request.remoteAddr ?: "-"
        }
    }

    override fun onScopeClosed(context: ServerRequestObservationContext) {
        val durationMs = (System.nanoTime() - startTime.get()).nanoToMs()
        val response = context.response

        MDC.put(MdcKeys.HTTP_END_TIME, OffsetDateTime.now().toString())
        if (response != null) {
            MDC.put(MdcKeys.HTTP_STATUS, response.status.toString())
        }
        MDC.put(MdcKeys.HTTP_DURATION_MS, durationMs.toString())

        log.info(
            "Request completed: {} {} - {} ({}ms) - Start: {}, End: {} - user: {}, session: {}",
            context.carrier.method,
            context.carrier.requestURI,
            response?.status ?: "?",
            durationMs,
            MDC.get(MdcKeys.HTTP_START_TIME),
            MDC.get(MdcKeys.HTTP_END_TIME),
            MDC.get(MdcKeys.USER_ID) ?: "-",
            MDC.get(MdcKeys.AUTH_SESSION_ID) ?: "-",
        )

        removeMdcKeys()
        startTime.remove()
    }

    private fun removeMdcKeys() {
        MDC.remove(MdcKeys.HTTP_METHOD)
        MDC.remove(MdcKeys.HTTP_PATH)
        MDC.remove(MdcKeys.HTTP_STATUS)
        MDC.remove(MdcKeys.HTTP_DURATION_MS)
        MDC.remove(MdcKeys.HTTP_START_TIME)
        MDC.remove(MdcKeys.HTTP_END_TIME)
        MDC.remove(MdcKeys.CLIENT_IP)
        MDC.remove(MdcKeys.USER_AGENT)
        MDC.remove(MdcKeys.USER_ID)
        MDC.remove(MdcKeys.AUTH_SESSION_ID)
    }
}
