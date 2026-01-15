package site.rahoon.message.__monolitic.common.application

import site.rahoon.message.__monolitic.common.application.config.PageCursorProperties
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * cursor 디코딩 결과(공통)
 *
 * - version: 커서 포맷 버전 (예: "1")
 * - cursors: 실제 커서 payload
 *
 * ## Payload 인코딩 규칙
 *
 * 1. **Payload 형식**: `key=value&key=value&...` 형태의 query string
 * 2. **특수 문자 금지**: 키와 값에 `&`, `=` 문자는 사용할 수 없음 (파싱 방해 방지)
 *    - 키와 값은 Base62 인코딩 등 URL-safe 문자만 사용해야 함
 *    - 예: Base62 인코딩된 값은 이미 안전하므로 그대로 사용
 * 3. **최종 인코딩**: payload 전체를 Base64URL 인코딩하여 cursor 생성
 *
 * ## 예시
 *
 * ```
 * payload: v=1&sk=ca,i&ca=Base62값&i=Base62값
 *          ↓ (서명 활성화 시: HMAC 서명 추가)
 * payload: v=1&sk=ca,i&ca=Base62값&i=Base62값&s=Base62서명값
 *          ↓ (Base64URL 인코딩)
 * cursor:  dj0xJnNrPWNhLGkmY2E9QmFzZTYy값JmlkPUJhc2U2MlZhbHVlJnM9QmFzZTYy서명값
 * ```
 *
 * ## 서명 (선택적)
 *
 * - **활성화**: `page-cursor.signature-enabled=true` 및 `page-cursor.secret` 설정 시
 * - **서명 방식**: HMAC-SHA256의 처음 16바이트를 Base62로 인코딩
 * - **키 이름**: `s` (짧은 키 이름으로 길이 최소화)
 * - **길이 증가**: 서명 활성화 시 약 22자 추가
 * - **보안**: cursor 변조 방지 (악의적 요청 차단)
 */
