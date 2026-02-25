package site.rahoon.message.monolithic.channelconversation.domain

sealed class ChannelConversationCommand {
    /**
     * Application 레이어에서 ChatRoom 생성 후 chatRoomId를 전달받습니다.
     */
    data class Create(
        val chatRoomId: String,
        val channelId: String,
        val customerId: String,
        val name: String,
    ) : ChannelConversationCommand()

    data class Update(
        val id: String,
        val name: String,
    ) : ChannelConversationCommand()

    data class Delete(
        val id: String,
    ) : ChannelConversationCommand()
}
