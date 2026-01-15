package site.rahoon.message.__monolitic.common.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.common.application.config.PageCursorProperties
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException

/**
 * CommonPageCursor 단위 테스트
 * encode/decode 및 검증 로직을 테스트합니다.
 */
class CommonPageCursorUT {

    @Test
    fun `정상적인 encode와 decode - round trip 성공`() {
        // given
        val version = "1"
        val cursors = listOf(
            "createdAt" to "1736900000123",
            "id" to "12345"
        )

        // when
        val encoded = CommonPageCursor.encode(version, cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.version shouldBe version
        decoded.cursors shouldBe cursors
    }

    @Test
    fun `정상적인 encode와 decode - 여러 키`() {
        // given
        val version = "1"
        val cursors = listOf(
            "createdAt" to "1736900000123",
            "id" to "12345",
            "extra" to "value"
        )

        // when
        val encoded = CommonPageCursor.encode(version, cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.version shouldBe version
        decoded.cursors.size shouldBe 3
        decoded.getAsString("createdAt") shouldBe "1736900000123"
        decoded.getAsString("id") shouldBe "12345"
        decoded.getAsString("extra") shouldBe "value"
    }

    @Test
    fun `decode - 잘못된 Base64 형식`() {
        // given
        val invalidCursor = "invalid-base64!!!"

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(invalidCursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "base64url decode failed"
    }

    @Test
    fun `decode - 빈 payload`() {
        // given
        val emptyPayload = ""
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(emptyPayload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "empty payload"
    }

    @Test
    fun `decode - 특수 문자 포함 key 거부`() {
        // given
        val payload = "v=1&sk=key&key&with=ampersand=value"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"]?.toString()?.contains("invalid character") shouldBe true
    }

    @Test
    fun `decode - version(v) 누락`() {
        // given
        val payload = "sk=createdAt,id&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "missing version(v)"
    }

    @Test
    fun `decode - version(v) 중복`() {
        // given
        val payload = "v=1&v=2&sk=createdAt,id&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "duplicate version key(v)"
    }

    @Test
    fun `decode - sort keys(sk) 누락`() {
        // given
        val payload = "v=1&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "missing or empty sort keys(sk)"
    }

    @Test
    fun `decode - sort keys(sk) 중복`() {
        // given
        val payload = "v=1&sk=createdAt,id&sk=createdAt,id&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "duplicate sort keys key(sk)"
    }

    @Test
    fun `decode - sort keys(sk) 내부 중복 키`() {
        // given
        val payload = "v=1&sk=createdAt,createdAt,id&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "duplicate keys in sort keys(sk)"
        val duplicateKeys = exception.details!!["duplicateKeys"] as? List<*>
        duplicateKeys shouldNotBe null
        duplicateKeys!!.contains("createdAt") shouldBe true
    }

    @Test
    fun `decode - 일반 키 중복`() {
        // given
        val payload = "v=1&sk=createdAt,id&createdAt=123&createdAt=456&id=789"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "duplicate key in cursor payload"
        exception.details!!["duplicateKey"] shouldBe "createdAt"
    }

    @Test
    fun `decode - sk에 정의된 키가 pairs에 누락`() {
        // given
        val payload = "v=1&sk=createdAt,id,missing&createdAt=123&id=456"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "missing required keys in cursor payload"
        val missingKeys = exception.details!!["missingKeys"] as? List<*>
        missingKeys shouldNotBe null
        missingKeys!!.contains("missing") shouldBe true
    }

    @Test
    fun `requireVersion - 올바른 버전`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("key" to "value"))

        // when
        val decoded = CommonPageCursor.decode(cursor).requireVersion("1")

        // then
        decoded.version shouldBe "1"
    }

    @Test
    fun `requireVersion - 잘못된 버전`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("key" to "value"))

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor).requireVersion("2")
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "unsupported version"
        exception.details!!["version"] shouldBe "1"
        exception.details!!["expected"] shouldBe "2"
    }

    @Test
    fun `requireKeysInOrder - 올바른 순서`() {
        // given
        val cursors = listOf("createdAt" to "123", "id" to "456")
        val cursor = CommonPageCursor.encode("1", cursors)

        // when
        val decoded = CommonPageCursor.decode(cursor).requireKeysInOrder(listOf("createdAt", "id"))

        // then
        decoded.cursors.map { it.first } shouldBe listOf("createdAt", "id")
    }

    @Test
    fun `requireKeysInOrder - 잘못된 순서`() {
        // given
        val cursors = listOf("createdAt" to "123", "id" to "456")
        val cursor = CommonPageCursor.encode("1", cursors)

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor).requireKeysInOrder(listOf("id", "createdAt"))
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "invalid keys order"
    }

    @Test
    fun `getAsString - 정상적인 값`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("key" to "value"))

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        decoded.getAsString("key") shouldBe "value"
    }

    @Test
    fun `getAsString - 누락된 키`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("key" to "value"))

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor).getAsString("missing")
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "missing required key"
        exception.details!!["key"] shouldBe "missing"
    }

    @Test
    fun `getAsLong - 정상적인 값`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("timestamp" to "1736900000123"))

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        decoded.getAsLong("timestamp") shouldBe 1736900000123L
    }

    @Test
    fun `getAsLong - 잘못된 형식`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("timestamp" to "not-a-number"))

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor).getAsLong("timestamp")
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "invalid long value"
        exception.details!!["key"] shouldBe "timestamp"
    }

    @Test
    fun `getAsDouble - 정상적인 값`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("score" to "95.5"))

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        decoded.getAsDouble("score") shouldBe 95.5
    }

    @Test
    fun `getAsDouble - 잘못된 형식`() {
        // given
        val cursor = CommonPageCursor.encode("1", listOf("score" to "not-a-double"))

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.decode(cursor).getAsDouble("score")
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"] shouldBe "invalid double value"
        exception.details!!["key"] shouldBe "score"
    }

    @Test
    fun `encode - 여러 번 호출해도 동일한 결과`() {
        // given
        val version = "1"
        val cursors = listOf("key" to "value")

        // when
        val encoded1 = CommonPageCursor.encode(version, cursors)
        val encoded2 = CommonPageCursor.encode(version, cursors)

        // then
        encoded1 shouldBe encoded2
    }

    @Test
    fun `decode - sortKeys 순서대로 정렬된 cursors 반환`() {
        // given
        // sk에 정의된 순서: createdAt, id
        // 실제 pairs 순서: id, createdAt (다른 순서)
        val payload = "v=1&sk=createdAt,id&id=456&createdAt=123"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        // sortKeys 순서대로 정렬되어야 함
        decoded.cursors.map { it.first } shouldBe listOf("createdAt", "id")
        decoded.cursors.map { it.second } shouldBe listOf("123", "456")
    }

    @Test
    fun `decode - sortKeys에 없는 키는 뒤에 유지`() {
        // given
        val payload = "v=1&sk=createdAt,id&createdAt=123&id=456&extra=value"
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        val keys = decoded.cursors.map { it.first }
        keys.indexOf("createdAt") shouldBe 0
        keys.indexOf("id") shouldBe 1
        keys.indexOf("extra") shouldBe 2
    }

    @Test
    fun `decode - 빈 value 허용`() {
        // given
        val payload = "v=1&sk=key&key="
        val cursor = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())

        // when
        val decoded = CommonPageCursor.decode(cursor)

        // then
        // getAsString은 blank를 허용하지 않으므로 cursorMap에서 직접 확인
        decoded.cursors.find { it.first == "key" }?.second shouldBe ""
    }

    @Test
    fun `encode decode - 특수 문자 포함 value 거부`() {
        // given
        val value = "value&with=special"
        val cursors = listOf("key" to value)

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.encode("1", cursors)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"]?.toString()?.contains("invalid character") shouldBe true
    }

    @Test
    fun `encode decode - 특수 문자 포함 key 거부`() {
        // given
        val cursors = listOf("key&with=special" to "value")

        // when & then
        val exception = shouldThrow<DomainException> {
            CommonPageCursor.encode("1", cursors)
        }

        exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
        exception.details!!["reason"]?.toString()?.contains("invalid character") shouldBe true
    }

    @Test
    fun `encode decode - Base62 인코딩된 값 정상 처리`() {
        // given
        // Base62 인코딩된 값은 특수 문자가 없으므로 정상 처리됨
        val value = "1k2m3n4p" // Base62 인코딩된 예시
        val cursors = listOf("key" to value)

        // when
        val encoded = CommonPageCursor.encode("1", cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.getAsString("key") shouldBe value
    }

    @Test
    fun `encode decode - sortKeys에 특수 문자 포함`() {
        // given
        // 실제로는 sortKeys는 키 이름이므로 특수 문자가 없어야 하지만, 인코딩이 제대로 되는지 테스트
        val cursors = listOf("createdAt" to "123", "id" to "456")

        // when
        val encoded = CommonPageCursor.encode("1", cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.cursors.map { it.first } shouldBe listOf("createdAt", "id")
    }

    @Test
    fun `encode decode - version에 특수 문자 포함`() {
        // given
        val version = "1.0"
        val cursors = listOf("key" to "value")

        // when
        val encoded = CommonPageCursor.encode(version, cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.version shouldBe version
    }

    @Test
    fun `encode decode - 서명 없이 정상 동작`() {
        // given
        // 서명이 비활성화된 상태 (기본값)
        val cursors = listOf("createdAt" to "123", "id" to "456")

        // when
        val encoded = CommonPageCursor.encode("1", cursors)
        val decoded = CommonPageCursor.decode(encoded)

        // then
        decoded.version shouldBe "1"
        decoded.cursors shouldBe cursors
    }

    @Test
    fun `encode decode - 서명 활성화 시 서명 포함`() {
        // given
        val secret = "test-secret-key-16bytes"
        CommonPageCursor.initializeSignature(
            PageCursorProperties(secret = secret, signatureEnabled = true)
        )
        val cursors = listOf("createdAt" to "123", "id" to "456")

        try {
            // when
            val encoded = CommonPageCursor.encode("1", cursors)
            val decoded = CommonPageCursor.decode(encoded)

            // then
            decoded.version shouldBe "1"
            decoded.cursors shouldBe cursors
        } finally {
            // cleanup
            CommonPageCursor.initializeSignature(PageCursorProperties())
        }
    }

    @Test
    fun `decode - 서명 활성화 시 잘못된 서명 거부`() {
        // given
        val secret = "test-secret-key-16bytes"
        CommonPageCursor.initializeSignature(
            PageCursorProperties(secret = secret, signatureEnabled = true)
        )
        val cursors = listOf("key" to "value")
        val validCursor = CommonPageCursor.encode("1", cursors)

        try {
            // 잘못된 서명으로 cursor 변조
            val payload = String(java.util.Base64.getUrlDecoder().decode(validCursor), java.nio.charset.StandardCharsets.UTF_8)
            val payloadWithoutSig = payload.split("&").filterNot { it.startsWith("s=") }.joinToString("&")
            val wrongSignature = "wrong-signature"
            val tamperedPayload = "$payloadWithoutSig&s=$wrongSignature"
            val tamperedCursor = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tamperedPayload.toByteArray(java.nio.charset.StandardCharsets.UTF_8))

            // when & then
            val exception = shouldThrow<DomainException> {
                CommonPageCursor.decode(tamperedCursor)
            }

            exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
            exception.details!!["reason"] shouldBe "invalid signature"
        } finally {
            // cleanup
            CommonPageCursor.initializeSignature(PageCursorProperties())
        }
    }

    @Test
    fun `decode - 서명 활성화 시 서명 누락 거부`() {
        // given
        val secret = "test-secret-key-16bytes"
        CommonPageCursor.initializeSignature(
            PageCursorProperties(secret = secret, signatureEnabled = true)
        )

        try {
            // 서명 없이 payload 생성
            val payload = "v=1&sk=key&key=value"
            val cursor = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.toByteArray(java.nio.charset.StandardCharsets.UTF_8))

            // when & then
            val exception = shouldThrow<DomainException> {
                CommonPageCursor.decode(cursor)
            }

            exception.error shouldBe CommonError.INVALID_PAGE_CURSOR
            exception.details!!["reason"] shouldBe "missing signature"
        } finally {
            // cleanup
            CommonPageCursor.initializeSignature(PageCursorProperties())
        }
    }

    @Test
    fun `encode decode - 서명 길이 확인`() {
        // given
        val secret = "test-secret-key-16bytes"
        CommonPageCursor.initializeSignature(
            PageCursorProperties(secret = secret, signatureEnabled = true)
        )
        val cursors = listOf("createdAt" to "123", "id" to "456")

        try {
            // when
            val encoded = CommonPageCursor.encode("1", cursors)
            val decoded = CommonPageCursor.decode(encoded)

            // then
            decoded.version shouldBe "1"
            decoded.cursors shouldBe cursors

            // 서명 길이 확인 (Base62 인코딩된 16바이트)
            val payload = String(java.util.Base64.getUrlDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8)
            val signaturePart = payload.split("&").find { it.startsWith("s=") }
            signaturePart shouldNotBe null
            val signature = signaturePart!!.substring(2)
            // Base62 인코딩된 16바이트는 21자 또는 22자 (128비트를 62진법으로 표현)
            (signature.length in 21..22) shouldBe true
        } finally {
            // cleanup
            CommonPageCursor.initializeSignature(PageCursorProperties())
        }
    }
}
