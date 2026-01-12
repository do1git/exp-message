package site.rahoon.message.__monolitic.message.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.message.domain.Message
import site.rahoon.message.__monolitic.message.domain.MessageDomainService
import site.rahoon.message.__monolitic.message.domain.MessageError

/**
 * Message Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class MessageApplicationService(
    private val messageDomainService: MessageDomainService,
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService
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
    fun getByChatRoomId(criteria: MessageCriteria.GetByChatRoomId): List<Message> {
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

        return messageDomainService.getByChatRoomId(criteria.chatRoomId)
    }
}
