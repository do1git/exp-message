package site.rahoon.message.monolithic.channeloperator.controller

import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorInfo
import java.time.LocalDateTime

/**
 * ChannelOperator Controller 응답 DTO
 */
object ChannelOperatorResponse {
    data class Create(
        val id: String,
        val channelId: String,
        val userId: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelOperatorInfo.Detail): Create =
                Create(
                    id = info.id,
                    channelId = info.channelId,
                    userId = info.userId,
                    nickname = info.nickname,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }

    data class Detail(
        val id: String,
        val channelId: String,
        val userId: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelOperatorInfo.Detail): Detail =
                Detail(
                    id = info.id,
                    channelId = info.channelId,
                    userId = info.userId,
                    nickname = info.nickname,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }

    data class ListItem(
        val id: String,
        val channelId: String,
        val userId: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(info: ChannelOperatorInfo.Detail): ListItem =
                ListItem(
                    id = info.id,
                    channelId = info.channelId,
                    userId = info.userId,
                    nickname = info.nickname,
                    createdAt = info.createdAt,
                    updatedAt = info.updatedAt,
                )
        }
    }
}
