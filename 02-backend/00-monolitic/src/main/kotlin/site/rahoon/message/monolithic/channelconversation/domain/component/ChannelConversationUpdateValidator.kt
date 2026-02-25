package site.rahoon.message.monolithic.channelconversation.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationCommand
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationError
import site.rahoon.message.monolithic.common.domain.DomainException

interface ChannelConversationUpdateValidator {
    fun validate(command: ChannelConversationCommand.Update)
}

@Component
class ChannelConversationUpdateValidatorImpl : ChannelConversationUpdateValidator {
    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 100
    }

    override fun validate(command: ChannelConversationCommand.Update) {
        validateName(command.name)
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
