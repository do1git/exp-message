package site.rahoon.message.monolithic.user.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.common.global.TransactionalRepository
import site.rahoon.message.monolithic.user.domain.User
import site.rahoon.message.monolithic.user.domain.UserRepository
import java.time.LocalDateTime

/**
 * UserRepository 인터페이스의 JPA 구현체
 */
@TransactionalRepository
class UserRepositoryImpl(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    @Transactional
    override fun save(user: User): User {
        val entity = toEntity(user)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): User? =
        jpaRepository
            .findById(id)
            .map { toDomain(it) }
            .orElse(null)

    override fun findByEmail(email: String): User? =
        jpaRepository
            .findByEmail(email)
            .map { toDomain(it) }
            .orElse(null)

    @Transactional
    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    private fun toEntity(user: User): UserEntity =
        UserEntity(
            id = user.id,
            email = user.email,
            passwordHash = user.passwordHash,
            nickname = user.nickname,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )

    private fun toDomain(entity: UserEntity): User =
        User(
            id = entity.id,
            email = entity.email,
            passwordHash = entity.passwordHash,
            nickname = entity.nickname,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
