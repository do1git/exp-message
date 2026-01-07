package site.rahoon.message.__monolitic.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

/**
 * Spring Data JPA Repository
 */
interface UserJpaRepository : JpaRepository<UserEntity, String> {
    fun findByEmail(email: String): Optional<UserEntity>
}

