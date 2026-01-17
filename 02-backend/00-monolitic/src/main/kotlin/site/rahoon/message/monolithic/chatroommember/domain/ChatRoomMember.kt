package site.rahoon.message.monolithic.chatroommember.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChatRoomMember(
    val id: String,
    val chatRoomId: String,
    val userId: String,
    val joinedAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 ChatRoomMember를 생성합니다.
         */
        fun create(
            chatRoomId: String,
            userId: String,
        ): ChatRoomMember =
            ChatRoomMember(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = userId,
                joinedAt = LocalDateTime.now(),
            )
    }
}
