package site.rahoon.message.monolithic.common.infrastructure.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(
    // ""가 들어옴
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
    @Value("\${spring.data.redis.password:}") private val password: String?,
) {
    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        val serverConfig = config
            .useSingleServer()
            .setAddress("redis://$host:$port")

        // 핵심: 빈 문자열이면 password를 세팅하지 않음 (null 상태 유지)
        if (!password.isNullOrBlank()) {
            serverConfig.password = password
        }

        return Redisson.create(config)
    }
}
