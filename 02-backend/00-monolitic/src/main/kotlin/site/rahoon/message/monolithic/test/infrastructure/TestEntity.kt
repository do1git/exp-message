package site.rahoon.message.monolithic.test.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import site.rahoon.message.monolithic.common.infrastructure.JpaEntityBase
import java.time.LocalDateTime

/**
 * Soft Delete 기능 테스트를 위한 테스트 Entity
 * JpaEntityBase를 상속하여 자동으로 Soft Delete 필터가 적용됩니다.
 * (JpaEntityBase에 @Where 어노테이션이 있어서 자동 적용됨)
 */
@Entity
@Table(
    name = "test_entities",
    indexes = [
        Index(name = "idx_test_name", columnList = "name"),
        Index(name = "idx_test_deleted_at", columnList = "deleted_at"),
    ],
)
class TestEntity(
    @Id
    @Column(name = "id", length = 36)
    override var id: String,
    @Column(name = "name", nullable = false, length = 100)
    var name: String,
    @Column(name = "description", nullable = true, length = 255)
    var description: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime,
) : JpaEntityBase() {
    constructor() : this("", "", null, LocalDateTime.now())
}
