package site.rahoon.message.monolithic.channel.infrastructure

import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
interface ChannelJpaRepository : JpaSoftDeleteRepository<ChannelEntity, String> {
    fun findByApiKey(apiKey: String): ChannelEntity?
}
