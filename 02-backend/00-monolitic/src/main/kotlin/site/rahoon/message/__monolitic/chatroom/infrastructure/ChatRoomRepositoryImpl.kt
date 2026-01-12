package site.rahoon.message.__monolitic.chatroom.infrastructure

import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoom
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomRepository

/**
 * ChatRoomRepository 인터페이스의 JPA 구현체
 */
@Repository
class ChatRoomRepositoryImpl(
    private val jpaRepository: ChatRoomJpaRepository
) : ChatRoomRepository {

    override fun save(chatRoom: ChatRoom): ChatRoom {
        val entity = toEntity(chatRoom)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): ChatRoom? {
        return jpaRepository.findById(id)
            .map { toDomain(it) }
            .orElse(null)
    }

    override fun findByCreatedByUserId(userId: String): List<ChatRoom> {
        return jpaRepository.findByCreatedByUserId(userId)
            .map { toDomain(it) }
    }

    override fun delete(id: String) {
        jpaRepository.deleteById(id)
    }

    private fun toEntity(chatRoom: ChatRoom): ChatRoomEntity {
        return ChatRoomEntity(
            id = chatRoom.id,
            name = chatRoom.name,
            createdByUserId = chatRoom.createdByUserId,
            createdAt = chatRoom.createdAt,
            updatedAt = chatRoom.updatedAt
        )
    }

    private fun toDomain(entity: ChatRoomEntity): ChatRoom {
        return ChatRoom(
            id = entity.id,
            name = entity.name,
            createdByUserId = entity.createdByUserId,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
