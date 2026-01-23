package site.rahoon.message.monolithic.authtoken.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess
import site.rahoon.message.monolithic.user.application.UserApplicationITUtils

/**
 * Auth Controller 통합 테스트
 * 로그인, 토큰 갱신, 로그아웃 API에 대한 전체 스택 테스트
 */
class AuthControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val userApplicationITUtils: UserApplicationITUtils,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun authBaseUrl(): String = "http://localhost:$port/auth"

    private lateinit var testEmail: String
    private lateinit var testPassword: String

    @BeforeEach
    fun setUp() {
        val signUpResult = userApplicationITUtils.signUp()
        testEmail = signUpResult.email
        testPassword = signUpResult.password
    }

    // ===========================================
    // 로그인 테스트
    // ===========================================

    @Test
    fun `로그인 성공`() {
        // given
        val request =
            AuthRequest.Login(
                email = testEmail,
                password = testPassword,
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AuthResponse.Login>(objectMapper, HttpStatus.OK) { data ->
            data.accessToken shouldNotBe null
            data.accessTokenExpiresAt shouldNotBe null
            data.refreshToken shouldNotBe null
            data.refreshTokenExpiresAt shouldNotBe null
            data.userId shouldNotBe null
            data.sessionId shouldNotBe null
        }
    }

    @Test
    fun `로그인 실패 - 잘못된 이메일`() {
        // given
        val request =
            AuthRequest.Login(
                email = "wrong@example.com",
                password = "password123",
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "USER_001")
    }

    @Test
    fun `로그인 실패 - 잘못된 비밀번호`() {
        // given
        val request =
            AuthRequest.Login(
                email = testEmail,
                password = "wrongpassword",
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        // 보안상 동일한 에러 반환 (USER_NOT_FOUND)
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "USER_001")
    }

    @Test
    fun `로그인 실패 - 이메일 형식 오류`() {
        // given
        val request =
            AuthRequest.Login(
                email = "invalid-email",
                password = "password123",
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") { error ->
            error.details shouldNotBe null
        }
    }

    @Test
    fun `로그인 실패 - 필수 필드 누락`() {
        // given
        val request = mapOf("email" to uniqueEmail())

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "BAD_REQUEST")
    }

    @Test
    fun `로그인 실패 - 빈 이메일`() {
        // given
        val request =
            AuthRequest.Login(
                email = "",
                password = "password123",
            )

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/login",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") { error ->
            error.details shouldNotBe null
        }
    }

    // ===========================================
    // 토큰 갱신 테스트
    // ===========================================

    @Test
    fun `토큰 갱신 성공`() {
        // given
        val authResult = authApplicationITUtils.login(testEmail, testPassword)
        val request = AuthRequest.Refresh(refreshToken = authResult.refreshToken)

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/refresh",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AuthResponse.Login>(objectMapper, HttpStatus.OK) { data ->
            data.accessToken shouldNotBe null
            data.accessTokenExpiresAt shouldNotBe null
            data.refreshToken shouldNotBe null
            data.refreshTokenExpiresAt shouldNotBe null
            data.userId shouldNotBe null
            data.sessionId shouldNotBe null
            // 새로운 토큰이 발급되었는지 확인
            data.accessToken shouldNotBe authResult.accessToken
        }
    }

    @Test
    fun `토큰 갱신 실패 - 잘못된 리프레시 토큰`() {
        // given
        val request = AuthRequest.Refresh(refreshToken = "invalid-refresh-token")

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/refresh",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "AUTH_003")
    }

    @Test
    fun `토큰 갱신 실패 - 리프레시 토큰 누락`() {
        // given
        val request = mapOf<String, Any>()

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/refresh",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "BAD_REQUEST")
    }

    @Test
    fun `토큰 갱신 실패 - 빈 리프레시 토큰`() {
        // given
        val request = AuthRequest.Refresh(refreshToken = "")

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/refresh",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") { error ->
            error.details shouldNotBe null
        }
    }

    // ===========================================
    // 로그아웃 테스트
    // ===========================================

    @Test
    fun `로그아웃 성공`() {
        // given
        val authResult = authApplicationITUtils.login(testEmail, testPassword)
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/logout",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AuthResponse.Logout>(objectMapper, HttpStatus.OK) { data ->
            data.message shouldBe "로그아웃되었습니다"
        }

        // 로그아웃 후 리프레시 토큰이 무효화되었는지 확인
        val refreshRequest = AuthRequest.Refresh(refreshToken = authResult.refreshToken)
        val refreshHeaders =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val refreshEntity = HttpEntity(objectMapper.writeValueAsString(refreshRequest), refreshHeaders)

        val refreshResponse =
            restTemplate.exchange(
                "${authBaseUrl()}/refresh",
                HttpMethod.POST,
                refreshEntity,
                String::class.java,
            )

        refreshResponse.assertError(objectMapper, HttpStatus.BAD_REQUEST, "AUTH_003")
    }

    @Test
    fun `로그아웃 실패 - 토큰 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/logout",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.UNAUTHORIZED, "COMMON_005")
    }

    @Test
    fun `로그아웃 실패 - 잘못된 토큰`() {
        // given
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer invalid-token")
            }
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response =
            restTemplate.exchange(
                "${authBaseUrl()}/logout",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.UNAUTHORIZED, "COMMON_005")
    }
}
