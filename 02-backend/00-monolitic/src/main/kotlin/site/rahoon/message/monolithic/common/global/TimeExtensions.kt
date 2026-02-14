package site.rahoon.message.monolithic.common.global

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** System.nanoTime() 차이값을 밀리초로 변환할 때 사용 */
const val NANOSECONDS_PER_MILLISECOND = 1_000_000L

/**
 * System.nanoTime() 차이값(endNano - startNano)을 밀리초로 변환
 */
fun Long.nanoToMs(): Long = this / NANOSECONDS_PER_MILLISECOND

/**
 * LocalDateTime <-> epoch millis 변환 확장 함수
 */
fun LocalDateTime.toEpochMilliLong(zoneId: ZoneId): Long = this.atZone(zoneId).toInstant().toEpochMilli()

fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()

/**
 * LocalDateTime <-> epoch micros 변환 확장 함수
 * DB DATETIME(6) 정밀도와 일치
 */
@Suppress("MagicNumber")
fun LocalDateTime.toEpochMicroLong(zoneId: ZoneId): Long {
    val instant = this.atZone(zoneId).toInstant()
    return instant.epochSecond * 1_000_000L + instant.nano / 1_000
}

@Suppress("MagicNumber")
fun Long.toLocalDateTimeFromMicros(zoneId: ZoneId): LocalDateTime {
    val seconds = this / 1_000_000L
    val nanos = (this % 1_000_000L) * 1_000L
    return Instant.ofEpochSecond(seconds, nanos).atZone(zoneId).toLocalDateTime()
}
