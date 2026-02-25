package site.rahoon.message.monolithic.authtoken.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.authtoken.domain.AccessToken
import site.rahoon.message.monolithic.authtoken.domain.AuthToken
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenDomainService
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenError
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.global.Lock
import site.rahoon.message.monolithic.loginfailure.domain.LoginFailureError
import site.rahoon.message.monolithic.loginfailure.domain.LoginFailureTracker
import site.rahoon.message.monolithic.user.domain.UserDomainService

/**
 * AuthToken Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class AuthTokenApplicationService(
    private val userDomainService: UserDomainService,
    private val authTokenDomainService: AuthTokenDomainService,
    private val loginFailureTracker: LoginFailureTracker,
) {
    // Check
    fun checkAccessToken(accessToken: String): AccessToken = authTokenDomainService.verifyAccessToken(accessToken)

    // Login
    fun login(
        email: String,
        password: String,
        ipAddress: String,
    ): AuthToken {
        // 실패 횟수 증가 (원자적으로 처리, limit 이상이면 예외 발생)
        loginFailureTracker.incrementFailureCountOrThrowIfLocked(email, ipAddress)
        val user = userDomainService.getUser(email, password)
        // 성공 시 실패 카운트 초기화
        loginFailureTracker.resetFailureCount(email, ipAddress)
        // 토큰 발급
        return authTokenDomainService.issueToken(user.id, user.role.code)
    }

    fun loginWithLock(
        email: String,
        password: String,
        ipAddress: String,
    ): AuthToken {
        // 1. 잠금 상태 확인 (락 획득 전)
        loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)

        // 2-7. 락 획득 -> 처리 -> 락 해제
        return Lock.execute(listOf("login:$email", "login:$ipAddress")) { locked, _ ->
            if (!locked) throw DomainException(LoginFailureError.ACCOUNT_LOCKED, mapOf())

            // 3. 락 획득 후 재확인
            loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)

            val user =
                try {
                    // 4. 사용자 조회
                    userDomainService.getUser(email, password)
                } catch (e: DomainException) {
                    // 5. 실패 시 실패 카운트 증가 (원자적으로 처리, limit 이상이면 예외 발생)
                    loginFailureTracker.incrementFailureCountOrThrowIfLocked(email, ipAddress)
                    throw e
                }

            // 6. 성공 시 실패 카운트 초기화
            loginFailureTracker.resetFailureCount(email, ipAddress)
            authTokenDomainService.issueToken(user.id, user.role.code)
        }
    }

    // Refresh
    fun refresh(refreshTokenString: String): AuthToken =
        Lock.execute(listOf("refresh:$refreshTokenString")) { locked, _ ->
            if (!locked) {
                throw DomainException(
                    error = CommonError.CONFLICT,
                    details = mapOf("reason" to "Refresh already in progress for this token"),
                )
            }
            val refreshToken =
                authTokenDomainService.findRefreshToken(refreshTokenString)
                    ?: throw DomainException(
                        error = AuthTokenError.INVALID_TOKEN,
                        details = mapOf("refreshToken" to refreshTokenString),
                    )
            val user = userDomainService.getById(refreshToken.userId)
            authTokenDomainService.refresh(refreshTokenString, user.role.code)
        }

    // Logout
    fun logout(sessionId: String) {
        authTokenDomainService.expireBySessionId(sessionId)
    }
}
