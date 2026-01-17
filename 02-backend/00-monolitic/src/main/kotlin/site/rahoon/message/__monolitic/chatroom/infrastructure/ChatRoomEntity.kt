package site.rahoon.message.__monolitic.chatroom.infrastructure

import jakarta.persistence.*
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * ChatRoom 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(
    name = "chat_rooms",
    indexes = [
        Index(name = "idx_created_by_user_id", columnList = "created_by_user_id")
    ]
)
class ChatRoomEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "created_by_user_id", nullable = false, length = 36)
    var createdByUserId: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime
):JpaEntityBase() {
    constructor() : this("", "", "", LocalDateTime.now(), LocalDateTime.now())
}
