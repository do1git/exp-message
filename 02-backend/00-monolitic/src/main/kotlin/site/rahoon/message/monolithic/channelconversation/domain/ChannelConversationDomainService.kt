package site.rahoon.message.monolithic.channelconversation.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channelconversation.domain.component.ChannelConversationCreateValidator
import site.rahoon.message.monolithic.channelconversation.domain.component.ChannelConversationUpdateValidator
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * ChannelConversation 도메인 서비스.
 * ChatRoom 생성/수정/삭제는 Application 레이어에서 조율합니다.
 */
@Service
@Transactional(readOnly = true)
class ChannelConversationDomainService(
    private val channelConversationRepository: ChannelConversationRepository,
    private val channelConversationCreateValidator: ChannelConversationCreateValidator,
    private val channelConversationUpdateValidator: ChannelConversationUpdateValidator,
) {
    @Transactional
    fun create(command: ChannelConversationCommand.Create): ChannelConversationInfo.Detail {
        channelConversationCreateValidator.validate(command)

        val channelConversation =
            ChannelConversation.create(
                chatRoomId = command.chatRoomId,
                channelId = command.channelId,
                customerId = command.customerId,
                name = command.name,
            )

        val saved = channelConversationRepository.save(channelConversation)
        return ChannelConversationInfo.Detail.from(saved)
    }

    @Transactional
    fun update(command: ChannelConversationCommand.Update): ChannelConversationInfo.Detail {
        channelConversationUpdateValidator.validate(command)

        val channelConversation =
            channelConversationRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("channelConversationId" to command.id),
                )

        val updated = channelConversation.updateName(command.name)
        val saved = channelConversationRepository.save(updated)
        return ChannelConversationInfo.Detail.from(saved)
    }

    @Transactional
    fun delete(command: ChannelConversationCommand.Delete): ChannelConversationInfo.Detail {
        val channelConversation =
            channelConversationRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("channelConversationId" to command.id),
                )

        channelConversationRepository.delete(command.id)
        return ChannelConversationInfo.Detail.from(channelConversation)
    }

    fun getById(id: String): ChannelConversationInfo.Detail {
        val channelConversation =
            channelConversationRepository.findById(id)
                ?: throw DomainException(
                    error = ChannelConversationError.CHANNEL_CONVERSATION_NOT_FOUND,
                    details = mapOf("channelConversationId" to id),
                )
        return ChannelConversationInfo.Detail.from(channelConversation)
    }

    fun getByChannelId(channelId: String): List<ChannelConversationInfo.Detail> {
        val conversations = channelConversationRepository.findByChannelId(channelId)
        return conversations.map { ChannelConversationInfo.Detail.from(it) }
    }

    fun getByCustomerId(customerId: String): List<ChannelConversationInfo.Detail> {
        val conversations = channelConversationRepository.findByCustomerId(customerId)
        return conversations.map { ChannelConversationInfo.Detail.from(it) }
    }
}
