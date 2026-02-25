package site.rahoon.message.monolithic.channel.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channel.domain.ChannelCommand

/**
 * Channel Application Layer 입력 DTO
 */
object ChannelCriteria {
    data class Create(
        @field:NotBlank(message = "채널 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채널 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCommand(): ChannelCommand.Create =
            ChannelCommand.Create(
                name = this.name,
            )
    }

    data class Update(
        val channelId: String,
        @field:Size(min = 1, max = 100, message = "채널 이름은 1자 이상 100자 이하여야 합니다")
        val name: String? = null,
    ) {
        fun toCommand(): ChannelCommand.Update =
            ChannelCommand.Update(
                id = this.channelId,
                name = this.name,
            )
    }

    data class Delete(
        val channelId: String,
    ) {
        fun toCommand(): ChannelCommand.Delete =
            ChannelCommand.Delete(
                id = this.channelId,
            )
    }
}
