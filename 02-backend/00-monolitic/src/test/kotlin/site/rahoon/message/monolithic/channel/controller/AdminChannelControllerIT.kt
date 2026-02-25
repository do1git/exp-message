package site.rahoon.message.monolithic.channel.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

/**
 * Admin Channel Controller 통합 테스트
 * 채널 생성, 수정, 삭제 API (ADMIN 전용)에 대한 전체 스택 테스트
 */
class AdminChannelControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun adminBaseUrl(): String = "http://localhost:$port/admin/channels"

    private fun channelsUrl(): String = "http://localhost:$port/channels"

    /**
     * 채널을 생성하고 ID를 반환합니다.
     */
    private fun createChannel(
        adminAuthResult: AuthApplicationITUtils.AuthResult,
        name: String,
    ): String {
        val request = AdminChannelRequest.Create(name = name)
        val entity = HttpEntity(objectMapper.writeValueAsString(request), adminAuthResult.headers)

        val response =
            restTemplate.exchange(
                adminBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<AdminChannelResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.name shouldBe name
            }.id
    }

    // ===========================================
    // 채널 생성 테스트
    // ===========================================

    @Test
    fun `채널 생성 성공 - ADMIN`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val request = AdminChannelRequest.Create(name = "테스트 채널")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                adminBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AdminChannelResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.name shouldBe "테스트 채널"
            data.id shouldNotBe null
            data.apiKey shouldNotBe null
            data.apiKey!!.startsWith("ch_") shouldBe true
            data.createdAt shouldNotBe null
            data.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `채널 생성 실패 - USER 역할`() {
        // given
        val userAuthResult = authApplicationITUtils.signUpAndLogin()
        val request = AdminChannelRequest.Create(name = "테스트 채널")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), userAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                adminBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.FORBIDDEN
    }

    @Test
    fun `채널 생성 실패 - 인증 없음`() {
        // given
        val request = AdminChannelRequest.Create(name = "테스트 채널")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                adminBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `채널 생성 실패 - 이름 누락`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val request = mapOf<String, Any>()
        val entity = HttpEntity(objectMapper.writeValueAsString(request), adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                adminBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    // ===========================================
    // 채널 수정 테스트
    // ===========================================

    @Test
    fun `채널 수정 성공 - ADMIN`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "원래 이름")

        val updateRequest = AdminChannelRequest.Update(name = "수정된 이름")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/$channelId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AdminChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.name shouldBe "수정된 이름"
            data.id shouldBe channelId
        }
    }

    @Test
    fun `채널 수정 실패 - 인증 없음`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "원래 이름")

        val updateRequest = AdminChannelRequest.Update(name = "수정 시도")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), headers)

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/$channelId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `채널 수정 실패 - 존재하지 않는 채널`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val updateRequest = AdminChannelRequest.Update(name = "수정 시도")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/non-existent-id",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }

    // ===========================================
    // 채널 삭제 테스트
    // ===========================================

    @Test
    fun `채널 삭제 성공 - ADMIN`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "삭제될 채널")
        val entity = HttpEntity<Nothing?>(null, adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/$channelId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<AdminChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe channelId
        }

        // 삭제 후 조회 시 404 확인
        val getResponse =
            restTemplate.exchange(
                "${channelsUrl()}/$channelId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `채널 삭제 실패 - 인증 없음`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "삭제될 채널")
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/$channelId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED

        // 삭제되지 않았는지 확인
        val getEntity = HttpEntity<Nothing?>(null, adminAuthResult.headers)
        val getResponse =
            restTemplate.exchange(
                "${channelsUrl()}/$channelId",
                HttpMethod.GET,
                getEntity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `채널 삭제 실패 - 존재하지 않는 채널`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val entity = HttpEntity<Nothing?>(null, adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${adminBaseUrl()}/non-existent-id",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }
}
