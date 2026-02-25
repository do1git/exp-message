package site.rahoon.message.monolithic.channel.domain

import java.time.LocalDateTime

/**
 * Channel 정보를 외부에 노출할 때 사용하는 객체
 */
object ChannelInfo {
    data class Detail(
        val id: String,
        val name: String,
        val apiKey: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Channel을 ChannelInfo.Detail로 변환합니다.
             */
            fun from(channel: Channel): Detail =
                Detail(
                    id = channel.id,
                    name = channel.name,
                    apiKey = channel.apiKey,
                    createdAt = channel.createdAt,
                    updatedAt = channel.updatedAt,
                )
        }
    }
}
