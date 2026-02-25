package site.rahoon.message.monolithic.user.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.user.domain.component.UserCreateValidator
import site.rahoon.message.monolithic.user.domain.component.UserPasswordHasher
import site.rahoon.message.monolithic.user.domain.component.UserUpdateValidator

@Service
@Transactional(readOnly = true)
class UserDomainService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val userCreateValidator: UserCreateValidator,
    private val userUpdateValidator: UserUpdateValidator,
) {
    fun getUser(
        email: String,
        password: String,
    ): UserInfo.Detail {
        val user =
            userRepository.findByEmail(email)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("email" to email),
                )
        val match = passwordHasher.verify(password, user.passwordHash)
        if (!match) {
            throw DomainException(
                error = UserError.USER_NOT_FOUND,
                details = mapOf("email" to email),
            )
        }
        return UserInfo.Detail.from(user)
    }

    @Transactional
    fun create(command: UserCommand.Create): UserInfo.Detail {
        // 입력값 검증
        userCreateValidator.validate(command)

        // 이메일 중복 체크 (Repository 접근이 필요하므로 Domain Service에서 처리)
        userRepository.findByEmail(command.email)?.let {
            throw DomainException(
                error = UserError.EMAIL_ALREADY_EXISTS,
                details = mapOf("email" to command.email),
            )
        }

        // 비밀번호 해싱 (인프라 컴포넌트 사용)
        val passwordHash = passwordHasher.hash(command.password)

        // User 생성 로직은 도메인 객체에서 처리
        val user =
            User.create(
                email = command.email,
                passwordHash = passwordHash,
                nickname = command.nickname,
            )

        val savedUser = userRepository.save(user)
        return UserInfo.Detail.from(savedUser)
    }

    @Transactional
    fun update(command: UserCommand.Update): UserInfo.Detail {
        // 입력값 검증
        userUpdateValidator.validate(command)

        val user =
            userRepository.findById(command.id)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("userId" to command.id),
                )

        // 업데이트 로직은 도메인 객체에서 처리
        val updatedUser = user.updateNickname(command.nickname)

        val savedUser = userRepository.save(updatedUser)
        return UserInfo.Detail.from(savedUser)
    }

    @Transactional
    fun delete(command: UserCommand.Delete): UserInfo.Detail {
        val user =
            userRepository.findById(command.id)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("userId" to command.id),
                )
        userRepository.delete(command.id)
        return UserInfo.Detail.from(user)
    }

    /**
     * ID로 사용자 정보를 조회합니다.
     */
    fun getById(userId: String): UserInfo.Detail {
        val user =
            userRepository.findById(userId)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("userId" to userId),
                )
        return UserInfo.Detail.from(user)
    }

    /**
     * 이메일로 사용자를 조회합니다. (없으면 null)
     */
    fun findUserByEmail(email: String): User? = userRepository.findByEmail(email)

    /**
     * 해당 역할의 사용자가 존재하는지 확인합니다.
     */
    fun existsByRole(role: UserRole): Boolean = userRepository.existsByRole(role)

    @Transactional
    fun createAdmin(
        email: String,
        passwordHash: String,
        nickname: String,
    ): User {
        userRepository.findByEmail(email)?.let {
            throw DomainException(
                error = UserError.EMAIL_ALREADY_EXISTS,
                details = mapOf("email" to email),
            )
        }
        val user =
            User.create(
                email = email,
                passwordHash = passwordHash,
                nickname = nickname,
                role = UserRole.ADMIN,
            )
        return userRepository.save(user)
    }

    @Transactional
    fun updatePasswordAndRole(
        userId: String,
        passwordHash: String,
        role: UserRole,
    ) {
        val user =
            userRepository.findById(userId)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("userId" to userId),
                )
        val updatedUser = user.updatePassword(passwordHash).updateRole(role)
        userRepository.save(updatedUser)
    }

    @Transactional
    fun updateRole(
        userId: String,
        role: UserRole,
    ): UserInfo.Detail {
        val user =
            userRepository.findById(userId)
                ?: throw DomainException(
                    error = UserError.USER_NOT_FOUND,
                    details = mapOf("userId" to userId),
                )
        val updatedUser = user.updateRole(role)
        val savedUser = userRepository.save(updatedUser)
        return UserInfo.Detail.from(savedUser)
    }
}
