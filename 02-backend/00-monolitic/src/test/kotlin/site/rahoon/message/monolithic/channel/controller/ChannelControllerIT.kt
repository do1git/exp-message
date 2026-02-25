package site.rahoon.message.monolithic.channel.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess

/**
 * Channel Controller 통합 테스트
 * 채널 조회 API (ID, apiKey)에 대한 전체 스택 테스트
 */
class ChannelControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun baseUrl(): String = "http://localhost:$port/channels"

    private fun adminBaseUrl(): String = "http://localhost:$port/admin/channels"

    /**
     * 채널을 생성하고 ID를 반환합니다. (조회 테스트 setup용)
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
    // 채널 조회 테스트 (ID)
    // ===========================================

    @Test
    fun `채널 ID로 조회 성공`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "조회 테스트 채널")
        val entity = HttpEntity<Nothing?>(null, adminAuthResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe channelId
            data.name shouldBe "조회 테스트 채널"
            data.apiKey shouldNotBe null
        }
    }

    @Test
    fun `채널 ID로 조회 실패 - 존재하지 않는 채널`() {
        // given
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}/non-existent-id",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }

    // ===========================================
    // 채널 조회 테스트 (apiKey)
    // ===========================================

    @Test
    fun `채널 apiKey로 조회 성공`() {
        // given
        val adminAuthResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(adminAuthResult, "apiKey 조회 테스트 채널")
        val getByIdEntity = HttpEntity<Nothing?>(null, adminAuthResult.headers)
        val getByIdResponse =
            restTemplate.exchange(
                "${baseUrl()}/$channelId",
                HttpMethod.GET,
                getByIdEntity,
                String::class.java,
            )
        val channelDetail = getByIdResponse.assertSuccess<ChannelResponse.Detail>(objectMapper, HttpStatus.OK) { }
        val apiKey = channelDetail.apiKey!!

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}?apiKey=$apiKey",
                HttpMethod.GET,
                getByIdEntity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelResponse.Detail>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(1)
            dataList[0].id shouldBe channelId
            dataList[0].name shouldBe "apiKey 조회 테스트 채널"
            dataList[0].apiKey shouldBe apiKey
        }
    }

    @Test
    fun `채널 apiKey로 조회 - 존재하지 않는 apiKey`() {
        // given
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl()}?apiKey=ch_invalid_key_12345",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_001")
    }
}
