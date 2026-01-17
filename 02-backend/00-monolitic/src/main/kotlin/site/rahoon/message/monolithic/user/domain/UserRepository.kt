package site.rahoon.message.monolithic.user.domain

interface UserRepository {
    fun save(user: User): User

    fun findById(id: String): User?

    fun findByEmail(email: String): User?

    fun delete(id: String)
}
