package site.rahoon.message.monolithic.channelconversation.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationCommand
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationError
import site.rahoon.message.monolithic.common.domain.DomainException

interface ChannelConversationCreateValidator {
    fun validate(command: ChannelConversationCommand.Create)
}

@Component
class ChannelConversationCreateValidatorImpl : ChannelConversationCreateValidator {
    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 100
    }

    override fun validate(command: ChannelConversationCommand.Create) {
        validateChatRoomId(command.chatRoomId)
        validateChannelId(command.channelId)
        validateCustomerId(command.customerId)
        validateName(command.name)
    }

    private fun validateChatRoomId(chatRoomId: String) {
        if (chatRoomId.isBlank()) {
            throw DomainException(
                error = ChannelConversationError.INVALID_CHAT_ROOM_ID,
                details = mapOf("reason" to "채팅방 ID는 필수입니다"),
            )
        }
    }

    private fun validateChannelId(channelId: String) {
        if (channelId.isBlank()) {
            throw DomainException(
                error = ChannelConversationError.INVALID_CHANNEL_ID,
                details = mapOf("reason" to "채널 ID는 필수입니다"),
            )
        }
    }

    private fun validateCustomerId(customerId: String) {
        if (customerId.isBlank()) {
            throw DomainException(
                error = ChannelConversationError.INVALID_CUSTOMER_ID,
                details = mapOf("reason" to "고객 ID는 필수입니다"),
            )
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw DomainException(
                error = ChannelConversationError.INVALID_NAME,
                details = mapOf("reason" to "상담 세션 이름은 필수입니다"),
            )
        }
        if (name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH) {
            throw DomainException(
                error = ChannelConversationError.INVALID_NAME,
                details = mapOf(
                    "name" to name,
                    "reason" to "상담 세션 이름은 1자 이상 100자 이하여야 합니다",
                ),
            )
        }
    }
}
