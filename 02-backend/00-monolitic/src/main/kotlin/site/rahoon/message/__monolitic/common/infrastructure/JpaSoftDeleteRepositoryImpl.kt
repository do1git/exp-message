package site.rahoon.message.__monolitic.common.infrastructure

import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.domain.SoftDeleteContext
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

/**
 * Soft Delete 기능을 제공하는 JpaRepository 인터페이스
 * 
 * 이 인터페이스를 상속받는 JpaRepository는 `softDeleteById()` 메서드를 자동으로 제공받습니다.
 * `#{#entityName}` SpEL을 사용하여 엔티티 이름을 자동으로 추출합니다.
 */
class JpaSoftDeleteRepositoryImpl<T , ID : Any>(
    private val entityInformation: JpaEntityInformation<T, ID>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, ID>(entityInformation, entityManager), JpaSoftDeleteRepository<T, ID> {


    override fun findById(id: ID): Optional<T> {
        return super.findById(id).orElse(null)
            ?.takeIf {
                if(!(it is JpaEntityBase)) true
                else SoftDeleteContext.isDisabled() || it.deletedAt == null
            }.let { Optional.ofNullable(it) as Optional<T> }
    }

    override fun findByIdOrNull(id: ID): T? {
        return findById(id).orElse(null)
    }

    @Transactional
    override fun softDeleteById(id: ID, deletedAt: LocalDateTime): Int{
        val entityName = domainClass.simpleName
        return entityManager.createQuery(
            "UPDATE $entityName e SET e.deletedAt = :now WHERE e.id = :id"
        )
            .setParameter("now", LocalDateTime.now())
            .setParameter("id", id)
            .executeUpdate()
    }
}
