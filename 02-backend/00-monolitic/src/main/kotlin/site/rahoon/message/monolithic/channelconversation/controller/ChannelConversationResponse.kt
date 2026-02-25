package site.rahoon.message.monolithic.channelconversation.controller

import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationInfo
import java.time.LocalDateTime

/**
 * ChannelConversation Controller 응답 DTO
 */
object ChannelConversationResponse {
    data class Create(
        val id: String,
        val channelId: String,
        val customerId: String,
        val name: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelConversationInfo.Detail): Create =
                Create(
                    id = info.id,
                    channelId = info.channelId,
                    customerId = info.customerId,
                    name = info.name,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }

    data class Detail(
        val id: String,
        val channelId: String,
        val customerId: String,
        val name: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelConversationInfo.Detail): Detail =
                Detail(
                    id = info.id,
                    channelId = info.channelId,
                    customerId = info.customerId,
                    name = info.name,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }

    data class ListItem(
        val id: String,
        val channelId: String,
        val customerId: String,
        val name: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelConversationInfo.Detail): ListItem =
                ListItem(
                    id = info.id,
                    channelId = info.channelId,
                    customerId = info.customerId,
                    name = info.name,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }
}
