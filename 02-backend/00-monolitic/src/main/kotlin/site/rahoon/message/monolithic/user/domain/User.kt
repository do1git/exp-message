package site.rahoon.message.monolithic.user.domain

import java.time.LocalDateTime
import java.util.UUID

data class User(
    val id: String,
    val email: String,
    val passwordHash: String,
    val nickname: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * 새로운 User를 생성합니다.
         */
        fun create(
            email: String,
            passwordHash: String,
            nickname: String,
        ): User {
            val now = LocalDateTime.now()
            return User(
                id = UUID.randomUUID().toString(),
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    /**
     * 닉네임을 업데이트합니다.
     */
    fun updateNickname(newNickname: String): User =
        this.copy(
            nickname = newNickname,
            updatedAt = LocalDateTime.now(),
        )
}
