package site.rahoon.message.__monolitic.chatroom.domain

import java.time.LocalDateTime
import java.util.UUID

data class ChatRoom(
    val id: String,
    val name: String,
    val createdByUserId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        /**
         * 새로운 ChatRoom을 생성합니다.
         */
        fun create(
            name: String,
            createdByUserId: String
        ): ChatRoom {
            val now = LocalDateTime.now()
            return ChatRoom(
                id = UUID.randomUUID().toString(),
                name = name,
                createdByUserId = createdByUserId,
                createdAt = now,
                updatedAt = now
            )
        }
    }

    /**
     * 채팅방 이름을 업데이트합니다.
     */
    fun updateName(newName: String): ChatRoom {
        return this.copy(
            name = newName,
            updatedAt = LocalDateTime.now()
        )
    }
}
