package site.rahoon.message.monolithic.loginfailure.domain

import org.springframework.stereotype.Service
import java.time.Duration

/**
 * 로그인 실패 횟수 추적 도메인 서비스
 * 이메일과 IP 주소 기반으로 실패 횟수를 추적하고 계정 잠금을 관리합니다.
 */
@Service
class LoginFailureTracker(
    private val loginFailureRepository: LoginFailureRepository,
) {
    companion object {
        private val LOCKOUT_DURATION = Duration.ofMinutes(LoginFailure.LOCKOUT_DURATION_MINUTES)
    }

    /**
     * 로그인 실패 횟수를 확인하고 잠금 여부를 검증합니다.
     * @param email 사용자 이메일
     * @param ipAddress IP 주소
     * @throws DomainException 실패 횟수가 최대치에 도달한 경우
     */
    fun checkAndThrowIfLocked(
        email: String,
        ipAddress: String,
    ) {
        val failures = loginFailureRepository.findByKeys(listOf(email, ipAddress))
        failures.forEach { it.checkLocked() }
    }

    /**
     * 로그인 실패 횟수를 증가시킵니다.
     * 증가 후 잠금 여부를 확인하여 즉시 잠금을 적용합니다.
     * @param email 사용자 이메일
     * @param ipAddress IP 주소
     * @throws DomainException 실패 횟수가 최대치에 도달한 경우
     */
    fun incrementFailureCount(
        email: String,
        ipAddress: String,
    ) {
//        val emailCount = loginFailureRepository.incrementAndGet(email, LOCKOUT_DURATION)
//        val ipCount = loginFailureRepository.incrementAndGet(ipAddress, LOCKOUT_DURATION)
        val loginFailures =
            loginFailureRepository.incrementAndGetMultiple(
                listOf(
                    email to LOCKOUT_DURATION,
                    ipAddress to LOCKOUT_DURATION,
                ),
            )
        loginFailures.forEach { it.checkLocked() }
    }

    /**
     * 로그인 성공 시 실패 횟수를 초기화합니다.
     * @param email 사용자 이메일
     * @param ipAddress IP 주소
     */
    fun resetFailureCount(
        email: String,
        ipAddress: String,
    ) {
        loginFailureRepository.deleteByKey(email)
        loginFailureRepository.deleteByKey(ipAddress)
    }

    /**
     * 로그인 실패 횟수를 원자적으로 증가시키고, 최대치에 도달했으면 예외를 던집니다.
     * limit 미만일 경우 증가시키고, limit 이상이면 예외를 던집니다.
     * 락 없이 Redis atomic operation을 사용하여 성능을 최적화합니다.
     * @param email 사용자 이메일
     * @param ipAddress IP 주소
     * @throws DomainException 실패 횟수가 최대치에 도달한 경우
     */
    fun incrementFailureCountOrThrowIfLocked(
        email: String,
        ipAddress: String,
    ) {
        val (isUpdated, loginFailures) = loginFailureRepository.getAndIncrement(
            listOf(
                Triple(email, LoginFailure.MAX_FAILURE_COUNT, LOCKOUT_DURATION),
                Triple(ipAddress, LoginFailure.MAX_FAILURE_COUNT, LOCKOUT_DURATION),
            ),
        )
        if (!isUpdated) {
            loginFailures.forEach { it.checkLocked() }
        }
    }
}
