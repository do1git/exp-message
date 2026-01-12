package site.rahoon.message.__monolitic.chatroom.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomCommand

/**
 * ChatRoom Application Layer 입력 DTO
 */
object ChatRoomCriteria {
    data class Create(
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String
    ) {
        /**
         * ChatRoomCommand.Create로 변환합니다.
         */
        fun toCommand(createdByUserId: String): ChatRoomCommand.Create {
            return ChatRoomCommand.Create(
                name = this.name,
                createdByUserId = createdByUserId
            )
        }
    }

    data class Update(
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String
    ) {
        /**
         * ChatRoomCommand.Update로 변환합니다.
         */
        fun toCommand(chatRoomId: String): ChatRoomCommand.Update {
            return ChatRoomCommand.Update(
                id = chatRoomId,
                name = this.name
            )
        }
    }
}
