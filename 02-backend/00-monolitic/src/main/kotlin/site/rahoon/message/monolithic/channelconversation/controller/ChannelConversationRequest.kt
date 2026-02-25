package site.rahoon.message.monolithic.channelconversation.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channelconversation.application.ChannelConversationCriteria

/**
 * ChannelConversation Controller 요청 DTO
 */
object ChannelConversationRequest {
    data class Create(
        @field:NotBlank(message = "상담 세션 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "상담 세션 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCriteria(
            channelId: String,
            customerId: String,
        ): ChannelConversationCriteria.Create =
            ChannelConversationCriteria.Create(
                channelId = channelId,
                customerId = customerId,
                name = this.name,
            )
    }

    data class Update(
        @field:NotBlank(message = "상담 세션 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "상담 세션 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCriteria(channelConversationId: String): ChannelConversationCriteria.Update =
            ChannelConversationCriteria.Update(
                channelConversationId = channelConversationId,
                name = this.name,
            )
    }
}
