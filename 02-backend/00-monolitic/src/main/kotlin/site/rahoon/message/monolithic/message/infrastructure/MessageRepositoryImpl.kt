package site.rahoon.message.monolithic.message.infrastructure

import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import site.rahoon.message.monolithic.message.domain.Message
import site.rahoon.message.monolithic.message.domain.MessageRepository
import java.time.LocalDateTime

/**
 * MessageRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class MessageRepositoryImpl(
    private val jpaRepository: MessageJpaRepository,
) : MessageRepository {
    @Transactional
    override fun save(message: Message): Message {
        val entity = toEntity(message)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): Message? = jpaRepository.findByIdOrNull(id)?.let { toDomain(it) }

    override fun findPageByChatRoomId(
        chatRoomId: String,
        afterCreatedAt: LocalDateTime?,
        afterId: String?,
        limit: Int,
    ): List<Message> {
        // 정렬은 Repository 쿼리(메서드명/JPQL)의 order by로만 처리합니다.
        // Pageable에 Sort를 주면 order by가 중복으로 생성될 수 있습니다.
        val pageable = PageRequest.of(0, limit)

        val entities =
            if (afterCreatedAt == null || afterId == null) {
                jpaRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoomId, pageable)
            } else {
                jpaRepository.findNextPageByChatRoomId(chatRoomId, afterCreatedAt, afterId, pageable)
            }

        return entities.map { toDomain(it) }
    }

    private fun toEntity(message: Message): MessageEntity =
        MessageEntity(
            id = message.id,
            chatRoomId = message.chatRoomId,
            userId = message.userId,
            content = message.content,
            createdAt = message.createdAt,
        )

    private fun toDomain(entity: MessageEntity): Message =
        Message(
            id = entity.id,
            chatRoomId = entity.chatRoomId,
            userId = entity.userId,
            content = entity.content,
            createdAt = entity.createdAt,
        )
}
