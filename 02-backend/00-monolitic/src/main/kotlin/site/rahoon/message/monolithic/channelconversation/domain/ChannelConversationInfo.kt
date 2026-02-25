package site.rahoon.message.monolithic.channelconversation.domain

import java.time.LocalDateTime

/**
 * ChannelConversation 정보를 외부에 노출할 때 사용하는 객체
 */
object ChannelConversationInfo {
    data class Detail(
        val id: String,
        val channelId: String,
        val customerId: String,
        val name: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(channelConversation: ChannelConversation): Detail =
                Detail(
                    id = channelConversation.id,
                    channelId = channelConversation.channelId,
                    customerId = channelConversation.customerId,
                    name = channelConversation.name,
                    createdAt = channelConversation.createdAt,
                    updatedAt = channelConversation.updatedAt,
                )
        }
    }
}
