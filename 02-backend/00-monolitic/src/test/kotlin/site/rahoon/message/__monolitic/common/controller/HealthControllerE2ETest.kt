package site.rahoon.message.__monolitic.common.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Health Check API E2E 테스트
 * 실제 HTTP 요청을 통해 전체 스택을 테스트합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun baseUrl(): String = "http://localhost:$port/api/health"

    @Test
    fun `health check 성공`() {
        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            null,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        val typeFactory = TypeFactory.defaultInstance()
        val mapType = typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java)
        val type = typeFactory.constructParametricType(
            ApiResponse::class.java,
            mapType
        )
        val responseBody = objectMapper.readValue<ApiResponse<Map<String, Any>>>(
            response.body!!,
            type
        )

        // 응답 검증
        assertTrue(responseBody.success)
        assertNotNull(responseBody.data)
        assertEquals("ok", responseBody.data?.get("status"))
        assertEquals("Service is running", responseBody.data?.get("message"))
    }

    @Test
    fun `error endpoint - CLIENT_ERROR`() {
        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/error?errorType=CLIENT_ERROR",
            HttpMethod.GET,
            null,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        val errorMap = (responseMap["error"] as? Map<*, *>)
        
        // occurredAt이 String으로 직렬화되어 있는지 확인
        val occurredAtString = errorMap?.get("occurredAt") as? String
        assertNotNull(occurredAtString, "occurredAt should be present as String")
        
        // ISO-8601 형식으로 파싱 가능한지 확인
        val occurredAt = ZonedDateTime.parse(occurredAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertNotNull(occurredAt, "occurredAt should be parseable as ZonedDateTime")
        
        val responseBody = ApiResponse<Map<String, Any>>(
            success = responseMap["success"] as Boolean,
            data = responseMap["data"] as? Map<String, Any>,
            error = errorMap?.let {
                ApiResponse.ErrorInfo(
                    code = it["code"] as String,
                    message = it["message"] as String,
                    details = it["details"] as? Map<String, Any>,
                    occurredAt = occurredAt,
                    path = it["path"] as? String
                )
            }
        )

        // 응답 검증
        assertTrue(!responseBody.success)
        assertNotNull(responseBody.error)
        assertEquals("COMMON_003", responseBody.error?.code) // CLIENT_ERROR
        assertNotNull(responseBody.error?.occurredAt)
        assertNotNull(responseBody.error?.path)
    }

    @Test
    fun `error endpoint - NOT_FOUND`() {
        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/error?errorType=NOT_FOUND",
            HttpMethod.GET,
            null,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        val errorMap = (responseMap["error"] as? Map<*, *>)
        
        // occurredAt이 String으로 직렬화되어 있는지 확인
        val occurredAtString = errorMap?.get("occurredAt") as? String
        assertNotNull(occurredAtString, "occurredAt should be present as String")
        
        // ISO-8601 형식으로 파싱 가능한지 확인
        val occurredAt = ZonedDateTime.parse(occurredAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertNotNull(occurredAt, "occurredAt should be parseable as ZonedDateTime")
        
        val responseBody = ApiResponse<Map<String, Any>>(
            success = responseMap["success"] as Boolean,
            data = responseMap["data"] as? Map<String, Any>,
            error = errorMap?.let {
                ApiResponse.ErrorInfo(
                    code = it["code"] as String,
                    message = it["message"] as String,
                    details = it["details"] as? Map<String, Any>,
                    occurredAt = occurredAt,
                    path = it["path"] as? String
                )
            }
        )

        // 응답 검증
        assertTrue(!responseBody.success)
        assertNotNull(responseBody.error)
        assertEquals("COMMON_001", responseBody.error?.code) // NOT_FOUND
        assertNotNull(responseBody.error?.occurredAt)
        assertNotNull(responseBody.error?.path)
    }

    @Test
    fun `error endpoint - CONFLICT`() {
        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/error?errorType=CONFLICT",
            HttpMethod.GET,
            null,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        val errorMap = (responseMap["error"] as? Map<*, *>)
        
        // occurredAt이 String으로 직렬화되어 있는지 확인
        val occurredAtString = errorMap?.get("occurredAt") as? String
        assertNotNull(occurredAtString, "occurredAt should be present as String")
        
        // ISO-8601 형식으로 파싱 가능한지 확인
        val occurredAt = ZonedDateTime.parse(occurredAtString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        assertNotNull(occurredAt, "occurredAt should be parseable as ZonedDateTime")
        
        val responseBody = ApiResponse<Map<String, Any>>(
            success = responseMap["success"] as Boolean,
            data = responseMap["data"] as? Map<String, Any>,
            error = errorMap?.let {
                ApiResponse.ErrorInfo(
                    code = it["code"] as String,
                    message = it["message"] as String,
                    details = it["details"] as? Map<String, Any>,
                    occurredAt = occurredAt,
                    path = it["path"] as? String
                )
            }
        )

        // 응답 검증
        assertTrue(!responseBody.success)
        assertNotNull(responseBody.error)
        assertEquals("COMMON_002", responseBody.error?.code) // CONFLICT
        assertNotNull(responseBody.error?.occurredAt)
        assertNotNull(responseBody.error?.path)
    }
}

