package site.rahoon.message.__monolitic.chatroom.infrastructure

import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
interface ChatRoomJpaRepository : JpaSoftDeleteRepository<ChatRoomEntity, String> {
    fun findByCreatedByUserId(userId: String): List<ChatRoomEntity>
}
