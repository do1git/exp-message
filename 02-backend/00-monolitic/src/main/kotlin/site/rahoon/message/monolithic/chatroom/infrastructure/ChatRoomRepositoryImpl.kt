package site.rahoon.message.monolithic.chatroom.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.chatroom.domain.ChatRoom
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomRepository
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import java.time.LocalDateTime

/**
 * ChatRoomRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class ChatRoomRepositoryImpl(
    private val jpaRepository: ChatRoomJpaRepository,
) : ChatRoomRepository {
    @Transactional
    override fun save(chatRoom: ChatRoom): ChatRoom {
        val entity = toEntity(chatRoom)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): ChatRoom? = jpaRepository.findByIdOrNull(id)?.let { toDomain(it) }

    override fun findByIds(ids: List<String>): List<ChatRoom> {
        if (ids.isEmpty()) return emptyList()
        return jpaRepository
            .findAllById(ids)
            .map { toDomain(it) }
    }

    override fun findByCreatedByUserId(userId: String): List<ChatRoom> =
        jpaRepository
            .findByCreatedByUserId(userId)
            .map { toDomain(it) }

    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    private fun toEntity(chatRoom: ChatRoom): ChatRoomEntity =
        ChatRoomEntity(
            id = chatRoom.id,
            name = chatRoom.name,
            createdByUserId = chatRoom.createdByUserId,
            createdAt = chatRoom.createdAt,
            updatedAt = chatRoom.updatedAt,
        )

    private fun toDomain(entity: ChatRoomEntity): ChatRoom =
        ChatRoom(
            id = entity.id,
            name = entity.name,
            createdByUserId = entity.createdByUserId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
