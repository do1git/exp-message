package site.rahoon.message.monolithic.channel.domain.component

import org.springframework.stereotype.Component
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * Channel API 키 생성 컴포넌트 (순수 함수)
 * 형식만 생성하며, 중복 검사는 도메인 레이어에서 수행합니다.
 *
 * ## 구성 (총 10바이트 = 80비트)
 * | 구간 | 크기 | 설명 |
 * |------|------|------|
 * | timestamp | 5바이트 (40비트) | currentTimeMillis() 하위 5바이트 |
 * | random | 5바이트 (40비트) | SecureRandom, 동일 밀리초 내 충돌 방지 |
 *
 * ## 인코딩
 * - base62: 0-9, A-Z, a-z (특수문자 없음)
 * - 접두사: ch_
 *
 * ## 응답 예시 (총 ~18자)
 * ```
 * ch_7K9mN2pQ4rS6tU8vW
 * ch_3FgH8jKlMnOpQrStU
 * ```
 */
interface ChannelApiKeyGenerator {
    /**
     * API 키를 생성합니다.
     */
    fun generate(): String
}

@Component
class ChannelApiKeyGeneratorImpl : ChannelApiKeyGenerator {
    companion object {
        private const val API_KEY_PREFIX = "ch_"

        private const val BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        @Suppress("MagicNumber")
        private const val TIMESTAMP_BYTES = 5

        @Suppress("MagicNumber")
        private const val RANDOM_BYTES = 5

        @Suppress("MagicNumber")
        private const val LONG_BYTES = 8

        @Suppress("MagicNumber")
        private val BASE = BigInteger.valueOf(62)
    }

    override fun generate(): String {
        val timestampBytes =
            ByteBuffer
                .allocate(LONG_BYTES)
                .putLong(System.currentTimeMillis())
                .array()
                .takeLast(TIMESTAMP_BYTES)
                .toByteArray()
        val randomBytes = ByteArray(RANDOM_BYTES).also { SecureRandom().nextBytes(it) }
        val combined = timestampBytes + randomBytes
        val encoded = toBase62(combined)
        return API_KEY_PREFIX + encoded
    }

    private fun toBase62(bytes: ByteArray): String {
        var value = BigInteger(1, bytes)
        val sb = StringBuilder()
        while (value > BigInteger.ZERO) {
            val (quotient, remainder) = value.divideAndRemainder(BASE)
            sb.append(BASE62_CHARS[remainder.toInt()])
            value = quotient
        }
        return sb.reverse().toString()
    }
}
