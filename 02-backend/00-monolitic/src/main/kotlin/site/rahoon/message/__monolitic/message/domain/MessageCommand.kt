package site.rahoon.message.__monolitic.message.domain

sealed class MessageCommand {
    data class Create(
        val chatRoomId: String,
        val userId: String,
        val content: String
    ) : MessageCommand()
}
