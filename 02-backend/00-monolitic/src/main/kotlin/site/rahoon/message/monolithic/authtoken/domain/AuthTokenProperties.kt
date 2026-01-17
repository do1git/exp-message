package site.rahoon.message.monolithic.authtoken.domain

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authtoken")
data class AuthTokenProperties(
    /** Access Token HMAC 서명에 사용할 시크릿 키 (HS256 기준 최소 32바이트 권장) */
    val accessTokenSecret: String,
    val issuer: String = "site.rahoon.message",
    val accessTokenTtlSeconds: Long = 60 * 60,
    /** 리프레시 토큰 만료(초) */
    val refreshTokenTtlSeconds: Long = 60L * 60L * 24L * 14L, // 14 days
)
