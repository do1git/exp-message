package site.rahoon.message.monolithic.loginfailure.domain

import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * 로그인 실패 도메인 모델
 * 로그인 실패 횟수와 잠금 상태를 관리하는 값 객체
 */
data class LoginFailure(
    val key: String, // 이메일 또는 IP 주소
    val failureCount: Int,
) {
    companion object {
        private const val MAX_FAILURE_COUNT = 5
        const val LOCKOUT_DURATION_MINUTES = 15L

        fun create(key: String): LoginFailure = LoginFailure(key = key, failureCount = 0)

        fun from(
            key: String,
            failureCount: Int,
        ): LoginFailure = LoginFailure(key = key, failureCount = failureCount)
    }

    fun isLocked(): Boolean = failureCount >= MAX_FAILURE_COUNT

    fun checkLocked() {
        if (isLocked()) {
            throw DomainException(
                error = LoginFailureError.ACCOUNT_LOCKED,
                details =
                    mapOf(
                        "key" to key,
                        "lockoutDuration" to LOCKOUT_DURATION_MINUTES,
                    ),
            )
        }
    }

    fun increment(): LoginFailure = this.copy(failureCount = failureCount + 1)

    fun reset(): LoginFailure = this.copy(failureCount = 0)
}
