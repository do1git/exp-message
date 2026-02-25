package site.rahoon.message.monolithic.channelconversation.domain

interface ChannelConversationRepository {
    fun save(channelConversation: ChannelConversation): ChannelConversation

    fun findById(id: String): ChannelConversation?

    fun findByChannelId(channelId: String): List<ChannelConversation>

    fun findByCustomerId(customerId: String): List<ChannelConversation>

    fun delete(id: String)
}
