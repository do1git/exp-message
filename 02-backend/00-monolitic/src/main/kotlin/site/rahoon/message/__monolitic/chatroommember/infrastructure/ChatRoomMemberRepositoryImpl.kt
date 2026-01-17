package site.rahoon.message.__monolitic.chatroommember.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMember
import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMemberRepository
import site.rahoon.message.__monolitic.common.global.TransactionalRepository
import java.time.LocalDateTime

/**
 * ChatRoomMemberRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class ChatRoomMemberRepositoryImpl(
    private val jpaRepository: ChatRoomMemberJpaRepository
) : ChatRoomMemberRepository {

    @Transactional
    override fun save(chatRoomMember: ChatRoomMember): ChatRoomMember {
        val entity = toEntity(chatRoomMember)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findByChatRoomIdAndUserId(chatRoomId: String, userId: String): ChatRoomMember? {
        return jpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
            ?.let { toDomain(it) }
    }

    override fun findByChatRoomId(chatRoomId: String): List<ChatRoomMember> {
        return jpaRepository.findByChatRoomId(chatRoomId)
            .map { toDomain(it) }
    }

    override fun findByUserId(userId: String): List<ChatRoomMember> {
        return jpaRepository.findByUserId(userId)
            .map { toDomain(it) }
    }

    @Transactional
    override fun delete(chatRoomId: String, userId: String) {
        jpaRepository.softDeleteByChatRoomIdAndUserId(chatRoomId, userId, LocalDateTime.now())
    }

    private fun toEntity(chatRoomMember: ChatRoomMember): ChatRoomMemberEntity {
        return ChatRoomMemberEntity(
            id = chatRoomMember.id,
            chatRoomId = chatRoomMember.chatRoomId,
            userId = chatRoomMember.userId,
            joinedAt = chatRoomMember.joinedAt
        )
    }

    private fun toDomain(entity: ChatRoomMemberEntity): ChatRoomMember {
        return ChatRoomMember(
            id = entity.id,
            chatRoomId = entity.chatRoomId,
            userId = entity.userId,
            joinedAt = entity.joinedAt
        )
    }
}
