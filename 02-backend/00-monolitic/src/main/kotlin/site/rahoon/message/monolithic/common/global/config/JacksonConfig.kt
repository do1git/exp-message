package site.rahoon.message.monolithic.common.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Jackson 직렬화 설정
 */
@Configuration
class JacksonConfig {
    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        val module = SimpleModule()
        module.addSerializer(ZonedDateTime::class.java, ZonedDateTimeSerializer())
        module.addDeserializer(ZonedDateTime::class.java, ZonedDateTimeDeserializer())
        module.addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
        module.addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())

        return builder
            .modules(module, KotlinModule.Builder().build())
            .build()
    }
}

/**
 * ZonedDateTime을 ISO-8601 형식으로 직렬화하는 Serializer
 */
private class ZonedDateTimeSerializer : JsonSerializer<ZonedDateTime>() {
    override fun serialize(
        value: ZonedDateTime?,
        gen: JsonGenerator,
        serializers: SerializerProvider?,
    ) {
        if (value == null) {
            gen.writeNull()
        } else {
            gen.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        }
    }
}

/**
 * LocalDateTime을 ISO-8601 형식으로 직렬화하는 Serializer
 */
private class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {
    override fun serialize(
        value: LocalDateTime?,
        gen: JsonGenerator,
        serializers: SerializerProvider?,
    ) {
        if (value == null) {
            gen.writeNull()
        } else {
            gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        }
    }
}

/**
 * ZonedDateTime을 ISO-8601 형식으로 역직렬화하는 Deserializer
 */
private class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?,
    ): ZonedDateTime {
        val text = p.text
        return if (text.isBlank()) {
            throw IllegalArgumentException("ZonedDateTime 문자열이 비어있습니다")
        } else {
            ZonedDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
    }
}

/**
 * LocalDateTime을 ISO-8601 형식으로 역직렬화하는 Deserializer
 */
private class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?,
    ): LocalDateTime {
        val text = p.text
        return if (text.isBlank()) {
            throw IllegalArgumentException("LocalDateTime 문자열이 비어있습니다")
        } else {
            LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }
}
