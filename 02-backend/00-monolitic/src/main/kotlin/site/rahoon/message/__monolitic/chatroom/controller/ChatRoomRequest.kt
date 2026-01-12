package site.rahoon.message.__monolitic.chatroom.controller

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.__monolitic.chatroom.application.ChatRoomCriteria

/**
 * ChatRoom Controller 요청 DTO
 */
object ChatRoomRequest {
    /**
     * 채팅방 생성 요청
     */
    data class Create(
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String
    ) {
        /**
         * ChatRoomCriteria.Create로 변환합니다.
         */
        fun toCriteria(): ChatRoomCriteria.Create {
            return ChatRoomCriteria.Create(
                name = this.name
            )
        }
    }

    /**
     * 채팅방 수정 요청
     */
    data class Update(
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String
    ) {
        /**
         * ChatRoomCriteria.Update로 변환합니다.
         */
        fun toCriteria(): ChatRoomCriteria.Update {
            return ChatRoomCriteria.Update(
                name = this.name
            )
        }
    }
}
