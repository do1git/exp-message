package site.rahoon.message.__monolitic.common.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional

@NoRepositoryBean
interface JpaSoftDeleteRepository<T, ID : Any> : JpaRepository<T, ID> {
    override fun findById(id:ID):Optional<T>
    fun findByIdOrNull(id:ID): T?
    fun softDeleteById(id: ID, deletedAt: LocalDateTime): Int
}
