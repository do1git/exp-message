package site.rahoon.message.__monolitic.common.infrastructure

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.common.global.LockToken
import site.rahoon.message.__monolitic.common.global.LockRepository
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * LockRepository의 Redis 구현체
 * Redis SETNX + Lua 스크립트를 사용한 분산 락 구현
 * LockToken 기반으로 자신이 획득한 락만 해제 가능
 */
@Repository
class LockRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : LockRepository {

    companion object {
        private const val LOCK_PREFIX = "lock:"
    }

    override fun acquireLock(key: String, ttl: Duration): LockToken? {
        val redisKey = "$LOCK_PREFIX$key"
        val lockId = UUID.randomUUID().toString()
        val result = redisTemplate.opsForValue().setIfAbsent(redisKey, lockId, ttl)
        return if (result == true) {
            LockToken(key, lockId, Instant.now().plus(ttl))
        } else null
    }

    override fun acquireLocks(keys: List<String>, ttl: Duration): LockToken? {
        if (keys.isEmpty()) {
            return LockToken(emptyList(), UUID.randomUUID().toString(), Instant.now().plus(ttl))
        }

        val sortedKeys = keys.sorted()
        val redisKeys = sortedKeys.map { "$LOCK_PREFIX$it" }
        val lockId = UUID.randomUUID().toString()
        val ttlSeconds = ttl.toSeconds().toString()

        // Lua 스크립트로 all-or-nothing 락 획득
        val script = DefaultRedisScript<Long>(
            """
            -- 먼저 모든 키가 획득 가능한지 확인
            for i = 1, #KEYS do
                if redis.call('EXISTS', KEYS[i]) == 1 then
                    return 0
                end
            end
            -- 모든 키 획득 (동일한 lockId 사용)
            for i = 1, #KEYS do
                redis.call('SET', KEYS[i], ARGV[1], 'EX', ARGV[2])
            end
            return 1
            """.trimIndent(),
            Long::class.java
        )

        val result = redisTemplate.execute(script, redisKeys, lockId, ttlSeconds)
        return if (result == 1L) {
            LockToken(sortedKeys, lockId, Instant.now().plus(ttl))
        } else null
    }

    override fun releaseLock(token: LockToken): Boolean {
        if (token.keys.isEmpty()) return true

        val redisKeys = token.keys.map { "$LOCK_PREFIX$it" }

        // Lua 스크립트로 자신의 락만 해제 (원자적 연산)
        val script = DefaultRedisScript<Long>(
            """
            local released = 0
            for i = 1, #KEYS do
                if redis.call('GET', KEYS[i]) == ARGV[1] then
                    released = released + redis.call('DEL', KEYS[i])
                end
            end
            return released
            """.trimIndent(),
            Long::class.java
        )

        val result = redisTemplate.execute(script, redisKeys, token.lockId)
        return result == token.keys.size.toLong()
    }
}
