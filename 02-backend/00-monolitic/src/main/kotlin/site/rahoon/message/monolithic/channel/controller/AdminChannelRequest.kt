package site.rahoon.message.monolithic.channel.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channel.application.ChannelCriteria

/**
 * Admin Channel Controller 요청 DTO
 */
object AdminChannelRequest {
    data class Create(
        @field:NotBlank(message = "채널 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채널 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCriteria(): ChannelCriteria.Create =
            ChannelCriteria.Create(
                name = this.name,
            )
    }

    data class Update(
        @field:Size(min = 1, max = 100, message = "채널 이름은 1자 이상 100자 이하여야 합니다")
        val name: String? = null,
    ) {
        fun toCriteria(channelId: String): ChannelCriteria.Update =
            ChannelCriteria.Update(
                channelId = channelId,
                name = this.name,
            )
    }
}
