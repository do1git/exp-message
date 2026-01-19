package site.rahoon.message.monolithic.user.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.user.domain.UserCommand
import site.rahoon.message.monolithic.user.domain.UserError

/**
 * User 생성 시 입력값 검증을 위한 인터페이스
 */
interface UserCreateValidator {
    /**
     * UserCommand.Create를 검증합니다.
     * 검증 실패 시 DomainException을 발생시킵니다.
     *
     * @param command 검증할 명령 객체
     * @throws DomainException 검증 실패 시
     */
    fun validate(command: UserCommand.Create)
}

/**
 * User 생성 검증 구현체
 */
@Component
class UserCreateValidatorImpl : UserCreateValidator {
    override fun validate(command: UserCommand.Create) {
        validateEmail(command.email)
        validatePassword(command.password)
        validateNickname(command.nickname)
    }

    private fun validateEmail(email: String) {
        if (email.isBlank()) {
            throw DomainException(
                error = UserError.INVALID_EMAIL,
                details = mapOf("email" to email, "reason" to "이메일은 필수입니다"),
            )
        }

        // 간단한 이메일 형식 검증
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!emailRegex.matches(email)) {
            throw DomainException(
                error = UserError.INVALID_EMAIL,
                details = mapOf("email" to email, "reason" to "올바른 이메일 형식이 아닙니다"),
            )
        }
    }

    private fun validatePassword(password: String) {
        if (password.isBlank()) {
            throw DomainException(
                error = UserError.INVALID_PASSWORD,
                details = mapOf("reason" to "비밀번호는 필수입니다"),
            )
        }

        @Suppress("MagicNumber")
        if (password.length !in 8..20) {
            throw DomainException(
                error = UserError.INVALID_PASSWORD,
                details = mapOf("reason" to "비밀번호는 최소 8자 이상이어야 합니다"),
            )
        }
    }

    private fun validateNickname(nickname: String) {
        if (nickname.isBlank()) {
            throw DomainException(
                error = UserError.INVALID_NICKNAME,
                details = mapOf("nickname" to nickname, "reason" to "닉네임은 필수입니다"),
            )
        }

        @Suppress("MagicNumber")
        if (nickname.length !in 2..20) {
            throw DomainException(
                error = UserError.INVALID_NICKNAME,
                details =
                    mapOf(
                        "nickname" to nickname,
                        "reason" to "닉네임은 2자 이상 20자 이하여야 합니다",
                    ),
            )
        }
    }
}
