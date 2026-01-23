package site.rahoon.message.monolithic.message.infrastructure

import jakarta.annotation.PostConstruct
import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.global.AsyncRunner
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.application.MessageEventPublisher
import site.rahoon.message.monolithic.message.application.MessageEventSubscriber

/**
 * Redisson 기반 메시지 이벤트 발행 구현체 (Adapter)
 *
 * MessageEventPublisher 인터페이스의 Redisson 구현
 */
@Component
class MessageEventRedisRepository(
    private val redissonClient: RedissonClient,
    private val messageEventSubscribers: List<MessageEventSubscriber>,
) : MessageEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val CHANNEL_PREFIX = "message:chat-rooms:"
    }

    override fun publishCreated(event: MessageEvent.Created) {
        val channel = "$CHANNEL_PREFIX${event.chatRoomId}"
        log.debug("Publishing message event to Redis: channel=$channel, messageId=${event.id}")
        redissonClient.getTopic(channel).publish(event)
    }

    @PostConstruct
    fun subscribe() {
        val topicPattern = "$CHANNEL_PREFIX*"
        // Redisson의 PatternTopic 활용
        val topic = redissonClient.getPatternTopic(topicPattern)

        // 세 번째 인자인 'pattern'을 추가하거나 '_'로 무시해야 합니다.
        topic.addListener(MessageEvent.Created::class.java) { pattern, channel, event ->
            log.debug("MessageEvent.Created 수신됨 - Channel: $channel, Message.Id: ${event.id}, 구독자 수: ${messageEventSubscribers.size}")
            messageEventSubscribers.map { AsyncRunner.runAsync { it.onCreated(event) } }
            log.debug("MessageEvent.Created 처리요청완료 - Channel: $channel, Message.Id: ${event.id}")
        }

        log.info("Subscribed to Redis pattern: $topicPattern (Total subscribers: ${messageEventSubscribers.size})")
    }
}
