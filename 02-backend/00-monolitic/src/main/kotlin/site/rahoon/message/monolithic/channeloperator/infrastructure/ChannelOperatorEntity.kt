package site.rahoon.message.monolithic.channeloperator.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import site.rahoon.message.monolithic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * ChannelOperator 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(
    name = "channel_operators",
    indexes = [
        Index(name = "idx_channel_id", columnList = "channel_id"),
        Index(name = "idx_user_id", columnList = "user_id"),
        Index(name = "idx_channel_user", columnList = "channel_id,user_id", unique = true),
    ],
)
class ChannelOperatorEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,
    @Column(name = "channel_id", nullable = false, length = 36)
    var channelId: String,
    @Column(name = "user_id", nullable = false, length = 36)
    var userId: String,
    @Column(name = "nickname", nullable = false, length = 50)
    var nickname: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) : JpaEntityBase() {
    constructor() : this("", "", "", "", LocalDateTime.now(), LocalDateTime.now())
}
