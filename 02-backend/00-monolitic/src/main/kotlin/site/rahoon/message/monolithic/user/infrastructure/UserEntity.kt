package site.rahoon.message.monolithic.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import site.rahoon.message.monolithic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * User 도메인 객체를 위한 JPA Entity
 */
@Entity
@Table(name = "users", indexes = [Index(name = "idx_email", columnList = "email", unique = true)])
class UserEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,
    @Column(name = "email", nullable = false, unique = true, length = 255)
    var email: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,
    @Column(name = "nickname", nullable = false, length = 100)
    var nickname: String,
    @Column(name = "role", nullable = false, length = 20)
    var role: String,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) : JpaEntityBase() {
    constructor() : this("", "", "", "", "USER", LocalDateTime.now(), LocalDateTime.now())
}
