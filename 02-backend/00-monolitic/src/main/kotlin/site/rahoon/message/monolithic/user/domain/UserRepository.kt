package site.rahoon.message.monolithic.user.domain

interface UserRepository {
    fun save(user: User): User

    fun findById(id: String): User?

    fun findByEmail(email: String): User?

    fun existsByRole(role: UserRole): Boolean

    fun delete(id: String)
}
