package site.rahoon.message.__monolitic.chatroom.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA Repository
 */
interface ChatRoomJpaRepository : JpaRepository<ChatRoomEntity, String> {
    fun findByCreatedByUserId(userId: String): List<ChatRoomEntity>
}
