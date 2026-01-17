package site.rahoon.message.__monolitic.message.infrastructure

import jakarta.persistence.*
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * Message 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_chat_room_id", columnList = "chat_room_id"),
        Index(name = "idx_chat_room_created_at", columnList = "chat_room_id,created_at"),
        Index(name = "idx_user_id", columnList = "user_id")
    ]
)
class MessageEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,

    @Column(name = "chat_room_id", nullable = false, length = 36)
    var chatRoomId: String,

    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String,

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime
):JpaEntityBase() {
    constructor() : this("", "", "", "", LocalDateTime.now())
}
