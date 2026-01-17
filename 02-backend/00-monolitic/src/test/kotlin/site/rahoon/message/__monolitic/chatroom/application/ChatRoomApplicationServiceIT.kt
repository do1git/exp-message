package site.rahoon.message.__monolitic.chatroom.application

import io.kotest.matchers.ints.shouldBeExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import site.rahoon.message.__monolitic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomCommand
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMemberInfo
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

/**
 * ChatRoomApplicationService 단위 테스트
 * 의존성을 Mock으로 처리하여 Application Service 로직만 검증합니다.
 */
class ChatRoomApplicationServiceIT(
    private val chatRoomApplicationService: ChatRoomApplicationService,
    private val chatRoomDomainService: ChatRoomDomainService,
) : IntegrationTestBase() {

    @Test
    fun `삭제된 채팅방은 리스트 조회시 조회되지 않아야한다`() {
        // given
        val userUuid = UUID.randomUUID().toString()
        val chatRoom = chatRoomApplicationService.create(ChatRoomCriteria.Create(name = "삭제될 채팅방", createdByUserId = userUuid,))
        chatRoomApplicationService.delete(ChatRoomCriteria.Delete(chatRoomId = chatRoom.id, userId = userUuid))

        // When
        val list = chatRoomDomainService.getByCreatedByUserId(userUuid)

        // Then
        list.size shouldBeExactly 0


    }

}
