package site.rahoon.message.__monolitic.chatroommember.infrastructure

import jakarta.persistence.*
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * ChatRoomMember 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(
    name = "chat_room_members",
    indexes = [
        Index(name = "idx_chat_room_id", columnList = "chat_room_id"),
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_chat_room_user", columnList = "chat_room_id,user_id", unique = true)
    ]
)
class ChatRoomMemberEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,

    @Column(name = "chat_room_id", nullable = false, length = 36)
    var chatRoomId: String,

    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String,

    @Column(name = "joined_at", nullable = false)
    var joinedAt: LocalDateTime
):JpaEntityBase() {
    constructor() : this("", "", "", LocalDateTime.now())
}
