package site.rahoon.message.monolithic.message.application

import site.rahoon.message.monolithic.message.domain.Message
import java.time.LocalDateTime

/**
 * 메시지 이벤트 DTO
 *
 * Redis Pub/Sub을 통해 Pod 간 전파되는 이벤트
 * 도메인 객체를 포함하지 않고 순수 데이터만 전달
 */
sealed interface MessageEvent {
    /**
     * 메시지 생성 이벤트
     */
    data class Created(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val content: String,
        val createdAt: LocalDateTime,
    ) : MessageEvent {
        companion object {
            fun from(message: Message): Created =
                Created(
                    id = message.id,
                    chatRoomId = message.chatRoomId,
                    userId = message.userId,
                    content = message.content,
                    createdAt = message.createdAt,
                )
        }
    }
}
