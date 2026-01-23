package site.rahoon.message.monolithic.message.infrastructure

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.application.MessageEventPublisher
import site.rahoon.message.monolithic.message.application.MessageEventSubscriber
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.CopyOnWriteArrayList

class MessageEventRedisRepositoryIT(
    private val messageEventPublisher: MessageEventPublisher,
    private val testMessageSubscriber: TestMessageSubscriber, // 테스트용으로 주입
) : IntegrationTestBase() {
    @BeforeEach
    fun setUp() {
        testMessageSubscriber.clear()
    }

    @Test
    fun `메시지 발행 시 패턴 구독자가 이벤트를 정상적으로 수신한다`() {
        // given
        val chatRoomId = "room-123"
        val event = MessageEvent.Created(
            id = "msg-999",
            chatRoomId = chatRoomId,
            userId = "user-1",
            content = "통합 테스트 메시지",
            createdAt = LocalDateTime.now(),
        )

        logger.info { "EventCreated: $event" }

        // when
        messageEventPublisher.publishCreated(event)
        logger.info { "EventPublished: $event" }

        // then: Redis를 거쳐 비동기로 돌아오므로 Awaitility 사용
        await.atMost(Duration.ofSeconds(3)) untilAsserted {
            val receivedEvents = testMessageSubscriber.getReceivedEvents()
            receivedEvents.size shouldBe 1
            with(receivedEvents[0]) {
                id shouldBe event.id
                chatRoomId shouldBe event.chatRoomId
                content shouldBe event.content
                // LocalDateTime 역직렬화 검증이 여기서 핵심!
                createdAt.truncatedTo(ChronoUnit.SECONDS) shouldBe event.createdAt.truncatedTo(ChronoUnit.SECONDS)
            }
        }
    }
}

/**
 * 테스트용 구독자 클래스
 * 실제 빈으로 등록되어 MessageEventRedisRepository의 List<MessageEventSubscriber>에 포함되어야 함
 */
@Component
class TestMessageSubscriber : MessageEventSubscriber {
    private val receivedEvents = CopyOnWriteArrayList<MessageEvent.Created>()
    private val logger = KotlinLogging.logger { }

    override fun onCreated(event: MessageEvent.Created) {
        logger.info { "EventReceived: $event" }
        receivedEvents.add(event)
    }

    fun getReceivedEvents() = receivedEvents

    fun clear() = receivedEvents.clear()
}
