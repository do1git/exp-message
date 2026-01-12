package site.rahoon.message.__monolitic.chatroom.application

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMemberInfo
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.time.LocalDateTime
import java.util.UUID

/**
 * ChatRoomApplicationService 단위 테스트
 * 의존성을 Mock으로 처리하여 Application Service 로직만 검증합니다.
 */
class ChatRoomApplicationServiceTest {

    private lateinit var chatRoomDomainService: ChatRoomDomainService
    private lateinit var chatRoomMemberApplicationService: ChatRoomMemberApplicationService
    private lateinit var chatRoomApplicationService: ChatRoomApplicationService

    @BeforeEach
    fun setUp() {
        chatRoomDomainService = mock()
        chatRoomMemberApplicationService = mock()
        chatRoomApplicationService = ChatRoomApplicationService(
            chatRoomDomainService,
            chatRoomMemberApplicationService
        )
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 단일 채팅방`() {
        // given
        val userId = "user123"
        val chatRoomId = UUID.randomUUID().toString()
        
        val memberInfo = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        
        val chatRoomInfo = ChatRoomInfo.Detail(
            id = chatRoomId,
            name = "테스트 채팅방",
            createdByUserId = "creator123",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(chatRoomMemberApplicationService.getByUserId(userId))
            .thenReturn(listOf(memberInfo))
        whenever(chatRoomDomainService.getByIds(listOf(chatRoomId)))
            .thenReturn(listOf(chatRoomInfo))

        // when
        val result = chatRoomApplicationService.getByMemberUserId(userId)

        // then
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals(chatRoomId, result[0].id)
        assertEquals("테스트 채팅방", result[0].name)
        
        verify(chatRoomMemberApplicationService).getByUserId(userId)
        verify(chatRoomDomainService).getByIds(listOf(chatRoomId))
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 여러 채팅방`() {
        // given
        val userId = "user123"
        val chatRoomId1 = UUID.randomUUID().toString()
        val chatRoomId2 = UUID.randomUUID().toString()
        val chatRoomId3 = UUID.randomUUID().toString()
        
        val memberInfo1 = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId1,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        val memberInfo2 = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId2,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        val memberInfo3 = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId3,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        
        val chatRoomInfo1 = ChatRoomInfo.Detail(
            id = chatRoomId1,
            name = "채팅방 1",
            createdByUserId = "creator1",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val chatRoomInfo2 = ChatRoomInfo.Detail(
            id = chatRoomId2,
            name = "채팅방 2",
            createdByUserId = "creator2",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val chatRoomInfo3 = ChatRoomInfo.Detail(
            id = chatRoomId3,
            name = "채팅방 3",
            createdByUserId = "creator3",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        whenever(chatRoomMemberApplicationService.getByUserId(userId))
            .thenReturn(listOf(memberInfo1, memberInfo2, memberInfo3))
        whenever(chatRoomDomainService.getByIds(listOf(chatRoomId1, chatRoomId2, chatRoomId3)))
            .thenReturn(listOf(chatRoomInfo1, chatRoomInfo2, chatRoomInfo3))

        // when
        val result = chatRoomApplicationService.getByMemberUserId(userId)

        // then
        assertNotNull(result)
        assertEquals(3, result.size)
        assertEquals(chatRoomId1, result[0].id)
        assertEquals(chatRoomId2, result[1].id)
        assertEquals(chatRoomId3, result[2].id)
        assertEquals("채팅방 1", result[0].name)
        assertEquals("채팅방 2", result[1].name)
        assertEquals("채팅방 3", result[2].name)
        
        verify(chatRoomMemberApplicationService).getByUserId(userId)
        verify(chatRoomDomainService).getByIds(listOf(chatRoomId1, chatRoomId2, chatRoomId3))
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 참여한 채팅방이 없을 때`() {
        // given
        val userId = "user123"

        whenever(chatRoomMemberApplicationService.getByUserId(userId))
            .thenReturn(emptyList())

        // when
        val result = chatRoomApplicationService.getByMemberUserId(userId)

        // then
        assertNotNull(result)
        assertTrue(result.isEmpty())
        
        verify(chatRoomMemberApplicationService).getByUserId(userId)
        // 빈 리스트 조기 반환으로 getByIds는 호출되지 않아야 함
        verify(chatRoomDomainService, org.mockito.kotlin.never()).getByIds(any())
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 - 채팅방 ID 순서 확인`() {
        // given
        val userId = "user123"
        val chatRoomId1 = UUID.randomUUID().toString()
        val chatRoomId2 = UUID.randomUUID().toString()
        
        val memberInfo1 = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId1,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        val memberInfo2 = ChatRoomMemberInfo.Detail(
            id = UUID.randomUUID().toString(),
            chatRoomId = chatRoomId2,
            userId = userId,
            joinedAt = LocalDateTime.now()
        )
        
        val chatRoomInfo1 = ChatRoomInfo.Detail(
            id = chatRoomId1,
            name = "채팅방 1",
            createdByUserId = "creator1",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        val chatRoomInfo2 = ChatRoomInfo.Detail(
            id = chatRoomId2,
            name = "채팅방 2",
            createdByUserId = "creator2",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        // memberInfo 순서대로 chatRoomId가 추출되어야 함
        whenever(chatRoomMemberApplicationService.getByUserId(userId))
            .thenReturn(listOf(memberInfo1, memberInfo2))
        // getByIds는 추출된 ID 순서대로 호출되어야 함
        whenever(chatRoomDomainService.getByIds(listOf(chatRoomId1, chatRoomId2)))
            .thenReturn(listOf(chatRoomInfo1, chatRoomInfo2))

        // when
        val result = chatRoomApplicationService.getByMemberUserId(userId)

        // then
        assertEquals(2, result.size)
        // getByIds가 올바른 순서로 호출되었는지 확인
        verify(chatRoomDomainService).getByIds(listOf(chatRoomId1, chatRoomId2))
    }
}
