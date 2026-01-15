package site.rahoon.message.__monolitic.chatroom.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberCriteria
import site.rahoon.message.__monolitic.common.domain.DomainException

/**
 * ChatRoom Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class ChatRoomApplicationService(
    private val chatRoomDomainService: ChatRoomDomainService,
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService
) {

    /**
     * 채팅방 생성
     */
    fun create(criteria: ChatRoomCriteria.Create): ChatRoomInfo.Detail {
        val command = criteria.toCommand()
        val chatRoomInfo = chatRoomDomainService.create(command)
        
        // 생성자를 자동으로 멤버로 추가
        val joinCriteria = ChatRoomMemberCriteria.Join(
            chatRoomId = chatRoomInfo.id,
            userId = criteria.createdByUserId
        )
        chatRoomMemberApplicationService.join(joinCriteria)
        
        return chatRoomInfo
    }

    /**
     * 채팅방 수정
     */
    fun update(criteria: ChatRoomCriteria.Update): ChatRoomInfo.Detail {
        // 권한 검증: 생성자만 수정 가능
        val chatRoomInfo = chatRoomDomainService.getById(criteria.chatRoomId)
        if (chatRoomInfo.createdByUserId != criteria.userId) {
            throw DomainException(
                error = ChatRoomError.UNAUTHORIZED_ACCESS,
                details = mapOf(
                    "chatRoomId" to criteria.chatRoomId,
                    "userId" to criteria.userId,
                    "reason" to "채팅방 생성자만 수정할 수 있습니다"
                )
            )
        }

        val command = criteria.toCommand()
        return chatRoomDomainService.update(command)
    }

    /**
     * 채팅방 삭제
     */
    fun delete(criteria: ChatRoomCriteria.Delete): ChatRoomInfo.Detail {
        // 권한 검증: 생성자만 삭제 가능
        val chatRoomInfo = chatRoomDomainService.getById(criteria.chatRoomId)
        if (chatRoomInfo.createdByUserId != criteria.userId) {
            throw DomainException(
                error = ChatRoomError.UNAUTHORIZED_ACCESS,
                details = mapOf(
                    "chatRoomId" to criteria.chatRoomId,
                    "userId" to criteria.userId,
                    "reason" to "채팅방 생성자만 삭제할 수 있습니다"
                )
            )
        }

        val command = criteria.toCommand()
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

    /**
     * 내가 참여한 채팅방 목록 조회
     */
    fun getByMemberUserId(userId: String): List<ChatRoomInfo.Detail> {
        // 사용자가 참여한 채팅방 멤버 정보 조회
        val memberInfoList = chatRoomMemberApplicationService.getByUserId(userId)
        
        // 빈 리스트 조기 반환
        if (memberInfoList.isEmpty()) {
            return emptyList()
        }
        
        // 채팅방 ID 목록 추출
        val chatRoomIds = memberInfoList.map { it.chatRoomId }
        
        // 한 번의 쿼리로 모든 채팅방 정보 조회 (N+1 문제 해결)
        return chatRoomDomainService.getByIds(chatRoomIds)
    }
}
