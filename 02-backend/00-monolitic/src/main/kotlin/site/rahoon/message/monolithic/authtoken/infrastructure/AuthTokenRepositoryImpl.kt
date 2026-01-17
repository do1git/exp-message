package site.rahoon.message.monolithic.authtoken.infrastructure

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import site.rahoon.message.monolithic.authtoken.domain.AuthTokenRepository
import site.rahoon.message.monolithic.authtoken.domain.RefreshToken
import java.time.Duration
import java.time.LocalDateTime

/**
 * AuthTokenRepository 인터페이스의 Redis 구현체
 */
@Repository
class AuthTokenRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : AuthTokenRepository {
    private val logger = LoggerFactory.getLogger(AuthTokenRepositoryImpl::class.java)

    companion object {
        private const val REFRESH_TOKEN_PREFIX = "auth_token:refresh_token:"
        private const val SESSION_REFRESH_TOKEN_PREFIX = "auth_token:session_refresh_token:"
    }

    override fun saveRefreshToken(refreshToken: RefreshToken): RefreshToken {
        val byTokenKey = "$REFRESH_TOKEN_PREFIX${refreshToken.token}"
        val bySessionKey = "$SESSION_REFRESH_TOKEN_PREFIX${refreshToken.sessionId}"
        val now = LocalDateTime.now()

        // 1. RefreshToken 데이터 생성
        val refreshTokenJson = objectMapper.writeValueAsString(refreshToken)
        val refreshTokenTtlSecond = Duration.between(now, refreshToken.expiresAt).seconds
        val refreshTokenTtl = if (refreshTokenTtlSecond > 0) Duration.ofSeconds(refreshTokenTtlSecond) else Duration.ZERO

        // 2. byTokenKey에 RefreshToken 데이터 저장
        redisTemplate.opsForValue().set(byTokenKey, refreshTokenJson, refreshTokenTtl)

        // 3. bySessionKey에 토큰 참조 저장
        redisTemplate.opsForValue().set(bySessionKey, refreshToken.token, refreshTokenTtl)

        return refreshToken
    }

    override fun findRefreshToken(refreshToken: String): RefreshToken? {
        val tokenKey = "$REFRESH_TOKEN_PREFIX$refreshToken"
        val json = redisTemplate.opsForValue().get(tokenKey) ?: return null

        return try {
            objectMapper.readValue(json, RefreshToken::class.java)
        } catch (e: Exception) {
            logger.error(
                "Failed to parse RefreshToken from Redis. tokenKey: $tokenKey, json: $json",
                e,
            )
            throw IllegalStateException(
                "RefreshToken 데이터 파싱 실패: Redis에 저장된 데이터가 손상되었거나 형식이 맞지 않습니다. tokenKey=$tokenKey",
                e,
            )
        }
    }

    override fun deleteRefreshToken(refreshToken: String) {
        val tokenKey = "$REFRESH_TOKEN_PREFIX$refreshToken"

        // 1. sessionId 조회 (삭제 전에 조회)
        val json = redisTemplate.opsForValue().get(tokenKey)
        val sessionKey =
            if (json != null) {
                try {
                    val token = objectMapper.readValue(json, RefreshToken::class.java)
                    "$SESSION_REFRESH_TOKEN_PREFIX${token.sessionId}"
                } catch (e: Exception) {
                    logger.error(
                        "Failed to parse RefreshToken from Redis during deletion. tokenKey: $tokenKey, json: $json",
                        e,
                    )
                    // 삭제 작업이므로 예외를 던지지 않고 null 반환 (부분 실패 허용)
                    null
                }
            } else {
                null
            }

        // 2. 토큰 삭제
        redisTemplate.delete(tokenKey)

        // 3. 세션 키 삭제 (존재하는 경우)
        if (sessionKey != null) {
            redisTemplate.delete(sessionKey)
        }
    }

    override fun deleteRefreshTokenBySessionId(sessionId: String) {
        val sessionKey = "$SESSION_REFRESH_TOKEN_PREFIX$sessionId"

        // 1. 세션 키로부터 토큰 조회
        val token = redisTemplate.opsForValue().get(sessionKey)

        // 2. 토큰이 존재하면 토큰 키 삭제
        if (token != null) {
            val tokenKey = "$REFRESH_TOKEN_PREFIX$token"
            redisTemplate.delete(tokenKey)
        }

        // 3. 세션 키 삭제
        redisTemplate.delete(sessionKey)
    }
}
