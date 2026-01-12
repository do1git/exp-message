package site.rahoon.message.__monolitic.chatroom.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomCommand
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo

/**
 * ChatRoom Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class ChatRoomApplicationService(
    private val chatRoomDomainService: ChatRoomDomainService
) {

    /**
     * 채팅방 생성
     */
    fun create(criteria: ChatRoomCriteria.Create, createdByUserId: String): ChatRoomInfo.Detail {
        val command = criteria.toCommand(createdByUserId)
        // TODO ChatRoomUser에 본인 추가 필요.
        return chatRoomDomainService.create(command)
    }

    /**
     * 채팅방 수정
     */
    fun update(criteria: ChatRoomCriteria.Update, chatRoomId: String): ChatRoomInfo.Detail {
        val command = criteria.toCommand(chatRoomId)
        return chatRoomDomainService.update(command)
    }

    /**
     * 채팅방 삭제
     */
    fun delete(chatRoomId: String): ChatRoomInfo.Detail {
        val command = ChatRoomCommand.Delete(id = chatRoomId)
        return chatRoomDomainService.delete(command)
    }

    /**
     * 채팅방 조회
     */
    fun getById(chatRoomId: String): ChatRoomInfo.Detail {
        return chatRoomDomainService.getById(chatRoomId)
    }

    /**
     * 내가 생성한 채팅방 목록 조회
     */
    fun getByCreatedByUserId(userId: String): List<ChatRoomInfo.Detail> {
        return chatRoomDomainService.getByCreatedByUserId(userId)
    }
}
