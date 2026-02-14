package site.rahoon.message.monolithic.message.infrastructure

import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.global.AsyncRunner
import site.rahoon.message.monolithic.common.global.SpanRunner
import site.rahoon.message.monolithic.message.application.MessageCommandEvent
import site.rahoon.message.monolithic.message.application.MessageCommandEventRelayPort
import java.util.concurrent.ConcurrentHashMap

/**
 * Redisson 기반 메시지 명령 이벤트 릴레이 구현체 (Adapter)
 *
 * MessageCommandEventRelayPort 인터페이스의 Redisson 구현
 * 사용자별 토픽으로 이벤트를 발행하고 구독합니다.
 */
@Component
class MessageCommandEventRelayRedisRepository(
    private val redissonClient: RedissonClient,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : MessageCommandEventRelayPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val listenerIdMap = ConcurrentHashMap<String, Int>()

    companion object {
        const val TOPIC_PREFIX = "users:"
        const val TOPIC_SUFFIX = ":messages"
    }

    override fun sendToUsers(sends: List<MessageCommandEvent.Send>) {
        sends.forEach { send ->
            val topic = "$TOPIC_PREFIX${send.recipientUserId}$TOPIC_SUFFIX"
            log.debug("Publishing message command event to Redis: topic=$topic, messageId=${send.id}")
            redissonClient.getTopic(topic).publish(send)
        }
    }

    override fun subscribe(userId: String) {
        if (listenerIdMap.containsKey(userId)) {
            log.debug("Already subscribed to user: $userId")
            return
        }

        val topic = redissonClient.getTopic("$TOPIC_PREFIX$userId$TOPIC_SUFFIX")
        // userId를 로컬 변수로 캡처하여 클로저 문제 방지
        val capturedUserId = userId
        val listenerId = topic.addListener(MessageCommandEvent.Send::class.java) { channel, event ->
            AsyncRunner.runAsync {
                SpanRunner.runWithSpan("redis-topic-listener") {
                    // 구독 해제된 경우 리스너 제거
                    if (!listenerIdMap.containsKey(capturedUserId)) {
                        val currentTopic = redissonClient.getTopic(channel.toString())
                        listenerIdMap[capturedUserId]?.let { currentTopic.removeListener(it) }
                        return@runWithSpan
                    }

                    log.debug("MessageCommandEvent.Send 수신됨 - Channel: $channel, Message.Id: ${event.id}, UserId: $capturedUserId")
                    applicationEventPublisher.publishEvent(event)
                }
            }
        }

        listenerIdMap[userId] = listenerId
        log.info("Subscribed to Redis topic: $TOPIC_PREFIX$userId$TOPIC_SUFFIX")
    }

    override fun unsubscribe(userId: String) {
        // 세션 종료 시 리스너 해제 (중요!)
        listenerIdMap.remove(userId)?.let { listenerId ->
            val topic = redissonClient.getTopic("$TOPIC_PREFIX$userId$TOPIC_SUFFIX")
            topic.removeListener(listenerId)
            log.info("Unsubscribed from Redis topic: $TOPIC_PREFIX$userId$TOPIC_SUFFIX")
        }
    }
}
