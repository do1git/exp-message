package site.rahoon.message.__monolitic.message.application

import site.rahoon.message.__monolitic.message.domain.MessageCommand

/**
 * Message Application 레이어에서 사용하는 입력 DTO
 */
object MessageCriteria {
    data class Create(
        val chatRoomId: String,
        val userId: String,
        val content: String
    ) {
        fun toCommand(): MessageCommand.Create {
            return MessageCommand.Create(
                chatRoomId = chatRoomId,
                userId = userId,
                content = content
            )
        }
    }

    data class GetByChatRoomId(
        val chatRoomId: String,
        val cursor: String?,
        val limit: Int
    )
}
