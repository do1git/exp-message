package site.rahoon.message.monolithic.authtoken.domain

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authtoken")
@Suppress("MagicNumber")
data class AuthTokenProperties(
    val accessTokenSecret: String, // Access Token HMAC 서명에 사용할 시크릿 키 (HS256 기준 최소 32바이트 권장)
    val issuer: String = "localhost:8080", // 토큰 발급자
    val accessTokenTtlSeconds: Long = 60 * 60, // 1 hour
    val refreshTokenTtlSeconds: Long = 60L * 60L * 24L * 14L, // 14 days
)
