package site.rahoon.message.monolithic.channeloperator.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorCommand
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorError
import site.rahoon.message.monolithic.common.domain.DomainException

interface ChannelOperatorUpdateValidator {
    fun validate(command: ChannelOperatorCommand.Update)
}

@Component
class ChannelOperatorUpdateValidatorImpl : ChannelOperatorUpdateValidator {
    companion object {
        private const val NICKNAME_MIN_LENGTH = 1
        private const val NICKNAME_MAX_LENGTH = 50
    }

    override fun validate(command: ChannelOperatorCommand.Update) {
        validateNickname(command.nickname)
    }

    private fun validateNickname(nickname: String) {
        if (nickname.isBlank()) {
            throw DomainException(
                error = ChannelOperatorError.INVALID_NICKNAME,
                details = mapOf("reason" to "상담원 표시명은 필수입니다"),
            )
        }
        if (nickname.length < NICKNAME_MIN_LENGTH || nickname.length > NICKNAME_MAX_LENGTH) {
            throw DomainException(
                error = ChannelOperatorError.INVALID_NICKNAME,
                details = mapOf(
                    "reason" to "상담원 표시명은 1자 이상 50자 이하여야 합니다",
                ),
            )
        }
    }
}
