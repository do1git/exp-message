package site.rahoon.message.monolithic.channel.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channel.domain.Channel
import site.rahoon.message.monolithic.channel.domain.ChannelRepository
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import java.time.LocalDateTime

/**
 * ChannelRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class ChannelRepositoryImpl(
    private val jpaRepository: ChannelJpaRepository,
) : ChannelRepository {
    @Transactional
    override fun save(channel: Channel): Channel {
        val entity = toEntity(channel)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): Channel? = jpaRepository.findByIdOrNull(id)?.let { toDomain(it) }

    override fun findByApiKey(apiKey: String): Channel? = jpaRepository.findByApiKey(apiKey)?.let { toDomain(it) }

    @Transactional
    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    private fun toEntity(channel: Channel): ChannelEntity =
        ChannelEntity(
            id = channel.id,
            name = channel.name,
            apiKey = channel.apiKey,
            createdAt = channel.createdAt,
            updatedAt = channel.updatedAt,
        )

    private fun toDomain(entity: ChannelEntity): Channel =
        Channel(
            id = entity.id,
            name = entity.name,
            apiKey = entity.apiKey,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
