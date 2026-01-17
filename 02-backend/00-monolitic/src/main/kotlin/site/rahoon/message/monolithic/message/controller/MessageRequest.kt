package site.rahoon.message.monolithic.message.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.message.application.MessageCriteria

/**
 * Message Controller 요청 DTO
 */
object MessageRequest {
    /**
     * 메시지 전송 요청
     */
    data class Create(
        @field:NotBlank(message = "채팅방 ID는 필수입니다")
        val chatRoomId: String,
        @field:NotBlank(message = "메시지 내용은 필수입니다")
        @field:Size(max = 10000, message = "메시지 내용은 10000자 이하여야 합니다")
        val content: String,
    ) {
        /**
         * MessageCriteria.Create로 변환합니다.
         */
        fun toCriteria(userId: String): MessageCriteria.Create =
            MessageCriteria.Create(
                chatRoomId = this.chatRoomId,
                userId = userId,
                content = this.content,
            )
    }
}
