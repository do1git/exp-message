package site.rahoon.message.monolithic.common.websocket

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.expression.ExpressionException
import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.messaging.MessagingException
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

/**
 * @WebsocketSend 어노테이션이 달린 메서드의 반환값을 자동으로 WebSocket 토픽으로 브로드캐스트하는 Aspect
 */
@Aspect
@Component
class WebsocketSendAspect(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    private val logger = KotlinLogging.logger {}
    private val spelParser: ExpressionParser = SpelExpressionParser()

    @Around("@annotation(websocketSend)")
    fun broadcastReturnValue(
        joinPoint: ProceedingJoinPoint,
        websocketSend: WebsocketSend,
    ): Any? {
        logger.info { "@WebsocketSend Aspect 실행: method=${joinPoint.signature.name}, topic=${websocketSend.value}" }

        // 메서드 실행
        val returnValue = joinPoint.proceed()

        // 반환값이 null이 아니면 브로드캐스트
        if (returnValue != null) {
            try {
                val topic = resolveTopic(websocketSend.value, returnValue)
                logger.info { "WebSocket 브로드캐스트 시도: topic=$topic, returnValue=$returnValue" }
                messagingTemplate.convertAndSend(topic, returnValue)
                logger.info { "WebSocket 브로드캐스트 완료: topic=$topic" }
            } catch (e: MessagingException) {
                logger.error(e) { "WebSocket 브로드캐스트 실패: topic=${websocketSend.value}, error=${e.message}" }
            } catch (e: IllegalStateException) {
                logger.error(e) { "WebSocket 브로드캐스트 실패: topic=${websocketSend.value}, error=${e.message}" }
            }
        } else {
            logger.info { "반환값이 null이므로 브로드캐스트하지 않음" }
        }

        return returnValue
    }

    /**
     * SPEL(Spring Expression Language)을 사용하여 토픽 경로의 변수를 반환값 객체의 프로퍼티 값으로 치환합니다.
     * 예: "/topic/chat-rooms/{chatRoomId}/messages" -> "/topic/chat-rooms/room-123/messages"
     *
     * SPEL 표현식 예시:
     * - {chatRoomId} : 반환값의 chatRoomId 프로퍼티
     * - {id} : 반환값의 id 프로퍼티
     * - {user.id} : 반환값의 user.id (중첩 프로퍼티)
     * - {userId ?: 'default'} : userId가 null이면 'default' 사용
     */
    private fun resolveTopic(
        topicTemplate: String,
        returnValue: Any,
    ): String {
        var resolvedTopic = topicTemplate

        // {표현식} 패턴 찾기
        val variablePattern = Regex("\\{([^}]+)\\}")
        val matches = variablePattern.findAll(topicTemplate)

        // SPEL 컨텍스트 생성 및 반환값 등록
        val context = StandardEvaluationContext(returnValue)
        // 반환값을 'result' 또는 직접 접근 가능하도록 설정
        context.setVariable("result", returnValue)
        context.setVariable("returnValue", returnValue)

        matches.forEach { match ->
            val spelExpression = match.groupValues[1] // 중괄호 안의 표현식
            try {
                val expression = spelParser.parseExpression(spelExpression)
                val value = expression.getValue(context)

                if (value != null) {
                    resolvedTopic = resolvedTopic.replace("{$spelExpression}", value.toString())
                    logger.debug { "SPEL 평가 성공: expression=$spelExpression, value=$value" }
                } else {
                    logger.warn { "SPEL 평가 결과가 null입니다: expression=$spelExpression" }
                }
            } catch (e: ExpressionException) {
                logger.error(e) { "SPEL 평가 실패: expression=$spelExpression, error=${e.message}" }
            }
        }

        return resolvedTopic
    }
}
