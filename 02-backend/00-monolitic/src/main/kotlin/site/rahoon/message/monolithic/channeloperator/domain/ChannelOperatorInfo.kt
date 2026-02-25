package site.rahoon.message.monolithic.channeloperator.domain

import java.time.LocalDateTime

/**
 * ChannelOperator 정보를 외부에 노출할 때 사용하는 객체
 */
object ChannelOperatorInfo {
    data class Detail(
        val id: String,
        val channelId: String,
        val userId: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(channelOperator: ChannelOperator): Detail =
                Detail(
                    id = channelOperator.id,
                    channelId = channelOperator.channelId,
                    userId = channelOperator.userId,
                    nickname = channelOperator.nickname,
                    createdAt = channelOperator.createdAt,
                    updatedAt = channelOperator.updatedAt,
                )
        }
    }
}
