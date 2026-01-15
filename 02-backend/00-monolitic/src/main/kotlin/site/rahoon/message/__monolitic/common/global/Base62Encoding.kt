package site.rahoon.message.__monolitic.common.global

import java.util.UUID

/**
 * Base62 인코딩/디코딩 유틸리티
 * 
 * 커서 키의 숫자 값(타임스탬프, ID 등)을 더 짧게 표현하기 위해 사용
 * Base62는 0-9, a-z, A-Z를 사용하여 URL-safe하며 Base64보다 짧음
 * 
 * 예시:
 * - Long: 1736900000123 → "1k2m3n4p" (약 30-40% 길이 감소)
 * - UUID: 36자 → 약 21-22자 (16바이트를 Base62로 인코딩)
 */
object Base62Encoding {
    private const val BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    private const val BASE = 62L

    /**
     * Long 값을 Base62 문자열로 인코딩
     */
    fun encodeLong(value: Long): String {
        if (value == 0L) return "0"
        
        val result = StringBuilder()
        var num = if (value < 0) -value else value
        
        while (num > 0) {
            result.append(BASE62_CHARS[(num % BASE).toInt()])
            num /= BASE
        }
        
        if (value < 0) result.append("-")
        return result.reverse().toString()
    }

    /**
     * Base62 문자열을 Long 값으로 디코딩
     */
    fun decodeLong(encoded: String): Long {
        if (encoded.isEmpty()) throw IllegalArgumentException("Empty string cannot be decoded")
        
        val isNegative = encoded.startsWith("-")
        val str = if (isNegative) encoded.substring(1) else encoded
        
        var result = 0L
        var multiplier = 1L
        
        for (i in str.length - 1 downTo 0) {
            val char = str[i]
            val index = BASE62_CHARS.indexOf(char)
            if (index == -1) {
                throw IllegalArgumentException("Invalid Base62 character: $char")
            }
            result += index * multiplier
            multiplier *= BASE
        }
        
        return if (isNegative) -result else result
    }

    /**
     * UUID를 Base62로 인코딩
     * UUID를 16바이트로 파싱하여 Base62로 인코딩 (36자 → 약 21-22자)
     */
    fun encodeUuid(uuid: UUID): String {
        val mostSigBits = uuid.mostSignificantBits
        val leastSigBits = uuid.leastSignificantBits
        
        val bytes = ByteArray(16)
        // mostSignificantBits (8바이트)
        for (i in 7 downTo 0) {
            bytes[i] = ((mostSigBits ushr (8 * (7 - i))) and 0xFF).toByte()
        }
        // leastSignificantBits (8바이트)
        for (i in 15 downTo 8) {
            bytes[i] = ((leastSigBits ushr (8 * (15 - i))) and 0xFF).toByte()
        }
        
        return encodeBytes(bytes)
    }

    /**
     * Base62 문자열을 UUID로 디코딩
     */
    fun decodeUuid(encoded: String): UUID {
        val bytes = decodeBytes(encoded)
        if (bytes.size != 16) {
            throw IllegalArgumentException("Decoded bytes must be exactly 16 bytes for UUID, got ${bytes.size}")
        }
        
        var mostSigBits = 0L
        var leastSigBits = 0L
        
        // mostSignificantBits (8바이트)
        for (i in 0 until 8) {
            mostSigBits = (mostSigBits shl 8) or (bytes[i].toLong() and 0xFF)
        }
        // leastSignificantBits (8바이트)
        for (i in 8 until 16) {
            leastSigBits = (leastSigBits shl 8) or (bytes[i].toLong() and 0xFF)
        }
        
        return UUID(mostSigBits, leastSigBits)
    }

    /**
     * 문자열을 Base62로 인코딩
     * 일반 문자열을 UTF-8 바이트로 변환 후 Base62 인코딩
     */
    fun encodeString(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return encodeBytes(bytes)
    }

    /**
     * Base62 문자열을 원본 문자열로 디코딩
     */
    fun decodeString(encoded: String): String {
        val bytes = decodeBytes(encoded)
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * 바이트 배열을 Base62로 인코딩
     * 3바이트씩 묶어서 24비트를 Base62로 인코딩 (오버플로우 방지)
     */
    fun encodeBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        
        val result = StringBuilder()
        var i = 0
        
        while (i < bytes.size) {
            // 3바이트씩 묶어서 처리
            var value = 0L
            var bits = 0
            
            for (j in 0 until 3) {
                if (i + j < bytes.size) {
                    value = (value shl 8) or (bytes[i + j].toLong() and 0xFF)
                    bits += 8
                }
            }
            
            // 24비트를 Base62로 인코딩 (최대 4자)
            var temp = value
            var encodedPart = StringBuilder()
            for (k in 0 until 4) {
                if (temp == 0L && k > 0 && i + 3 >= bytes.size) break
                encodedPart.append(BASE62_CHARS[(temp % BASE).toInt()])
                temp /= BASE
            }
            result.append(encodedPart.reverse())
            
            i += 3
        }
        
        return result.toString()
    }

    /**
     * Base62 문자열을 바이트 배열로 디코딩
     */
    fun decodeBytes(encoded: String): ByteArray {
        if (encoded.isEmpty()) return ByteArray(0)
        
        val bytes = mutableListOf<Byte>()
        var i = 0
        
        while (i < encoded.length) {
            // 4자씩 읽어서 24비트로 디코딩
            val end = minOf(i + 4, encoded.length)
            val chunk = encoded.substring(i, end)
            
            var value = 0L
            var multiplier = 1L
            for (j in chunk.length - 1 downTo 0) {
                val char = chunk[j]
                val index = BASE62_CHARS.indexOf(char)
                if (index == -1) {
                    throw IllegalArgumentException("Invalid Base62 character: $char")
                }
                value += index * multiplier
                multiplier *= BASE
            }
            
            // 24비트를 3바이트로 변환
            val tempBytes = mutableListOf<Byte>()
            var temp = value
            for (j in 0 until 3) {
                tempBytes.add(0, (temp and 0xFF).toByte())
                temp = temp ushr 8
            }
            
            // 실제 필요한 바이트만 추가 (마지막 청크는 패딩 제거)
            val actualBytes = if (end == encoded.length) {
                // 마지막 청크: 앞의 0 바이트 제거
                tempBytes.dropWhile { it == 0.toByte() }
            } else {
                tempBytes
            }
            bytes.addAll(actualBytes)
            
            i = end
        }
        
        return bytes.toByteArray()
    }
}
