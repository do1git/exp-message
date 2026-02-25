package site.rahoon.message.monolithic.channelconversation.application

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationDomainService
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationInfo
import site.rahoon.message.monolithic.chatroom.application.ChatRoomApplicationService
import site.rahoon.message.monolithic.chatroom.application.ChatRoomCriteria

/**
 * ChannelConversation Application Service
 * ChatRoom 생성/삭제를 조율하고 ChannelConversation을 생성합니다.
 */
@Service
class ChannelConversationApplicationService(
    private val channelConversationDomainService: ChannelConversationDomainService,
    private val chatRoomApplicationService: ChatRoomApplicationService,
) {
    /**
     * 상담 세션 생성
     * 1. ChatRoom 생성 (고객이 생성자, ChatRoomMember 자동 추가)
     * 2. ChannelConversation 생성
     */
    @Transactional
    fun create(criteria: ChannelConversationCriteria.Create): ChannelConversationInfo.Detail {
        val chatRoomCriteria =
            ChatRoomCriteria.Create(
                name = criteria.name,
                createdByUserId = criteria.customerId,
            )
        val chatRoomInfo = chatRoomApplicationService.create(chatRoomCriteria)

        val command = criteria.toCommand(chatRoomInfo.id)
        return channelConversationDomainService.create(command)
    }

    @Transactional
    fun update(criteria: ChannelConversationCriteria.Update): ChannelConversationInfo.Detail {
        val existingConversation = channelConversationDomainService.getById(criteria.channelConversationId)
        val command = criteria.toCommand()
        val conversationInfo = channelConversationDomainService.update(command)

        chatRoomApplicationService.update(
            ChatRoomCriteria.Update(
                chatRoomId = conversationInfo.id,
                name = criteria.name,
                userId = existingConversation.customerId,
            ),
        )
        return conversationInfo
    }

    /**
     * 상담 세션 삭제
     * ChannelConversation과 ChatRoom 모두 삭제
     */
    @Transactional
    fun delete(criteria: ChannelConversationCriteria.Delete): ChannelConversationInfo.Detail {
        val conversationInfo = channelConversationDomainService.getById(criteria.channelConversationId)
        val chatRoomInfo = chatRoomApplicationService.getById(conversationInfo.id)

        val deletedConversation = channelConversationDomainService.delete(criteria.toCommand())
        chatRoomApplicationService.delete(
            ChatRoomCriteria.Delete(
                chatRoomId = conversationInfo.id,
                userId = chatRoomInfo.createdByUserId,
            ),
        )
        return deletedConversation
    }

    fun getById(id: String): ChannelConversationInfo.Detail = channelConversationDomainService.getById(id)

    fun getByChannelId(channelId: String): List<ChannelConversationInfo.Detail> = channelConversationDomainService.getByChannelId(channelId)

    fun getByCustomerId(customerId: String): List<ChannelConversationInfo.Detail> =
        channelConversationDomainService.getByCustomerId(customerId)
}
