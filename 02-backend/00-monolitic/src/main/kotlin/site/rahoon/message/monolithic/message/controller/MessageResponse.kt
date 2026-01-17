package site.rahoon.message.monolithic.message.controller

import site.rahoon.message.monolithic.message.domain.Message

/**
 * Message Controller 응답 DTO
 */
object MessageResponse {
    /**
     * 메시지 전송 응답
     */
    data class Create(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val content: String,
        val createdAt: java.time.LocalDateTime,
    ) {
        companion object {
            /**
             * Message로부터 MessageResponse.Create를 생성합니다.
             */
            fun from(message: Message): Create =
                Create(
                    id = message.id,
                    chatRoomId = message.chatRoomId,
                    userId = message.userId,
                    content = message.content,
                    createdAt = message.createdAt,
                )
        }
    }

    /**
     * 메시지 조회 응답
     */
    data class Detail(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val content: String,
        val createdAt: java.time.LocalDateTime,
    ) {
        companion object {
            /**
             * Message로부터 MessageResponse.Detail을 생성합니다.
             */
            fun from(message: Message): Detail =
                Detail(
                    id = message.id,
                    chatRoomId = message.chatRoomId,
                    userId = message.userId,
                    content = message.content,
                    createdAt = message.createdAt,
                )
        }
    }
}
