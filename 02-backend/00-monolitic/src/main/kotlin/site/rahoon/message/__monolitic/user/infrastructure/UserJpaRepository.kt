package site.rahoon.message.__monolitic.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository
import java.util.Optional

/**
 * Spring Data JPA Repository
 */
interface UserJpaRepository : JpaSoftDeleteRepository<UserEntity, String> {
    fun findByEmail(email: String): Optional<UserEntity>
}

