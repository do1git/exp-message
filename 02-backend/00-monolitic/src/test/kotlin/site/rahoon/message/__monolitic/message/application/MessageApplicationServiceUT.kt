package site.rahoon.message.__monolitic.message.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.message.domain.Message
import site.rahoon.message.__monolitic.message.domain.MessageDomainService
import site.rahoon.message.__monolitic.message.domain.MessageError
import java.time.Clock
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
    private lateinit var messageApplicationService: MessageApplicationService

    @BeforeEach
    fun setUp() {
        messageDomainService = mockk()
        chatRoomDomainService = mockk()
        chatRoomMemberApplicationService = mockk()
        messageApplicationService = MessageApplicationService(
            messageDomainService,
            chatRoomDomainService,
            chatRoomMemberApplicationService,
            ZoneId.systemDefault()
        )
    }

    @Test
    fun `메시지 전송 성공`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val content = "안녕하세요!"

        val chatRoomInfo = ChatRoomInfo.Detail(
            id = chatRoomId,
            name = "테스트 채팅방",
            createdByUserId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val message = Message(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId,
            userId = userId,
            content = content,
            createdAt = LocalDateTime.now()
        )

        val criteria = MessageCriteria.Create(
            chatRoomId = chatRoomId,
            userId = userId,
            content = content
        )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every { chatRoomMemberApplicationService.isMember(chatRoomId, userId) } returns true
        every { messageDomainService.create(any()) } returns message

        // when
        val result = messageApplicationService.create(criteria)

        // then
        assertNotNull(result)
        assertEquals(chatRoomId, result.chatRoomId)
        assertEquals(userId, result.userId)
        assertEquals(content, result.content)

        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { chatRoomMemberApplicationService.isMember(chatRoomId, userId) }
        verify { messageDomainService.create(any()) }
    }

    @Test
    fun `메시지 전송 실패 - 채팅방이 존재하지 않음`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val content = "안녕하세요!"

        val criteria = MessageCriteria.Create(
            chatRoomId = chatRoomId,
            userId = userId,
            content = content
        )

        every { chatRoomDomainService.getById(chatRoomId) } throws DomainException(
            error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
            details = mapOf("chatRoomId" to chatRoomId)
        )

        // when & then
        val exception = assertThrows<DomainException> {
            messageApplicationService.create(criteria)
        }

        assertEquals(MessageError.CHAT_ROOM_NOT_FOUND, exception.error)
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

        val chatRoomInfo = ChatRoomInfo.Detail(
            id = chatRoomId,
            name = "테스트 채팅방",
            createdByUserId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val criteria = MessageCriteria.Create(
            chatRoomId = chatRoomId,
            userId = userId,
            content = content
        )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every { chatRoomMemberApplicationService.isMember(chatRoomId, userId) } returns false

        // when & then
        val exception = assertThrows<DomainException> {
            messageApplicationService.create(criteria)
        }

        assertEquals(MessageError.UNAUTHORIZED_ACCESS, exception.error)
        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { chatRoomMemberApplicationService.isMember(chatRoomId, userId) }
        verify(exactly = 0) { messageDomainService.create(any()) }
    }

    @Test
    fun `메시지 조회 성공`() {
        // given
        val messageId = UUID.randomUUID().toString()
        val message = Message(
            id = messageId,
            chatRoomId = UUID.randomUUID().toString(),
            userId = UUID.randomUUID().toString(),
            content = "테스트 메시지",
            createdAt = LocalDateTime.now()
        )

        every { messageDomainService.getById(messageId) } returns message

        // when
        val result = messageApplicationService.getById(messageId)

        // then
        assertNotNull(result)
        assertEquals(messageId, result.id)
        assertEquals("테스트 메시지", result.content)
        verify { messageDomainService.getById(messageId) }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 성공`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()

        val chatRoomInfo = ChatRoomInfo.Detail(
            id = chatRoomId,
            name = "테스트 채팅방",
            createdByUserId = UUID.randomUUID().toString(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val message1 = Message(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId,
            userId = UUID.randomUUID().toString(),
            content = "메시지 1",
            createdAt = LocalDateTime.now()
        )
        val message2 = Message(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId,
            userId = UUID.randomUUID().toString(),
            content = "메시지 2",
            createdAt = LocalDateTime.now()
        )

        val criteria = MessageCriteria.GetByChatRoomId(
            chatRoomId = chatRoomId,
            cursor = null,
            limit = 20
        )

        every { chatRoomDomainService.getById(chatRoomId) } returns chatRoomInfo
        every {
            messageDomainService.getByChatRoomId(chatRoomId, null, null, 21)
        } returns listOf(message1, message2)

        // when
        val result = messageApplicationService.getByChatRoomId(criteria)

        // then
        assertNotNull(result)
        assertEquals(2, result.items.size)
        assertEquals("메시지 1", result.items[0].content)
        assertEquals("메시지 2", result.items[1].content)
        assertNull(result.nextCursor)
        assertEquals(20, result.limit)

        verify { chatRoomDomainService.getById(chatRoomId) }
        verify { messageDomainService.getByChatRoomId(chatRoomId, null, null, 21) }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 채팅방이 존재하지 않음`() {
        // given
        val chatRoomId = UUID.randomUUID().toString()

        val criteria = MessageCriteria.GetByChatRoomId(
            chatRoomId = chatRoomId,
            cursor = null,
            limit = 20
        )

        every { chatRoomDomainService.getById(chatRoomId) } throws DomainException(
            error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
            details = mapOf("chatRoomId" to chatRoomId)
        )

        // when & then
        val exception = assertThrows<DomainException> {
            messageApplicationService.getByChatRoomId(criteria)
        }

        assertEquals(MessageError.CHAT_ROOM_NOT_FOUND, exception.error)
        verify { chatRoomDomainService.getById(chatRoomId) }
        verify(exactly = 0) { messageDomainService.getByChatRoomId(any(), any(), any(), any()) }
    }
}
