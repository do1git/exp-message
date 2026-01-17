package site.rahoon.message.monolithic.chatroom.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomCommand

/**
 * ChatRoom Application Layer 입력 DTO
 */
object ChatRoomCriteria {
    data class Create(
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
        val createdByUserId: String,
    ) {
        /**
         * ChatRoomCommand.Create로 변환합니다.
         */
        fun toCommand(): ChatRoomCommand.Create =
            ChatRoomCommand.Create(
                name = this.name,
                createdByUserId = this.createdByUserId,
            )
    }

    data class Update(
        val chatRoomId: String,
        @field:NotBlank(message = "채팅방 이름은 필수입니다")
        @field:Size(min = 1, max = 100, message = "채팅방 이름은 1자 이상 100자 이하여야 합니다")
        val name: String,
        val userId: String,
    ) {
        /**
         * ChatRoomCommand.Update로 변환합니다.
         */
        fun toCommand(): ChatRoomCommand.Update =
            ChatRoomCommand.Update(
                id = this.chatRoomId,
                name = this.name,
            )
    }

    data class Delete(
        val chatRoomId: String,
        val userId: String,
    ) {
        /**
         * ChatRoomCommand.Delete로 변환합니다.
         */
        fun toCommand(): ChatRoomCommand.Delete =
            ChatRoomCommand.Delete(
                id = this.chatRoomId,
            )
    }
}
