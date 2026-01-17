package site.rahoon.message.monolithic.user.domain.component

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

/**
 * 비밀번호 해싱을 위한 인터페이스
 */
interface UserPasswordHasher {
    /**
     * 원본 비밀번호를 해시로 변환합니다.
     *
     * @param rawPassword 원본 비밀번호
     * @return 해시된 비밀번호
     */
    fun hash(rawPassword: String): String

    /**
     * 원본 비밀번호와 해시된 비밀번호를 비교합니다.
     *
     * @param rawPassword 원본 비밀번호
     * @param hashedPassword 해시된 비밀번호
     * @return 일치 여부
     */
    fun verify(
        rawPassword: String,
        hashedPassword: String,
    ): Boolean
}

/**
 * BCrypt를 사용한 비밀번호 해싱 구현체
 */
@Component
class BCryptUserPasswordHasher : UserPasswordHasher {
    private val encoder = BCryptPasswordEncoder()

    override fun hash(rawPassword: String): String = encoder.encode(rawPassword)

    override fun verify(
        rawPassword: String,
        hashedPassword: String,
    ): Boolean = encoder.matches(rawPassword, hashedPassword)
}
