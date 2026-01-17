package site.rahoon.message.__monolitic.user.infrastructure

import jakarta.persistence.*
import site.rahoon.message.__monolitic.common.infrastructure.JpaEntityBase
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

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime
):JpaEntityBase() {
    constructor() : this("", "", "", "", LocalDateTime.now(), LocalDateTime.now())
}

