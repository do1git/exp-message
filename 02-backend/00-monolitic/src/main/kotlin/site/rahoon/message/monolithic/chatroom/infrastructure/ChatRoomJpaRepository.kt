package site.rahoon.message.monolithic.chatroom.infrastructure

import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository

/**
 * Spring Data JPA Repository
 */
interface ChatRoomJpaRepository : JpaSoftDeleteRepository<ChatRoomEntity, String> {
    fun findByCreatedByUserId(userId: String): List<ChatRoomEntity>
}
