package site.rahoon.message.monolithic.common.websocket.config.session

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.auth.WebSocketAuthBody
import site.rahoon.message.monolithic.common.websocket.auth.WebSocketAuthController
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBody
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBodyBuilder
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionController
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Heartbeat 주기(websocket.heartbeat-interval-ms)마다 등록된 세션의 TTL(만료)을 검사하고,
 * 만료된 세션에 대해 ERROR 프레임을 보낸 뒤 레지스트리에서 제거한다.
 * 만료 임박 구간의 세션에는 갱신 유도 MESSAGE를 전송한다.
 *
 * - SimpleBroker heartbeat와 동일 주기로 실행.
 * - [WebSocketSessionAuthInfoRegistry]에 등록된 (sessionId, authInfo)만 검사.
 * - ScheduledSpanAspect로 traceId/spanId 생성, @Async로 taskExecutor에서 실행(스케줄러 스레드 비점유)
 */
@Component
class WebSocketSessionExpiryHeartbeatTask(
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
    private val authController: WebSocketAuthController,
    @param:Value("\${websocket.imminent-threshold-seconds:120}") private val imminentThresholdSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${websocket.heartbeat-interval-ms:10000}")
    fun checkExpiredSessions() {
        doCheckExpiredSessions()
    }

    private fun doCheckExpiredSessions() {
        val now = LocalDateTime.now()
        val imminentThreshold = now.plusSeconds(imminentThresholdSeconds)
        val snapshot = sessionAuthInfoRegistry.snapshot()
        for ((sessionId, authInfo) in snapshot) {
            when {
                authInfo.expiresAt.isBefore(now) -> {
                    log.warn(
                        "세션 만료(heartbeat 검사)로 연결 종료: userId={}, sessionId={}, wsSessionId={}",
                        authInfo.userId,
                        authInfo.sessionId,
                        sessionId,
                    )
                    val body = buildExpiredBody(sessionId, authInfo)
                    try {
                        exceptionController.sendErrorFrame(body)
                    } catch (e: Exception) {
                        log.debug("ERROR 프레임 전송 실패(연결 이미 끊김 등): sessionId={}", sessionId, e)
                    }
                    sessionAuthInfoRegistry.unregister(sessionId)
                }
                isExpiringSoon(authInfo.expiresAt, now, imminentThreshold) -> {
                    sendTokenExpiringSoon(sessionId, authInfo)
                }
            }
        }
    }

    private fun isExpiringSoon(
        expiresAt: LocalDateTime,
        now: LocalDateTime,
        imminentThreshold: LocalDateTime,
    ): Boolean = !expiresAt.isBefore(now) && !expiresAt.isAfter(imminentThreshold)

    private fun sendTokenExpiringSoon(
        websocketSessionId: String,
        authInfo: CommonAuthInfo,
    ) {
        val expiresAtIso = authInfo.expiresAt.atZone(ZoneId.systemDefault()).toString()
        val body = WebSocketAuthBody(
            event = WebSocketAuthBody.EVENT_TOKEN_EXPIRING_SOON,
            expiresAt = expiresAtIso,
            websocketSessionId = websocketSessionId,
            occurredAt = ZonedDateTime.now(),
        )
        try {
            authController.sendToAuthQueue(body)
        } catch (e: Exception) {
            log.debug("갱신 유도 MESSAGE 전송 실패(연결 끊김 등): sessionId={}", websocketSessionId, e)
        }
    }

    private fun buildExpiredBody(
        websocketSessionId: String,
        authInfo: CommonAuthInfo,
    ): WebSocketExceptionBody {
        val domainException = DomainException(
            CommonError.UNAUTHORIZED,
            mapOf(
                "reason" to "Session expired",
                "expiresAt" to authInfo.expiresAt.toString(),
            ),
        )
        return exceptionBodyBuilder.fromDomainException(
            domainException,
            websocketSessionId = websocketSessionId,
            receiptId = null,
            requestDestination = null,
        )
    }
}
