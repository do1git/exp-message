package site.rahoon.message.monolithic.common.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.time.LocalDateTime
import java.util.Optional

@NoRepositoryBean
interface JpaSoftDeleteRepository<T, ID : Any> : JpaRepository<T, ID> {
    override fun findById(id: ID): Optional<T>

    fun findByIdOrNull(id: ID): T?

    fun softDeleteById(
        id: ID,
        deletedAt: LocalDateTime,
    ): Int
}
