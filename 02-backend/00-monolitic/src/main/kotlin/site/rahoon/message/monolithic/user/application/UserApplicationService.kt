package site.rahoon.message.monolithic.user.application

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.user.application.component.UserPasswordCreator
import site.rahoon.message.monolithic.user.domain.UserDomainService
import site.rahoon.message.monolithic.user.domain.UserInfo
import site.rahoon.message.monolithic.user.domain.UserRole
import site.rahoon.message.monolithic.user.domain.component.UserPasswordHasher

/**
 * User Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class UserApplicationService(
    private val userDomainService: UserDomainService,
    private val passwordHasher: UserPasswordHasher,
    private val userPasswordCreator: UserPasswordCreator,
) {
    private val logger = LoggerFactory.getLogger(UserApplicationService::class.java)

    /**
     * 회원가입
     */
    fun register(criteria: UserCriteria.Register): UserInfo.Detail {
        val command = criteria.toCommand()
        return userDomainService.create(command)
    }

    /**
     * 현재 로그인한 사용자 정보를 조회합니다.
     */
    fun getCurrentUser(userId: String): UserInfo.Detail = userDomainService.getById(userId)

    /**
     * 사용자 역할을 업데이트합니다.
     */
    fun updateRole(
        userId: String,
        role: UserRole,
    ): UserInfo.Detail = userDomainService.updateRole(userId, role)

    fun createOrUpdateAdmin(criteria: UserCriteria.CreateOrUpdateAdmin) {
        val existingUser = userDomainService.findUserByEmail(criteria.email)

        if (existingUser != null) {
            val (passwordToUse, wasRandom) = userPasswordCreator.resolve(criteria.password)
            userDomainService.updatePasswordAndRole(
                userId = existingUser.id,
                passwordHash = passwordHasher.hash(passwordToUse),
                role = UserRole.ADMIN,
            )
            val passwordLog = if (wasRandom) "password=$passwordToUse (printed only once)" else "password=(configured)"
            logger.info("Default admin password reset complete (create-update): email=${criteria.email}, $passwordLog")
            return
        }

        createAdmin(criteria.email, criteria.password, criteria.nickname)
    }

    fun createAdminIfNotExists(criteria: UserCriteria.CreateAdminIfNotExists) {
        val existingUser = userDomainService.findUserByEmail(criteria.email)

        if (existingUser != null) {
            logger.debug("Default admin email (${criteria.email}) already in use - skipping default admin creation")
            return
        }

        if (userDomainService.existsByRole(UserRole.ADMIN)) {
            logger.debug("ADMIN account already exists - skipping default admin creation")
            return
        }

        createAdmin(criteria.email, criteria.password, criteria.nickname)
    }

    private fun createAdmin(
        email: String,
        password: String,
        nickname: String,
    ) {
        val (passwordToUse, _) = userPasswordCreator.resolve(password)
        val passwordHash = passwordHasher.hash(passwordToUse)
        userDomainService.createAdmin(
            email = email,
            passwordHash = passwordHash,
            nickname = nickname,
        )
        logger.info("Default admin account created: email=$email")
    }
}
