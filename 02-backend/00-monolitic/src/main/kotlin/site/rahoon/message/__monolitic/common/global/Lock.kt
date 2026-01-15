package site.rahoon.message.__monolitic.common.global

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class Lock(
    private val lockRepository: LockRepository
) {

    companion object {
        @Volatile
        lateinit var inst: Lock
            private set

        private val DEFAULT_TTL = Duration.ofSeconds(5)


        /**
         * 단일 키에 대해 락을 획득하고 action을 실행합니다.
         * @return LockResult.Success(결과) 또는 LockResult.Failure
         */
        fun <T> execute(
            key: String,
            ttl: Duration = DEFAULT_TTL,
            lockFailException: Exception? = null,
            action: (locked: Boolean, lockToken: LockToken?) -> T
        ): T = execute(listOf(key), ttl, lockFailException, action)

        /**
         * 여러 키에 대해 락을 획득하고 action을 실행합니다.
         * @return LockResult.Success(결과) 또는 LockResult.Failure
         */
        fun <T> execute(
            keys: List<String>,
            ttl: Duration = DEFAULT_TTL,
            lockFailException: Exception? = null,
            action: (locked:Boolean, lockToken: LockToken?) -> T
        ): T {
            val token = inst.acquireLocks(keys, ttl)
            if(token == null && lockFailException != null) throw lockFailException
            if(token == null) return action(false, null)
            return try {
                action(true, token)
            } finally {
                inst.releaseLock(token)
            }
        }
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    /**
     * 단일 키에 대해 락을 획득합니다.
     * @return LockToken (성공 시), null (실패 시)
     */
    fun acquireLock(key: String, ttl: Duration = DEFAULT_TTL): LockToken? {
        return lockRepository.acquireLock(key, ttl)
    }

    /**
     * 여러 키에 대해 락을 획득합니다.
     * @return LockToken (성공 시), null (실패 시)
     */
    fun acquireLocks(keys: List<String>, ttl: Duration = DEFAULT_TTL): LockToken? {
        return lockRepository.acquireLocks(keys, ttl)
    }

    /** 락을 해제합니다. 토큰에 포함된 정보로 자신의 락만 해제됩니다. */
    fun releaseLock(token: LockToken): Boolean {
        return lockRepository.releaseLock(token)
    }
}

/**
 * 분산 락 토큰
 * 락 획득 시 반환되며, 해제 시 이 토큰을 사용합니다.
 */
data class LockToken(
    val keys: List<String>,
    val lockId: String,
    val expiresAt: Instant
) {
    constructor(key: String, lockId: String, expiresAt: Instant)
            : this(listOf(key), lockId, expiresAt)

    /**
     * 락이 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
