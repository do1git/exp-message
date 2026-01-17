package site.rahoon.message.__monolitic.chatroommember.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository
import java.time.LocalDateTime

/**
 * Spring Data JPA Repository
 */
interface ChatRoomMemberJpaRepository : JpaSoftDeleteRepository<ChatRoomMemberEntity, String> {
    fun findByChatRoomIdAndUserId(chatRoomId: String, userId: String): ChatRoomMemberEntity?
    fun findByChatRoomId(chatRoomId: String): List<ChatRoomMemberEntity>
    fun findByUserId(userId: String): List<ChatRoomMemberEntity>

    @Modifying
    @Query("UPDATE ChatRoomMemberEntity SET deletedAt=?3 where chatRoomId = ?1 and userId = ?2")
    fun softDeleteByChatRoomIdAndUserId(chatRoomId: String, userId: String, deletedAt: LocalDateTime)
}
