package site.rahoon.message.monolithic.message.application

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomError
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.monolithic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.message.domain.Message
import site.rahoon.message.monolithic.message.domain.MessageDomainService
import site.rahoon.message.monolithic.message.domain.MessageError
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * MessageApplicationService 단위 테스트
 * 의존성을 Mock으로 처리하여 Application Service 로직만 검증합니다.
 */
class MessageApplicationServiceUT {
    private lateinit var messageDomainService: MessageDomainService
    private lateinit var chatRoomDomainService: ChatRoomDomainService
    private lateinit var chatRoomMemberApplicationService: ChatRoomMemberApplicationService
    private lateinit var messageEventPublisher: MessageEventPublisher
    private lateinit var messageApplicationService: MessageApplicationService

    @BeforeEach
    fun setUp() {
        messageDomainService = mockk()
        chatRoomDomainService = mockk()
        chatRoomMemberApplicationService = mockk()
        messageEventPublisher = mockk<MessageEventPublisher>(relaxed = true)
        messageApplicationService =
            MessageApplicationService(
                messageDomainService,
                chatRoomDomainService,
                chatRoomMemberApplicationService,
                messageEventPublisher,
                ZoneId.systemDefault(),
            )
    }

    @Test
    fun `메시지 전송 성공`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val content = "안녕하세요!"

        val chatRoomInfo =
            ChatRoomInfo.Detail(
                id = chatRoomId,
                name = "테스트 채팅방",
                createdByUserId = UUID.randomUUID().toString(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val message =
            Message(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = userId,
                content = content,
                createdAt = LocalDateTime.now(),
            )

        val criteria =
            MessageCriteria.Create(
                chatRoomId = chatRoomId,
                userId = userId,
                content = content,
            )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every { chatRoomMemberApplicationService.isMember(chatRoomId, userId) } returns true
        every { messageDomainService.create(any()) } returns message

        // when
        val result = messageApplicationService.create(criteria)

        // then
        result.shouldNotBeNull()
        result.chatRoomId shouldBe chatRoomId
        result.chatRoomId shouldBe chatRoomId
        result.userId shouldBe userId
        result.content shouldBe content

        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { chatRoomMemberApplicationService.isMember(chatRoomId, userId) }
        verify { messageDomainService.create(any()) }
        verify {
            messageEventPublisher.publishCreated(
                match<MessageEvent.Created> {
                    it.id == message.id && it.chatRoomId == chatRoomId
                },
            )
        }
    }

    @Test
    fun `메시지 전송 실패 - 채팅방이 존재하지 않음`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val content = "안녕하세요!"

        val criteria =
            MessageCriteria.Create(
                chatRoomId = chatRoomId,
                userId = userId,
                content = content,
            )

        every { chatRoomDomainService.getById(chatRoomId) } throws
            DomainException(
                error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                details = mapOf("chatRoomId" to chatRoomId),
            )

        // when & then
        val exception =
            assertThrows<DomainException> {
                messageApplicationService.create(criteria)
            }

        exception.error shouldBe MessageError.CHAT_ROOM_NOT_FOUND
        verify { chatRoomDomainService.getById(chatRoomId) }
        verify(exactly = 0) { chatRoomMemberApplicationService.isMember(any(), any()) }
        verify(exactly = 0) { messageDomainService.create(any()) }
    }

    @Test
    fun `메시지 전송 실패 - 채팅방 멤버가 아님`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val content = "안녕하세요!"

        val chatRoomInfo =
            ChatRoomInfo.Detail(
                id = chatRoomId,
                name = "테스트 채팅방",
                createdByUserId = UUID.randomUUID().toString(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val criteria =
            MessageCriteria.Create(
                chatRoomId = chatRoomId,
                userId = userId,
                content = content,
            )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every { chatRoomMemberApplicationService.isMember(chatRoomId, userId) } returns false

        // when & then
        val exception =
            assertThrows<DomainException> {
                messageApplicationService.create(criteria)
            }

        exception.error shouldBe MessageError.UNAUTHORIZED_ACCESS
        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { chatRoomMemberApplicationService.isMember(chatRoomId, userId) }
        verify(exactly = 0) { messageDomainService.create(any()) }
    }

    @Test
    fun `메시지 조회 성공`() {
        // given
        val messageId = UUID.randomUUID().toString()
        val message =
            Message(
                id = messageId,
                chatRoomId = UUID.randomUUID().toString(),
                userId = UUID.randomUUID().toString(),
                content = "테스트 메시지",
                createdAt = LocalDateTime.now(),
            )

        every { messageDomainService.getById(messageId) } returns message

        // when
        val result = messageApplicationService.getById(messageId)

        // then
        result.shouldNotBeNull()
        result.id shouldBe messageId
        result.content shouldBe "테스트 메시지"
        verify { messageDomainService.getById(messageId) }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 성공`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()

        val chatRoomInfo =
            ChatRoomInfo.Detail(
                id = chatRoomId,
                name = "테스트 채팅방",
                createdByUserId = UUID.randomUUID().toString(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val message1 =
            Message(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = UUID.randomUUID().toString(),
                content = "메시지 1",
                createdAt = LocalDateTime.now(),
            )
        val message2 =
            Message(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = UUID.randomUUID().toString(),
                content = "메시지 2",
                createdAt = LocalDateTime.now(),
            )

        val criteria =
            MessageCriteria.GetByChatRoomId(
                chatRoomId = chatRoomId,
                cursor = null,
                limit = 20,
            )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every {
            messageDomainService.getByChatRoomId(chatRoomId, null, null, 21)
        } returns listOf(message1, message2)

        // when
        val result = messageApplicationService.getByChatRoomId(criteria)

        // then
        result.shouldNotBeNull()
        result.items.size shouldBe 2
        result.items[0].content shouldBe "메시지 1"
        result.items[1].content shouldBe "메시지 2"
        result.nextCursor shouldBe null
        result.limit shouldBe 20

        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { messageDomainService.getByChatRoomId(chatRoomId, null, null, 21) }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 채팅방이 존재하지 않음`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()

        val criteria =
            MessageCriteria.GetByChatRoomId(
                chatRoomId = chatRoomId,
                cursor = null,
                limit = 20,
            )

        every { chatRoomDomainService.getById(chatRoomId) } throws
            DomainException(
                error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                details = mapOf("chatRoomId" to chatRoomId),
            )

        // when & then
        val exception =
            assertThrows<DomainException> {
                messageApplicationService.getByChatRoomId(criteria)
            }

        exception.error shouldBe MessageError.CHAT_ROOM_NOT_FOUND
        verify { chatRoomDomainService.getById(chatRoomId) }
        verify(exactly = 0) { messageDomainService.getByChatRoomId(any(), any(), any(), any()) }
    }
}
