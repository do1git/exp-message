package site.rahoon.message.monolithic.channelconversation.domain

import java.time.LocalDateTime

/**
 * 채널별 상담 세션 도메인.
 * ChatRoom의 확장. 채널에 속한 상담 대화. 고객(User)이 첫 질문 시 생성.
 * id는 ChatRoom id와 동일하여 1:1 관계를 가집니다.
 * ChatRoom은 Application 레이어에서 생성 후 chatRoomId를 전달받습니다.
 */
data class ChannelConversation(
    val id: String,
    val channelId: String,
    val customerId: String,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 ChannelConversation을 생성합니다.
         * chatRoomId는 Application 레이어에서 ChatRoom 생성 후 전달받은 값입니다.
         */
        fun create(
            chatRoomId: String,
            channelId: String,
            customerId: String,
            name: String,
        ): ChannelConversation {
            val now = LocalDateTime.now()
            return ChannelConversation(
                id = chatRoomId,
                channelId = channelId,
                customerId = customerId,
                name = name,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /**
     * 상담 세션 이름을 업데이트합니다.
     */
    fun updateName(newName: String): ChannelConversation =
        this.copy(
            name = newName,
            updatedAt = LocalDateTime.now(),
        )
}
