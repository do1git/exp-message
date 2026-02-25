package site.rahoon.message.monolithic.user.application.component

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * Creates user password from configured value or generates random.
 * If configured password is blank, generates random and logs it.
 *
 * @return Pair(password to use, wasRandom)
 */
@Component
class UserPasswordCreator {
    private val logger = LoggerFactory.getLogger(UserPasswordCreator::class.java)

    fun resolve(configuredPassword: String): Pair<String, Boolean> =
        if (configuredPassword.isNotBlank()) {
            configuredPassword to false
        } else {
            val randomPassword = generateRandomPassword()
            logger.info(
                "Default admin password not set - generated randomly. password=$randomPassword (printed only once)",
            )
            randomPassword to true
        }

    private fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..DEFAULT_RANDOM_PASSWORD_LENGTH).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    companion object {
        private const val DEFAULT_RANDOM_PASSWORD_LENGTH = 16
    }
}
