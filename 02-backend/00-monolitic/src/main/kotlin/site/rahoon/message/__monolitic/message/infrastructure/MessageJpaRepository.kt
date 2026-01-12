package site.rahoon.message.__monolitic.message.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageJpaRepository : JpaRepository<MessageEntity, String> {
    fun findByChatRoomIdOrderByCreatedAtDesc(chatRoomId: String): List<MessageEntity>
}
