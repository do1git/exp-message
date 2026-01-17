package site.rahoon.message.monolithic.message.domain

import java.time.LocalDateTime
import java.util.UUID

data class Message(
    val id: String,
    val chatRoomId: String,
    val userId: String,
    val content: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 Message를 생성합니다.
         */
        fun create(
            chatRoomId: String,
            userId: String,
            content: String,
        ): Message =
            Message(
                id = UUID.randomUUID().toString(),
                chatRoomId = chatRoomId,
                userId = userId,
                content = content,
                createdAt = LocalDateTime.now(),
            )
    }
}
