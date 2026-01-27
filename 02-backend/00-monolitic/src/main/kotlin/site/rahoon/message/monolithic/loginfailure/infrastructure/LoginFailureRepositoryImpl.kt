package site.rahoon.message.monolithic.loginfailure.infrastructure

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.loginfailure.domain.LoginFailure
import site.rahoon.message.monolithic.loginfailure.domain.LoginFailureRepository
import java.time.Duration

/**
 * LoginFailureRepository의 Redis 구현체
 */
@Repository
class LoginFailureRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisTemplateLong: RedisTemplate<String, Long>,
) : LoginFailureRepository {
    companion object {
        private const val FAILURE_COUNT_PREFIX = "login_failure:"
    }

    override fun findByKey(key: String): LoginFailure {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        val count = redisTemplate.opsForValue().get(redisKey) ?: return LoginFailure.create(key)
        val failureCount = count.toIntOrNull() ?: 0
        return LoginFailure.from(key, failureCount)
    }

    override fun findByKeys(keys: List<String>): List<LoginFailure> {
        if (keys.isEmpty()) return emptyList()
        val sortedKeys = keys.sorted()
        val redisKeys = sortedKeys.map { "$FAILURE_COUNT_PREFIX$it" }
        val values = redisTemplate.opsForValue().multiGet(redisKeys) ?: emptyList()
        return sortedKeys.mapIndexed { index, key ->
            val failureCount = values.getOrNull(index)?.toIntOrNull() ?: 0
            LoginFailure.from(key, failureCount)
        }
    }

    override fun save(
        loginFailure: LoginFailure,
        ttl: Duration,
    ): LoginFailure {
        val redisKey = "$FAILURE_COUNT_PREFIX${loginFailure.key}"
        redisTemplate.opsForValue().set(redisKey, loginFailure.failureCount.toString(), ttl)
        return loginFailure
    }

    override fun deleteByKey(key: String) {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        redisTemplate.delete(redisKey)
    }

    override fun incrementAndGet(
        key: String,
        ttl: Duration,
    ): Int {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        // Lua 스크립트를 사용하여 INCR과 EXPIRE를 원자적으로 실행
        val script =
            DefaultRedisScript<Long>(
                """
                local newValue = redis.call('INCR', KEYS[1])
                local ttl = tonumber(ARGV[1])
                local currentTtl = redis.call('TTL', KEYS[1])
                -- 키가 새로 생성되었거나 (값이 1인 경우) TTL이 설정된 값보다 작으면 EXPIRE 설정
                if newValue == 1 or currentTtl == -1 or currentTtl < ttl then
                    redis.call('EXPIRE', KEYS[1], ttl)
                end
                return newValue
                """.trimIndent(),
                Long::class.java,
            )
        val newValue =
            redisTemplateLong.execute(script, listOf(redisKey), ttl.toSeconds().toString())
                ?: throw IllegalStateException("Redis INCR operation failed for key: $redisKey")

        return newValue.toInt()
    }

    override fun incrementAndGetMultiple(keyTtlPairs: List<Pair<String, Duration>>): List<LoginFailure> {
        if (keyTtlPairs.isEmpty()) {
            return emptyList()
        }
        val sortedKeyTtpPairs = keyTtlPairs.sortedBy { it.first }
        val redisKeys = sortedKeyTtpPairs.map { "$FAILURE_COUNT_PREFIX${it.first}" }
        val ttls = sortedKeyTtpPairs.map { it.second.toSeconds().toString() }

        // Lua 스크립트를 사용하여 여러 키에 대해 INCR과 EXPIRE를 원자적으로 실행
        val script =
            DefaultRedisScript<List<*>>(
                """
                local results = {}
                for i = 1, #KEYS do
                    local newValue = redis.call('INCR', KEYS[i])
                    local ttl = tonumber(ARGV[i])
                    local currentTtl = redis.call('TTL', KEYS[i])
                    -- 키가 새로 생성되었거나 (값이 1인 경우) TTL이 설정된 값보다 작으면 EXPIRE 설정
                    if newValue == 1 or currentTtl == -1 or currentTtl < ttl then
                        redis.call('EXPIRE', KEYS[i], ttl)
                    end
                    results[i] = newValue
                end
                return results
                """.trimIndent(),
                List::class.java,
            )

        @Suppress("UNCHECKED_CAST")
        val results =
            redisTemplateLong.execute(script, redisKeys, *ttls.toTypedArray()) as? List<Long>
                ?: throw IllegalStateException("Redis INCR operation failed for keys: $redisKeys")

        return sortedKeyTtpPairs.mapIndexed { index, key ->
            val failureCount = results.getOrNull(index) ?: 0
            LoginFailure.from(key.first, failureCount.toInt())
        }
    }

    override fun getAndIncrement(keyLimitTtlTriplet: List<Triple<String, Int, Duration>>): Pair<Boolean, List<LoginFailure>> {
        if (keyLimitTtlTriplet.isEmpty()) {
            return false to emptyList()
        }
        val sortedTriplets = keyLimitTtlTriplet.sortedBy { it.first }
        val redisKeys = sortedTriplets.map { "$FAILURE_COUNT_PREFIX${it.first}" }
        val ttls = sortedTriplets.map { it.third.toSeconds().toString() }
        val maxFailureCounts = sortedTriplets.map { it.second.toLong() }

        // Lua 스크립트를 사용하여 여러 키에 대해 INCR, 잠금 확인, EXPIRE를 원자적으로 실행
        // 1. 모든 값 조회 2. limit 이상인지 확인 -> 하나라도 limit 이상이면 원래 값 반환 3. 모든 값들 업데이트 -> 새로운 값 반환
        val script =
            DefaultRedisScript<List<*>>(
                """
                -- 1단계: 모든 값 조회
                local originalValues = {}
                for i = 1, #KEYS do
                    local currentValue = redis.call('GET', KEYS[i])
                    local currentCount = 0
                    if currentValue then
                        currentCount = tonumber(currentValue)
                    end
                    originalValues[i] = currentCount
                end
                
                -- 2단계: 각 값들에 대해서 limit 이상인지 확인 -> 하나라도 limit 이상인 경우 원래 값 반환
                for i = 1, #KEYS do
                    local maxCount = tonumber(ARGV[i + #KEYS])
                    if originalValues[i] >= maxCount then
                        -- 하나라도 limit 이상이면 업데이트 안 함 (원래 값 반환)
                        originalValues[#originalValues + 1] = 0  -- isUpdated = false
                        return originalValues
                    end
                end
                
                -- 3단계: 모든 값들에 대해서 업데이트 -> 원래 값 반환 (getAndIncrement이므로 증가 전 값 반환)
                for i = 1, #KEYS do
                    local newValue = redis.call('INCR', KEYS[i])
                    local ttl = tonumber(ARGV[i])
                    local currentTtl = redis.call('TTL', KEYS[i])
                    -- 키가 새로 생성되었거나 (값이 1인 경우) TTL이 설정된 값보다 작으면 EXPIRE 설정
                    if newValue == 1 or currentTtl == -1 or currentTtl < ttl then
                        redis.call('EXPIRE', KEYS[i], ttl)
                    end
                end
                
                -- 업데이트됨 (원래 값 반환)
                originalValues[#originalValues + 1] = 1  -- isUpdated = true
                return originalValues
                """.trimIndent(),
                List::class.java,
            )

        @Suppress("UNCHECKED_CAST")
        val results =
            redisTemplateLong.execute(script, redisKeys, *(ttls + maxFailureCounts.map { it.toString() }).toTypedArray()) as? List<Long>
                ?: throw IllegalStateException("Redis getAndIncrement operation failed for keys: $redisKeys")

        val isUpdated = results.lastOrNull() == 1L
        val loginFailures = sortedTriplets.mapIndexed { index, triplet ->
            val value = results.getOrNull(index) ?: 0
            LoginFailure.from(triplet.first, value.toInt())
        }

        return isUpdated to loginFailures
    }
}
