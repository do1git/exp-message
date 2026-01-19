package site.rahoon.message.monolithic.user.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.user.domain.UserCommand
import site.rahoon.message.monolithic.user.domain.UserError

/**
 * User 수정 시 입력값 검증을 위한 인터페이스
 */
interface UserUpdateValidator {
    /**
     * UserCommand.Update를 검증합니다.
     * 검증 실패 시 DomainException을 발생시킵니다.
     *
     * @param command 검증할 명령 객체
     * @throws DomainException 검증 실패 시
     */
    fun validate(command: UserCommand.Update)
}

/**
 * User 수정 검증 구현체
 */
@Component
class UserUpdateValidatorImpl : UserUpdateValidator {
    companion object {
        private const val NICKNAME_MIN_LENGTH = 2
        private const val NICKNAME_MAX_LENGTH = 20
    }

    override fun validate(command: UserCommand.Update) {
        validateId(command.id)
        validateNickname(command.nickname)
    }

    private fun validateId(id: String) {
        if (id.isBlank()) {
            throw DomainException(
                error = UserError.USER_NOT_FOUND,
                details = mapOf("userId" to id, "reason" to "사용자 ID는 필수입니다"),
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

        if (nickname.length !in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH) {
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
