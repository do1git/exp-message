package site.rahoon.message.__monolitic.user.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.utils.Tx
import site.rahoon.message.__monolitic.user.domain.component.UserCreateValidator
import site.rahoon.message.__monolitic.user.domain.component.UserPasswordHasher
import site.rahoon.message.__monolitic.user.domain.component.UserUpdateValidator

@Service
@Transactional(readOnly = true)
class UserDomainService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val userCreateValidator: UserCreateValidator,
    private val userUpdateValidator: UserUpdateValidator
) {

    @Transactional
    fun create(command: UserCommand.Create): UserInfo.Detail {
            // 입력값 검증
            userCreateValidator.validate(command)

            // 이메일 중복 체크 (Repository 접근이 필요하므로 Domain Service에서 처리)
            userRepository.findByEmail(command.email)?.let {
                throw DomainException(
                    error = UserError.EMAIL_ALREADY_EXISTS,
                    details = mapOf("email" to command.email)
                )
            }

            // 비밀번호 해싱 (인프라 컴포넌트 사용)
            val passwordHash = passwordHasher.hash(command.password)

            // User 생성 로직은 도메인 객체에서 처리
            val user = User.create(
                email = command.email,
                passwordHash = passwordHash,
                nickname = command.nickname
            )

            val savedUser = userRepository.save(user)
            return UserInfo.Detail.from(savedUser)
    }

    @Transactional
    fun update(command: UserCommand.Update): UserInfo.Detail {
        // 입력값 검증
        userUpdateValidator.validate(command)

        val user = userRepository.findById(command.id)
            ?: throw DomainException(
                error = UserError.USER_NOT_FOUND,
                details = mapOf("userId" to command.id)
            )

        // 업데이트 로직은 도메인 객체에서 처리
        val updatedUser = user.updateNickname(command.nickname)

        val savedUser = userRepository.save(updatedUser)
        return UserInfo.Detail.from(savedUser)
    }

    @Transactional
    fun delete(command: UserCommand.Delete): UserInfo.Detail {
        val user = userRepository.findById(command.id)
            ?: throw DomainException(
                error = UserError.USER_NOT_FOUND,
                details = mapOf("userId" to command.id)
            )
        userRepository.delete(command.id)
        return UserInfo.Detail.from(user)
    }
}

