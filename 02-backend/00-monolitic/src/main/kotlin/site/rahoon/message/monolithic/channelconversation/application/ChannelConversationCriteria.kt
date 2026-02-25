package site.rahoon.message.monolithic.channelconversation.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationCommand

/**
 * ChannelConversation Application Layer 입력 DTO
 */
object ChannelConversationCriteria {
    data class Create(
        val channelId: String,
        val customerId: String,
        @field:NotBlank(message = "상담 세션 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "상담 세션 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCommand(chatRoomId: String): ChannelConversationCommand.Create =
            ChannelConversationCommand.Create(
                chatRoomId = chatRoomId,
                channelId = this.channelId,
                customerId = this.customerId,
                name = this.name,
            )
    }

    data class Update(
        val channelConversationId: String,
        @field:NotBlank(message = "상담 세션 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "상담 세션 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
    ) {
        fun toCommand(): ChannelConversationCommand.Update =
            ChannelConversationCommand.Update(
                id = this.channelConversationId,
                name = this.name,
            )
    }

    data class Delete(
        val channelConversationId: String,
    ) {
        fun toCommand(): ChannelConversationCommand.Delete =
            ChannelConversationCommand.Delete(
                id = this.channelConversationId,
            )
    }
}
