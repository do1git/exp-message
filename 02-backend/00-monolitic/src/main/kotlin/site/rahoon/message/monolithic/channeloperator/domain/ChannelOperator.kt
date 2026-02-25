package site.rahoon.message.monolithic.channeloperator.domain

import java.time.LocalDateTime
import java.util.UUID

/**
 * 채널 운영자 도메인.
 * 해당 Channel의 상담원. User와 연결하여 로그인·인증 처리.
 */
data class ChannelOperator(
    val id: String,
    val channelId: String,
    val userId: String,
    val nickname: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 ChannelOperator를 생성합니다.
         */
        fun create(
            channelId: String,
            userId: String,
            nickname: String,
        ): ChannelOperator {
            val now = LocalDateTime.now()
            return ChannelOperator(
                id = UUID.randomUUID().toString(),
                channelId = channelId,
                userId = userId,
                nickname = nickname,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /**
     * 상담원 표시명을 업데이트합니다.
     */
    fun updateNickname(newNickname: String): ChannelOperator =
        this.copy(
            nickname = newNickname,
            updatedAt = LocalDateTime.now(),
        )
}
