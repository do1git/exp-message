package site.rahoon.message.monolithic.user.batch

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.user.application.UserApplicationService
import site.rahoon.message.monolithic.user.application.UserCriteria

/**
 * Triggers default admin creation/update on application startup.
 */
@Component
@Order(Int.MIN_VALUE)
class DefaultAdminStartupRunner(
    private val userApplicationService: UserApplicationService,
    @Value("\${default-admin.email:admin@example.com}") private val email: String,
    @Value("\${default-admin.password:}") private val password: String,
    @Value("\${default-admin.nickname:admin}") private val nickname: String,
    @Value("\${default-admin.method:default}") private val method: String,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        when {
            method.equals("create-update", ignoreCase = true) ->
                userApplicationService.createOrUpdateAdmin(
                    UserCriteria.CreateOrUpdateAdmin(
                        email = email,
                        password = password,
                        nickname = nickname,
                    ),
                )
            else ->
                userApplicationService.createAdminIfNotExists(
                    UserCriteria.CreateAdminIfNotExists(
                        email = email,
                        password = password,
                        nickname = nickname,
                    ),
                )
        }
    }
}
