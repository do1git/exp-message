package site.rahoon.message.__monolitic.chatroom.domain

import java.time.LocalDateTime

/**
 * ChatRoom 정보를 외부에 노출할 때 사용하는 객체
 */
object ChatRoomInfo {
    data class Detail(
        val id: String,
        val name: String,
        val createdByUserId: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        companion object {
            /**
             * ChatRoom을 ChatRoomInfo.Detail로 변환합니다.
             */
            fun from(chatRoom: ChatRoom): Detail {
                return Detail(
                    id = chatRoom.id,
                    name = chatRoom.name,
                    createdByUserId = chatRoom.createdByUserId,
                    createdAt = chatRoom.createdAt,
                    updatedAt = chatRoom.updatedAt
                )
            }
        }
    }
}
