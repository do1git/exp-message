package site.rahoon.message.monolithic.channelconversation.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import site.rahoon.message.monolithic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * ChannelConversation 도메인 객체를 위한 JPA Entity
 * id는 ChatRoom id와 1:1 관계를 가집니다.
 */
@Entity
@Table(
    name = "channel_conversations",
    indexes = [
        Index(name = "idx_channel_id", columnList = "channel_id"),
        Index(name = "idx_customer_id", columnList = "customer_id"),
    ],
)
class ChannelConversationEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "customer_id", nullable = false, length = 36)
    var customerId: String,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) : JpaEntityBase() {
    constructor() : this("", "", "", "", LocalDateTime.now(), LocalDateTime.now())
}