open class CommonPageCursor(
    val version: String,
    val cursors: List<Pair<String, String>>,
    private var encoded: String? = null,
    private var cursorMap: Map<String,String>? = null,
){

    companion object {
        /**
         * Cursor 서명을 위한 설정 (선택적)
         * null이면 서명을 사용하지 않음
         */
        @Volatile
        private var signatureConfig: PageCursorProperties? = null

        /**
         * Cursor 서명 설정 초기화
         * 애플리케이션 시작 시 한 번 호출
         */
        fun initializeSignature(config: PageCursorProperties) {
            signatureConfig = config
        }

        /**
         * 특수 문자 검증: 키와 값에 `&`, `=` 문자가 포함되어 있으면 안 됨
         */
        private fun validateSafeString(str: String, fieldName: String) {
            if (str.contains("&") || str.contains("=")) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf(
                        "reason" to "invalid character in $fieldName",
                        "fieldName" to fieldName,
                        "value" to str,
                        "message" to "$fieldName contains forbidden characters (& or =)"
                    )
                )
            }
        }

        /**
         * HMAC 서명 생성 (길이 최소화: SHA-256의 처음 16바이트만 사용)
         * Base62 인코딩으로 URL-safe하게 변환
         */
        private fun generateSignature(payload: String, secret: String): String {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
            mac.init(secretKey)
            val hash = mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            // 길이 최소화: 처음 16바이트만 사용 (128비트 보안)
            val truncated = hash.sliceArray(0 until 16)
            // Base62 인코딩으로 변환 (Base64 대신)
            return site.rahoon.message.__monolitic.common.global.Base62Encoding.encodeBytes(truncated)
        }

        /**
         * 서명 검증
         */
        private fun verifySignature(payload: String, signature: String, secret: String): Boolean {
            val expected = generateSignature(payload, secret)
            return expected == signature
        }

        /**
         * cursors 순서에 따라 sk가 생성됩니다.
         */
        fun encode(
            version: String,
            cursors: List<Pair<String, String>>,
        ): String {
            return CommonPageCursor(version, cursors).encode()
        }

        fun decode(cursor: String): CommonPageCursor {
            val payload = try {
                val decoded = Base64.getUrlDecoder().decode(cursor)
                String(decoded, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "reason" to "base64url decode failed"),
                    cause = e
                )
            }

            // 빈 payload 체크
            if (payload.isBlank()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "empty payload")
                )
            }

            var version: String? = null
            var versionCount = 0
            var sortKeys: List<String> = emptyList()
            var sortKeysCount = 0
            val pairs = mutableListOf<Pair<String, String>>()
            val seenKeys = mutableSetOf<String>()

            payload.split("&").forEach { part ->
                val idx = part.indexOf("=")
                if (idx <= 0) return@forEach

                val key = part.substring(0, idx)
                val value = part.substring(idx + 1)
                if (key.isBlank()) return@forEach

                // 특수 문자 검증
                validateSafeString(key, "key")
                validateSafeString(value, "value")

                when (key) {
                    "v" -> {
                        versionCount++
                        if (versionCount > 1) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate version key(v)"
                                )
                            )
                        }
                        version = value
                    }
                    "sk" -> {
                        sortKeysCount++
                        if (sortKeysCount > 1) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate sort keys key(sk)"
                                )
                            )
                        }
                        sortKeys = value
                            .split(",")
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toList()
                        
                        // sk에 중복된 키가 있는지 체크 (Set으로 효율적으로)
                        val sortKeysSet = mutableSetOf<String>()
                        val duplicateInSk = mutableListOf<String>()
                        for (sortKey in sortKeys) {
                            if (!sortKeysSet.add(sortKey)) {
                                duplicateInSk.add(sortKey)
                            }
                        }
                        if (duplicateInSk.isNotEmpty()) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate keys in sort keys(sk)",
                                    "duplicateKeys" to duplicateInSk
                                )
                            )
                        }
                    }
                    "s" -> {
                        // 서명 키는 pairs에 추가하지 않음 (검증용)
                        // 서명은 나중에 검증됨
                    }
                    else -> {
                        // 일반 키 중복 체크
                        if (seenKeys.contains(key)) {
                            throw DomainException(
                                error = CommonError.INVALID_PAGE_CURSOR,
                                details = mapOf(
                                    "cursor" to cursor,
                                    "payload" to payload,
                                    "reason" to "duplicate key in cursor payload",
                                    "duplicateKey" to key
                                )
                            )
                        }
                        seenKeys.add(key)
                        pairs.add(key to value)
                    }
                }
            }

            val appliedVersion = version?.takeIf { it.isNotBlank() }
                ?: throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "missing version(v)")
                )

            // sk가 비어있는지 체크
            if (sortKeys.isEmpty()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf("cursor" to cursor, "payload" to payload, "reason" to "missing or empty sort keys(sk)")
                )
            }

            // sk에 정의된 키가 pairs에 모두 있는지 확인 (누락 키 체크)
            // seenKeys를 재사용하여 불필요한 Set 생성 방지
            val missingKeys = sortKeys.filter { it !in seenKeys }
            if (missingKeys.isNotEmpty()) {
                throw DomainException(
                    error = CommonError.INVALID_PAGE_CURSOR,
                    details = mapOf(
                        "cursor" to cursor,
                        "payload" to payload,
                        "reason" to "missing required keys in cursor payload",
                        "missingKeys" to missingKeys,
                        "sortKeys" to sortKeys,
                        "availableKeys" to seenKeys.toList()
                    )
                )
            }

            // sk에 정의된 key는 먼저, 그 외 key는 뒤로(원래 순서 유지)
            // 정렬 대신 두 리스트로 분리 후 합치기 (O(p log p) → O(p))
            val order = sortKeys
                .withIndex()
                .associate { (idx, key) -> key to idx }
            
            val sortedPairs = mutableListOf<Pair<String, String>>()
            val otherPairs = mutableListOf<Pair<String, String>>()
            
            for (pair in pairs) {
                if (pair.first in order) {
                    sortedPairs.add(pair)
                } else {
                    otherPairs.add(pair)
                }
            }
            
            // sortKeys 순서대로 정렬 (보통 2-3개이므로 O(s log s)는 무시 가능)
            sortedPairs.sortBy { order[it.first] }
            
            val cursors = sortedPairs + otherPairs

            // 서명 검증 (활성화된 경우)
            val config = signatureConfig
            if (config?.isSignatureActive() == true) {
                // 서명 추출
                val signaturePart = payload.split("&").find { it.startsWith("s=") }
                if (signaturePart == null) {
                    throw DomainException(
                        error = CommonError.INVALID_PAGE_CURSOR,
                        details = mapOf(
                            "cursor" to cursor,
                            "payload" to payload,
                            "reason" to "missing signature"
                        )
                    )
                }
                
                val signature = signaturePart.substring(2)

                // 서명 제외한 payload 재구성
                val payloadWithoutSignature = payload.split("&")
                    .filterNot { it.startsWith("s=") }
                    .joinToString("&")

                // 서명 검증
                if (!verifySignature(payloadWithoutSignature, signature, config.secret)) {
                    throw DomainException(
                        error = CommonError.INVALID_PAGE_CURSOR,
                        details = mapOf(
                            "cursor" to cursor,
                            "payload" to payload,
                            "reason" to "invalid signature"
                        )
                    )
                }
            }

            return CommonPageCursor(version = appliedVersion, cursors = cursors)
        }
    }

    fun encode(): String {
        if(this.encoded != null) return this.encoded!!
        val sortKeys = cursors.map { it.first }
        
        // 특수 문자 검증
        validateSafeString(version, "version")
        sortKeys.forEach { validateSafeString(it, "sortKey") }
        cursors.forEach { (key, value) ->
            validateSafeString(key, "key")
            validateSafeString(value, "value")
        }
        
        val payload = buildString {
            append("v=")
            append(version)
            append("&sk=")
            append(sortKeys.joinToString(","))
            cursors.forEach { (key, value) ->
                append("&")
                append(key)
                append("=")
                append(value)
            }
        }

        // 서명 추가 (활성화된 경우)
        val config = signatureConfig
        val finalPayload = if (config?.isSignatureActive() == true) {
            val signature = generateSignature(payload, config.secret)
            "$payload&s=$signature"
        } else {
            payload
        }

        this.encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(finalPayload.toByteArray(StandardCharsets.UTF_8))
        return this.encoded!!
    }
    
    private fun validateSafeString(str: String, fieldName: String) {
        if (str.contains("&") || str.contains("=")) {
            throw DomainException(
                error = CommonError.INVALID_PAGE_CURSOR,
                details = mapOf(
                    "reason" to "invalid character in $fieldName",
                    "fieldName" to fieldName,
                    "value" to str,
                    "message" to "$fieldName contains forbidden characters (& or =)"
                )
            )
        }
    }

    private fun getCursorMap() : Map<String,String>{
        if(this.cursorMap != null) return this.cursorMap!!
        this.cursorMap = cursors.toMap()
        return this.cursorMap!!
    }


    private fun invalid(reason: String, extra: Map<String, Any> = emptyMap()): Nothing {
        throw DomainException(
            error = CommonError.INVALID_PAGE_CURSOR,
            details = mapOf("cursor" to encode(), "reason" to reason) + extra
        )
    }

    fun requireVersion(expected: String): CommonPageCursor {
        if (version != expected) {
            invalid(
                reason = "unsupported version",
                extra = mapOf("version" to version, "expected" to expected)
            )
        }
        return this
    }

    fun requireKeysInOrder(expectedKeys: List<String>): CommonPageCursor{
        val actualKeys = cursors.map { it.first }
        if (actualKeys != expectedKeys) {
            invalid(
                reason = "invalid keys order",
                extra = mapOf("expectedKeys" to expectedKeys, "actualKeys" to actualKeys)
            )
        }
        return this
    }

    fun getAsString(key: String): String {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value
    }

    fun getAsLong(key: String): Long {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value.toLongOrNull()
            ?: invalid(reason = "invalid long value", extra = mapOf("key" to key, "value" to value))
    }

    fun getAsDouble(key: String): Double {
        val value = getCursorMap()[key]
        if(value.isNullOrEmpty()) invalid(reason = "missing required key", extra = mapOf("key" to key))
        if (value.isBlank()) invalid(reason = "blank required key", extra = mapOf("key" to key))
        return value.toDoubleOrNull()
            ?: invalid(reason = "invalid double value", extra = mapOf("key" to key, "value" to value))
    }
}
