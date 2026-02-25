package site.rahoon.message.monolithic.channel.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import site.rahoon.message.monolithic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * Channel 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(
    name = "channels",
    indexes = [
        Index(name = "idx_api_key", columnList = "api_key", unique = true),
    ],
)
class ChannelEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    var apiKey: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) : JpaEntityBase() {
    constructor() : this("", "", "", LocalDateTime.now(), LocalDateTime.now())
}
