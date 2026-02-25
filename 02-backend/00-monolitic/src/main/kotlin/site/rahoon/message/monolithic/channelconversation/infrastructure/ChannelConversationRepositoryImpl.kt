package site.rahoon.message.monolithic.channelconversation.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversation
import site.rahoon.message.monolithic.channelconversation.domain.ChannelConversationRepository
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import java.time.LocalDateTime

/**
 * ChannelConversationRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class ChannelConversationRepositoryImpl(
    private val jpaRepository: ChannelConversationJpaRepository,
) : ChannelConversationRepository {
    @Transactional
    override fun save(channelConversation: ChannelConversation): ChannelConversation {
        val entity = toEntity(channelConversation)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): ChannelConversation? = jpaRepository.findByIdOrNull(id)?.let { toDomain(it) }

    override fun findByChannelId(channelId: String): List<ChannelConversation> =
        jpaRepository.findByChannelId(channelId).map { toDomain(it) }

    override fun findByCustomerId(customerId: String): List<ChannelConversation> =
        jpaRepository.findByCustomerId(customerId).map { toDomain(it) }

    @Transactional
    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    private fun toEntity(channelConversation: ChannelConversation): ChannelConversationEntity =
        ChannelConversationEntity(
            id = channelConversation.id,
            channelId = channelConversation.channelId,
            customerId = channelConversation.customerId,
            name = channelConversation.name,
            createdAt = channelConversation.createdAt,
            updatedAt = channelConversation.updatedAt,
        )

    private fun toDomain(entity: ChannelConversationEntity): ChannelConversation =
        ChannelConversation(
            id = entity.id,
            channelId = entity.channelId,
            customerId = entity.customerId,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
