package site.rahoon.message.__monolitic.common.global.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
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
        serializers: SerializerProvider?
    ) {
        if (value == null) {
            gen.writeNull()
        } else {
            gen.writeString(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        }
    }
}

