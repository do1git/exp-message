package site.rahoon.message.__monolitic.authtoken.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.authtoken.domain.AccessToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenDomainService
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.Lock
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureError
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureTracker
import site.rahoon.message.__monolitic.user.domain.UserDomainService

/**
 * AuthToken Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class AuthTokenApplicationService(
    private val userDomainService: UserDomainService,
    private val authTokenDomainService: AuthTokenDomainService,
    private val loginFailureTracker: LoginFailureTracker
){

    // Check
    fun checkAccessToken( accessToken: String ): AccessToken {
        return authTokenDomainService.verifyAccessToken(accessToken)
    }

    // Login
    fun login( email: String, password: String, ipAddress: String ): AuthToken {
        // 실패 횟수 확인 및 잠금 여부 검증
        loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)
        
        val user = try {
            userDomainService.getUser( email, password )
        } catch (e: DomainException) {
            // 실패 횟수 증가 (증가 후 잠금 확인 포함)
            loginFailureTracker.incrementFailureCount(email, ipAddress)
            throw e
        }
        
        // 성공 시에도 잠금 확인 (다른 스레드가 실패하여 잠금되었을 수 있음)
        loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)
        loginFailureTracker.resetFailureCount(email, ipAddress)
        return authTokenDomainService.issueToken(user.id)
    }
    
    fun loginWithLock( email: String, password: String, ipAddress: String ): AuthToken {
        // 1. 잠금 상태 확인 (락 획득 전)
        loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)

        // 2-7. 락 획득 -> 처리 -> 락 해제
        return Lock.execute(listOf("login:$email", "login:$ipAddress")) { locked, _ ->
            if(!locked) throw DomainException(LoginFailureError.ACCOUNT_LOCKED, mapOf())

            // 3. 락 획득 후 재확인
            loginFailureTracker.checkAndThrowIfLocked(email, ipAddress)

            val user = try {
                // 4. 사용자 조회
                userDomainService.getUser(email, password)
            } catch (e: DomainException) {
                // 5. 실패 시 실패 카운트 증가
                loginFailureTracker.incrementFailureCount(email, ipAddress)
                throw e
            }

            // 6. 성공 시 실패 카운트 초기화
            loginFailureTracker.resetFailureCount(email, ipAddress)
            authTokenDomainService.issueToken(user.id)
        }
    }

    // Refresh
    fun refresh( refreshToken: String ): AuthToken {
        return authTokenDomainService.refresh(refreshToken)
    }

    // Logout
    fun logout( sessionId: String ){
        authTokenDomainService.expireBySessionId(sessionId)
    }
}

