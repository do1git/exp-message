package site.rahoon.message.__monolitic.common.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.time.LocalDateTime

/**
 * 공통 Entity 기능을 위한 베이스 클래스
 * Soft Delete 등 공통 기능을 제공합니다.
 * 
 * @MappedSuperclass를 사용하여 공통 필드를 상속받을 수 있습니다.
 * 
 * 이 클래스를 상속하는 모든 Entity는 JpaSoftDeleteFilterMetadataContributor에 의해
 * 자동으로 Soft Delete 필터가 적용됩니다.
 * Entity별로 어노테이션을 추가할 필요가 없습니다.
 */
@MappedSuperclass
abstract class JpaEntityBase {

    @Id
    @Column(name = "id", length = 36)
    open lateinit var id: String
    /**
     * 삭제 시각 (Soft Delete)
     * null이면 삭제되지 않은 상태입니다.
     */
    @Column(name = "deleted_at", nullable = true)
    open var deletedAt: LocalDateTime? = null
}
