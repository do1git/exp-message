package site.rahoon.message.monolithic.channel.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * 서비스 단위 도메인.
 * 위젯이 삽입되는 웹사이트/서비스 단위. 채널톡의 "채널"과 동일 개념.
 */
data class Channel(
    val id: String,
    val name: String,
    val apiKey: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 Channel을 생성합니다.
         * apiKey는 ChannelApiKeyGenerator에서 생성 후 전달받습니다.
         */
        fun create(
            name: String,
            apiKey: String,
        ): Channel {
            val now = LocalDateTime.now()
            return Channel(
                id = UUID.randomUUID().toString(),
                name = name,
                apiKey = apiKey,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /**
     * 채널 이름을 업데이트합니다.
     */
    fun updateName(newName: String): Channel =
        this.copy(
            name = newName,
            updatedAt = LocalDateTime.now(),
        )

    /**
     * API 키를 업데이트합니다.
     */
    fun updateApiKey(newApiKey: String): Channel =
        this.copy(
            apiKey = newApiKey,
            updatedAt = LocalDateTime.now(),
        )
}
