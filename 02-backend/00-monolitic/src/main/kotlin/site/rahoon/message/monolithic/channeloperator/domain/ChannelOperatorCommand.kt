package site.rahoon.message.monolithic.channeloperator.domain

sealed class ChannelOperatorCommand {
    data class Create(
        val channelId: String,
        val userId: String,
        val nickname: String,
    ) : ChannelOperatorCommand()

    data class Update(
        val id: String,
        val nickname: String,
    ) : ChannelOperatorCommand()

    data class Delete(
        val id: String,
    ) : ChannelOperatorCommand()
}
