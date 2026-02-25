package site.rahoon.message.monolithic.channeloperator.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperator
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorRepository
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import java.time.LocalDateTime

/**
 * ChannelOperatorRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class ChannelOperatorRepositoryImpl(
    private val jpaRepository: ChannelOperatorJpaRepository,
) : ChannelOperatorRepository {
    @Transactional
    override fun save(channelOperator: ChannelOperator): ChannelOperator {
        val entity = toEntity(channelOperator)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): ChannelOperator? = jpaRepository.findByIdOrNull(id)?.let { toDomain(it) }

    override fun findByChannelId(channelId: String): List<ChannelOperator> = jpaRepository.findByChannelId(channelId).map { toDomain(it) }

    override fun findByChannelIdAndUserId(
        channelId: String,
        userId: String,
    ): ChannelOperator? = jpaRepository.findByChannelIdAndUserId(channelId, userId)?.let { toDomain(it) }

    @Transactional
    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    private fun toEntity(channelOperator: ChannelOperator): ChannelOperatorEntity =
        ChannelOperatorEntity(
            id = channelOperator.id,
            channelId = channelOperator.channelId,
            userId = channelOperator.userId,
            nickname = channelOperator.nickname,
            createdAt = channelOperator.createdAt,
            updatedAt = channelOperator.updatedAt,
        )

    private fun toDomain(entity: ChannelOperatorEntity): ChannelOperator =
        ChannelOperator(
            id = entity.id,
            channelId = entity.channelId,
            userId = entity.userId,
            nickname = entity.nickname,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
