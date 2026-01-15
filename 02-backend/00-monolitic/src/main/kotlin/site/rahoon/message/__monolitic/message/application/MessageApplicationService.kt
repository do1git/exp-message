package site.rahoon.message.__monolitic.message.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.common.application.CommonPageCursor
import site.rahoon.message.__monolitic.common.application.CommonResult
import site.rahoon.message.__monolitic.common.global.Base62Encoding
import site.rahoon.message.__monolitic.common.global.toEpochMicroLong
import site.rahoon.message.__monolitic.common.global.toLocalDateTimeFromMicros
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.message.domain.Message
import site.rahoon.message.__monolitic.message.domain.MessageDomainService
import site.rahoon.message.__monolitic.message.domain.MessageError
import java.time.ZoneId
import java.util.UUID

/**
 * Message Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class MessageApplicationService(
    private val messageDomainService: MessageDomainService,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService,
    private val zoneId: ZoneId,
) {

    /**
     * 메시지 전송
     * 채팅방 멤버만 메시지를 전송할 수 있습니다.
     */
    fun create(criteria: MessageCriteria.Create): Message {
        // 채팅방 존재 여부 확인
        try {
            chatRoomDomainService.getById(criteria.chatRoomId)
        } catch (e: DomainException) {
            if (e.error == ChatRoomError.CHAT_ROOM_NOT_FOUND) {
                throw DomainException(
                    error = MessageError.CHAT_ROOM_NOT_FOUND,
                    details = mapOf("chatRoomId" to criteria.chatRoomId)
                )
            }
            throw e
        }

        // 채팅방 멤버인지 확인
        val isMember = chatRoomMemberApplicationService.isMember(
            chatRoomId = criteria.chatRoomId,
            userId = criteria.userId
        )

        if (!isMember) {
            throw DomainException(
                error = MessageError.UNAUTHORIZED_ACCESS,
                details = mapOf(
                    "chatRoomId" to criteria.chatRoomId,
                    "userId" to criteria.userId,
                    "reason" to "채팅방 멤버만 메시지를 전송할 수 있습니다"
                )
            )
        }

        val command = criteria.toCommand()
        return messageDomainService.create(command)
    }

    /**
     * 메시지 조회
     */
    fun getById(messageId: String): Message {
        return messageDomainService.getById(messageId)
    }

    /**
     * 채팅방별 메시지 목록 조회
     */
    fun getByChatRoomId(criteria: MessageCriteria.GetByChatRoomId): CommonResult.Page<Message> {
        // 채팅방 존재 여부 확인
        try {
            chatRoomDomainService.getById(criteria.chatRoomId)
        } catch (e: DomainException) {
            if (e.error == ChatRoomError.CHAT_ROOM_NOT_FOUND) {
                throw DomainException(
                    error = MessageError.CHAT_ROOM_NOT_FOUND,
                    details = mapOf("chatRoomId" to criteria.chatRoomId)
                )
            }
            throw e
        }

        val decoded = criteria.cursor
            ?.let { CommonPageCursor.decode(it) }
            ?.requireVersion("1")
            ?.requireKeysInOrder(listOf("ca", "i"))

        val afterCreatedAt = decoded?.let {
            val encodedCreatedAt = it.getAsString("ca")
            val createdAtMicros = Base62Encoding.decodeLong(encodedCreatedAt)
            createdAtMicros.toLocalDateTimeFromMicros(zoneId)
        }
        val afterId = decoded?.let {
            val encodedId = it.getAsString("i")
            val uuid = Base62Encoding.decodeUuid(encodedId)
            uuid.toString()
        }

        val fetchSize = criteria.limit + 1
        val fetched = messageDomainService.getByChatRoomId(
            chatRoomId = criteria.chatRoomId,
            afterCreatedAt = afterCreatedAt,
            afterId = afterId,
            limit = fetchSize
        )

        val pageItems = fetched.take(criteria.limit)
        val hasNext = fetched.size > criteria.limit
        val nextCursor = if (hasNext && pageItems.isNotEmpty()) {
            val last = pageItems.last()
            val createdAtMicros = last.createdAt.toEpochMicroLong(zoneId)
            CommonPageCursor.encode(
                version = "1",
                // 순서 주의! createdAt -> id
                cursors = listOf(
                    "ca" to Base62Encoding.encodeLong(createdAtMicros),
                    "i" to Base62Encoding.encodeUuid(UUID.fromString(last.id))
                )
            )
        } else {
            null
        }

        return CommonResult.Page(
            items = pageItems,
            nextCursor = nextCursor,
            limit = criteria.limit
        )
    }
}
