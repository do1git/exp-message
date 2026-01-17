package site.rahoon.message.monolithic.chatroommember.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.chatroommember.domain.ChatRoomMemberDomainService
import site.rahoon.message.monolithic.chatroommember.domain.ChatRoomMemberInfo

/**
 * ChatRoomMember Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class ChatRoomMemberApplicationService(
    private val chatRoomMemberDomainService: ChatRoomMemberDomainService,
) {
    /**
     * 채팅방 참가
     */
    fun join(criteria: ChatRoomMemberCriteria.Join): ChatRoomMemberInfo.Detail {
        val command = criteria.toCommand()
        return chatRoomMemberDomainService.join(command)
    }

    /**
     * 채팅방 나가기
     */
    fun leave(criteria: ChatRoomMemberCriteria.Leave): ChatRoomMemberInfo.Detail {
        val command = criteria.toCommand()
        return chatRoomMemberDomainService.leave(command)
    }

    /**
     * 특정 채팅방의 멤버 목록 조회
     */
    fun getByChatRoomId(chatRoomId: String): List<ChatRoomMemberInfo.Detail> = chatRoomMemberDomainService.getByChatRoomId(chatRoomId)

    /**
     * 특정 사용자가 참가한 채팅방 목록 조회
     */
    fun getByUserId(userId: String): List<ChatRoomMemberInfo.Detail> = chatRoomMemberDomainService.getByUserId(userId)

    /**
     * 특정 사용자가 특정 채팅방의 멤버인지 확인합니다.
     */
    fun isMember(
        chatRoomId: String,
        userId: String,
    ): Boolean = chatRoomMemberDomainService.isMember(chatRoomId, userId)
}
