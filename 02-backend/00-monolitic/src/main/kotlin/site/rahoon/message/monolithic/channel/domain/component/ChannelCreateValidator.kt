package site.rahoon.message.monolithic.channel.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.channel.domain.ChannelCommand
import site.rahoon.message.monolithic.channel.domain.ChannelError
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * Channel 생성 시 입력값 검증을 위한 인터페이스
 */
interface ChannelCreateValidator {
    /**
     * ChannelCommand.Create를 검증합니다.
     * 검증 실패 시 DomainException을 발생시킵니다.
     */
    fun validate(command: ChannelCommand.Create)
}

@Component
class ChannelCreateValidatorImpl : ChannelCreateValidator {
    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 100
    }

    override fun validate(command: ChannelCommand.Create) {
        validateName(command.name)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw DomainException(
                error = ChannelError.INVALID_NAME,
                details = mapOf("reason" to "채널 이름은 필수입니다"),
            )
        }
        if (name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH) {
            throw DomainException(
                error = ChannelError.INVALID_NAME,
                details = mapOf(
                    "name" to name,
                    "reason" to "채널 이름은 1자 이상 100자 이하여야 합니다",
                ),
            )
        }
    }
}
