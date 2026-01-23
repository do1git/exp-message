package site.rahoon.message.monolithic.user.controller

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
import site.rahoon.message.monolithic.common.test.shouldHaveDetailField
import site.rahoon.message.monolithic.user.application.UserApplicationITUtils
import java.util.UUID

/**
 * User Controller E2E 테스트
 * 회원가입 및 사용자 정보 조회 API에 대한 전체 스택 테스트
 */
class UserControllerIT(
    private var restTemplate: TestRestTemplate,
    private var objectMapper: ObjectMapper,
    private val userApplicationITUtils: UserApplicationITUtils,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun baseUrl(): String = "http://localhost:$port/users"

    private lateinit var testEmail: String
    private lateinit var testPassword: String

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성 (Given)
        val signUpResult = userApplicationITUtils.signUp()
        testEmail = signUpResult.email
        testPassword = signUpResult.password
    }

    @Test
    fun `회원가입 성공`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request =
            UserRequest.SignUp(
                email = uniqueEmail,
                password = "password123",
                nickname = "testuser",
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<UserResponse.SignUp>(objectMapper, HttpStatus.CREATED) { user ->
            user.email shouldBe uniqueEmail
            user.nickname shouldBe "testuser"
            user.id shouldNotBe null
            user.createdAt shouldNotBe null
        }
    }

    @Test
    fun `회원가입 실패 - 이메일 형식 오류`() {
        // given
        val request =
            UserRequest.SignUp(
                email = "invalid-email",
                password = "password123",
                nickname = "testuser",
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
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
    fun `회원가입 실패 - 비밀번호 길이 부족`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request =
            UserRequest.SignUp(
                email = uniqueEmail,
                password = "short",
                nickname = "testuser",
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") { error ->
            error.shouldHaveDetailField("password")
        }
    }

    @Test
    fun `회원가입 실패 - 닉네임 길이 부족`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request =
            UserRequest.SignUp(
                email = uniqueEmail,
                password = "password123",
                nickname = "a",
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR") { error ->
            error.shouldHaveDetailField("nickname")
        }
    }

    @Test
    fun `회원가입 실패 - 필수 필드 누락`() {
        // given
        val request =
            mapOf(
                "email" to uniqueEmail(),
                // password와 nickname 누락
            )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        // 필수 필드 누락은 HttpMessageNotReadableException으로 처리되어 BAD_REQUEST 반환
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST, "BAD_REQUEST")
    }

    @Test
    fun `현재 사용자 정보 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.login(testEmail, testPassword)
        logger.info { "액세스 토큰: ${authResult.accessToken}" }
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/me",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        if (response.statusCode != HttpStatus.OK) {
            logger.error { "예상치 못한 상태 코드: ${response.statusCode}, 응답: ${response.body}" }
        }

        response.assertSuccess<UserResponse.Me>(objectMapper, HttpStatus.OK) { user ->
            user.email shouldBe testEmail
            user.nickname shouldBe "testuser"
            user.id shouldNotBe null
            user.createdAt shouldNotBe null
            user.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `현재 사용자 정보 조회 실패 - 토큰 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/me",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.UNAUTHORIZED, "COMMON_005")
    }

    @Test
    fun `현재 사용자 정보 조회 실패 - 잘못된 토큰`() {
        // given
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer invalid-token")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/me",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.UNAUTHORIZED, "COMMON_005")
    }
}
