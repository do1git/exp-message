package site.rahoon.message.__monolitic.chatroom.domain

sealed class ChatRoomCommand {
    data class Create(
        val name: String,
        val createdByUserId: String
    ) : ChatRoomCommand()

    data class Update(
        val id: String,
        val name: String
    ) : ChatRoomCommand()

    data class Delete(
        val id: String
    ) : ChatRoomCommand()
}
