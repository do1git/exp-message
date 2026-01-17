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
        val LOCKOUT_DURATION_MINUTES = 15L

        /**
         * 새로운 LoginFailure를 생성합니다 (실패 횟수 0)
         */
        fun create(key: String): LoginFailure =
            LoginFailure(
                key = key,
                failureCount = 0,
            )

        /**
         * 저장소에서 조회한 실패 횟수로 LoginFailure를 생성합니다.
         */
        fun from(
            key: String,
            failureCount: Int,
        ): LoginFailure =
            LoginFailure(
                key = key,
                failureCount = failureCount,
            )
    }

    /**
     * 잠금 여부를 확인합니다.
     */
    fun isLocked(): Boolean = failureCount >= MAX_FAILURE_COUNT

    /**
     * 잠금 여부를 확인하고 잠금된 경우 예외를 발생시킵니다.
     * @throws DomainException 잠금된 경우
     */
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

    /**
     * 실패 횟수를 증가시킵니다.
     */
    fun increment(): LoginFailure =
        this.copy(
            failureCount = failureCount + 1,
        )

    /**
     * 실패 횟수를 초기화합니다.
     */
    fun reset(): LoginFailure =
        this.copy(
            failureCount = 0,
        )
}
