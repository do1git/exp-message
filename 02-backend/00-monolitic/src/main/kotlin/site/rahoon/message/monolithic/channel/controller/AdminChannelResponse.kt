package site.rahoon.message.monolithic.channel.controller

import site.rahoon.message.monolithic.channel.domain.ChannelInfo
import java.time.LocalDateTime

/**
 * Admin Channel Controller 응답 DTO
 */
object AdminChannelResponse {
    data class Create(
        val id: String,
        val name: String,
        val apiKey: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(channelInfo: ChannelInfo.Detail): Create =
                Create(
                    id = channelInfo.id,
                    name = channelInfo.name,
                    apiKey = channelInfo.apiKey,
                    createdAt = channelInfo.createdAt,
                    updatedAt = channelInfo.updatedAt,
                )
        }
    }

    data class Detail(
        val id: String,
        val name: String,
        val apiKey: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(channelInfo: ChannelInfo.Detail): Detail =
                Detail(
                    id = channelInfo.id,
                    name = channelInfo.name,
                    apiKey = channelInfo.apiKey,
                    createdAt = channelInfo.createdAt,
                    updatedAt = channelInfo.updatedAt,
                )
        }
    }
}
