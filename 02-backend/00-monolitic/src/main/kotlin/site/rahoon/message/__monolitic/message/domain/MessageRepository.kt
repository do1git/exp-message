package site.rahoon.message.__monolitic.message.domain

import java.time.LocalDateTime

interface MessageRepository {
    fun save(message: Message): Message
    fun findById(id: String): Message?
    /**
     * 채팅방별 메시지 목록을 조회합니다. (Keyset pagination)
     *
     * - afterCreatedAt/afterId가 null이면 첫 페이지
     * - 값이 있으면 해당 커서(앵커) "이후" 데이터를 조회
     */
    fun findPageByChatRoomId(
        chatRoomId: String,
        afterCreatedAt: LocalDateTime?,
        afterId: String?,
        limit: Int
    ): List<Message>
}
