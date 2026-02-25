package site.rahoon.message.monolithic.channeloperator.infrastructure

import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
interface ChannelOperatorJpaRepository : JpaSoftDeleteRepository<ChannelOperatorEntity, String> {
    fun findByChannelId(channelId: String): List<ChannelOperatorEntity>

    fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelOperatorEntity?
}
