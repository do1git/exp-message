package site.rahoon.message.monolithic.channel.domain

interface ChannelRepository {
    fun save(channel: Channel): Channel

    fun findById(id: String): Channel?

    fun findByApiKey(apiKey: String): Channel?

    fun delete(id: String)
}
