package site.rahoon.message.__monolitic.test.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.common.test.assertError
import site.rahoon.message.__monolitic.common.test.assertSuccess

/**
 * Test Controller E2E 테스트
 * 실제 HTTP 요청을 통해 전체 스택을 테스트합니다.
 */
class TestControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    @LocalServerPort private var port: Int = 0
) : IntegrationTestBase() {

    private fun baseUrl(): String = "http://localhost:$port/test"

    @Test
    fun `health check 성공`() {
        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/health",
            HttpMethod.GET,
            null,
            String::class.java
        )

        // then
        response.assertSuccess<Map<String, Any>>(objectMapper, HttpStatus.OK) { data ->
            data["status"] shouldBe "ok"
            data["message"] shouldBe "Service is running"
        }
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
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "COMMON_003") { error ->
            error.occurredAt shouldNotBe null
            error.path shouldNotBe null
        }
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
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "COMMON_001") { error ->
            error.occurredAt shouldNotBe null
            error.path shouldNotBe null
        }
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
        response.assertError(objectMapper, HttpStatus.CONFLICT, "COMMON_002") { error ->
            error.occurredAt shouldNotBe null
            error.path shouldNotBe null
        }
    }
}
