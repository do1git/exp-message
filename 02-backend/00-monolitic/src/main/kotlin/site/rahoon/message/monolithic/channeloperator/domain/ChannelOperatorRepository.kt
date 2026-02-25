package site.rahoon.message.monolithic.channeloperator.domain

interface ChannelOperatorRepository {
    fun save(channelOperator: ChannelOperator): ChannelOperator

    fun findById(id: String): ChannelOperator?

    fun findByChannelId(channelId: String): List<ChannelOperator>

    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelOperator?

    fun delete(id: String)
}
