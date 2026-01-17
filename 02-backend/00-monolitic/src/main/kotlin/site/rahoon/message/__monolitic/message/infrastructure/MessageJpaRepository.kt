package site.rahoon.message.__monolitic.message.infrastructure

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository
import java.time.LocalDateTime

@Repository
interface MessageJpaRepository : JpaSoftDeleteRepository<MessageEntity, String> {
    fun findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoomId: String, pageable: Pageable): List<MessageEntity>

    @Query(
        """
        select m
        from MessageEntity m
        where m.chatRoomId = :chatRoomId
          and (
            m.createdAt < :createdAt
            or (m.createdAt = :createdAt and m.id < :id)
          )
        order by m.createdAt desc, m.id desc
        """
    )
    fun findNextPageByChatRoomId(
        @Param("chatRoomId") chatRoomId: String,
        @Param("createdAt") createdAt: LocalDateTime,
        @Param("id") id: String,
        pageable: Pageable
    ): List<MessageEntity>
}
