package site.rahoon.message.__monolitic.common.global

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * LocalDateTime <-> epoch millis 변환 확장 함수
 */
fun LocalDateTime.toEpochMilliLong(zoneId: ZoneId): Long {
    return this.atZone(zoneId).toInstant().toEpochMilli()
}

fun Long.toLocalDateTime(zoneId: ZoneId): LocalDateTime {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()
}

/**
 * LocalDateTime <-> epoch micros 변환 확장 함수
 * DB DATETIME(6) 정밀도와 일치
 */
fun LocalDateTime.toEpochMicroLong(zoneId: ZoneId): Long {
    val instant = this.atZone(zoneId).toInstant()
    return instant.epochSecond * 1_000_000L + instant.nano / 1_000
}

fun Long.toLocalDateTimeFromMicros(zoneId: ZoneId): LocalDateTime {
    val seconds = this / 1_000_000L
    val nanos = (this % 1_000_000L) * 1_000L
    return Instant.ofEpochSecond(seconds, nanos).atZone(zoneId).toLocalDateTime()
}

