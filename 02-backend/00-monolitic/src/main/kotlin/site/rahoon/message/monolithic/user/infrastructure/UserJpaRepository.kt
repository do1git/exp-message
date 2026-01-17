package site.rahoon.message.monolithic.user.infrastructure

import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepository
import java.util.Optional

/**
 * Spring Data JPA Repository
 */
interface UserJpaRepository : JpaSoftDeleteRepository<UserEntity, String> {
    fun findByEmail(email: String): Optional<UserEntity>
}
