package site.rahoon.message.monolithic.channel.domain

sealed class ChannelCommand {
    data class Create(
        val name: String,
    ) : ChannelCommand()

    data class Update(
        val id: String,
        val name: String? = null,
    ) : ChannelCommand()

    data class Delete(
        val id: String,
    ) : ChannelCommand()
}
