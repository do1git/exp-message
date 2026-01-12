package site.rahoon.message.__monolitic.message.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.message.domain.Message
import site.rahoon.message.__monolitic.message.domain.MessageRepository

/**
 * MessageRepository 인터페이스의 JPA 구현체
 */
@Repository
class MessageRepositoryImpl(
    private val jpaRepository: MessageJpaRepository
) : MessageRepository {

    override fun save(message: Message): Message {
        val entity = toEntity(message)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): Message? {
        return jpaRepository.findById(id)
            .map { toDomain(it) }
            .orElse(null)
    }

    override fun findByChatRoomIdOrderByCreatedAtDesc(chatRoomId: String): List<Message> {
        return jpaRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId)
            .map { toDomain(it) }
    }

    private fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            chatRoomId = message.chatRoomId,
            userId = message.userId,
            content = message.content,
            createdAt = message.createdAt
        )
    }

    private fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            chatRoomId = entity.chatRoomId,
            userId = entity.userId,
            content = entity.content,
            createdAt = entity.createdAt
        )
    }
}
