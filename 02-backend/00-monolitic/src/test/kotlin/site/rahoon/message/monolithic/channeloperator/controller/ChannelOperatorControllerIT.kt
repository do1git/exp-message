package site.rahoon.message.monolithic.channeloperator.controller

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
import org.springframework.http.MediaType
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.channel.controller.AdminChannelRequest
import site.rahoon.message.monolithic.channel.controller.AdminChannelResponse
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess

/**
 * ChannelOperator Controller 통합 테스트
 * 채널 상담원 등록, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
class ChannelOperatorControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun adminChannelsUrl(): String = "http://localhost:$port/admin/channels"

    private fun operatorsUrl(channelId: String): String = "http://localhost:$port/channels/$channelId/operators"

    /**
     * 채널을 생성하고 ID를 반환합니다. (Admin API 사용)
     */
    private fun createChannel(
        adminAccessToken: String,
        name: String,
    ): String {
        val request = AdminChannelRequest.Create(name = name)
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $adminAccessToken")
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                adminChannelsUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<AdminChannelResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.name shouldBe name
            }.id
    }

    /**
     * 상담원을 등록하고 ID를 반환합니다.
     */
    private fun createOperator(
        accessToken: String,
        channelId: String,
        userId: String,
        nickname: String,
    ): String {
        val request = ChannelOperatorRequest.Create(userId = userId, nickname = nickname)
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<ChannelOperatorResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.nickname shouldBe nickname
                data.userId shouldBe userId
            }.id
    }

    // ===========================================
    // 상담원 등록 테스트
    // ===========================================

    @Test
    fun `상담원 등록 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "상담원 테스트 채널")
        val request = ChannelOperatorRequest.Create(userId = authResult.userId, nickname = "상담원1")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelOperatorResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.channelId shouldBe channelId
            data.userId shouldBe authResult.userId
            data.nickname shouldBe "상담원1"
            data.id shouldNotBe null
            data.createdAt shouldNotBe null
            data.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `상담원 등록 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val request = ChannelOperatorRequest.Create(userId = authResult.userId, nickname = "상담원1")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `상담원 등록 실패 - 닉네임 누락`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val request = mapOf("userId" to authResult.userId)
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `상담원 등록 실패 - 이미 등록된 사용자`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        createOperator(authResult.accessToken, channelId, authResult.userId, "상담원1")

        val request = ChannelOperatorRequest.Create(userId = authResult.userId, nickname = "상담원2")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.CONFLICT, "CHANNEL_OPERATOR_005")
    }

    // ===========================================
    // 상담원 조회 테스트 (ID)
    // ===========================================

    @Test
    fun `상담원 ID로 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "조회 테스트 채널")
        val operatorId = createOperator(authResult.accessToken, channelId, authResult.userId, "조회 테스트 상담원")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelOperatorResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe operatorId
            data.channelId shouldBe channelId
            data.nickname shouldBe "조회 테스트 상담원"
        }
    }

    @Test
    fun `상담원 ID로 조회 실패 - 존재하지 않는 상담원`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/non-existent-operator-id",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_OPERATOR_001")
    }

    // ===========================================
    // 채널별 상담원 목록 조회 테스트
    // ===========================================

    @Test
    fun `채널별 상담원 목록 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "목록 테스트 채널")
        createOperator(authResult.accessToken, channelId, authResult.userId, "상담원1")

        val otherUser = authApplicationITUtils.signUpAndLogin()
        createOperator(authResult.accessToken, channelId, otherUser.userId, "상담원2")

        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelOperatorResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(2)
            val nicknames = dataList.map { it.nickname }.toSet()
            nicknames shouldBe setOf("상담원1", "상담원2")
        }
    }

    @Test
    fun `채널별 상담원 목록 조회 - 상담원이 없을 때`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "빈 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                operatorsUrl(channelId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelOperatorResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(0)
        }
    }

    // ===========================================
    // 상담원 수정 테스트
    // ===========================================

    @Test
    fun `상담원 수정 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "수정 테스트 채널")
        val operatorId = createOperator(authResult.accessToken, channelId, authResult.userId, "원래 닉네임")

        val updateRequest = ChannelOperatorRequest.Update(nickname = "수정된 닉네임")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelOperatorResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.nickname shouldBe "수정된 닉네임"
            data.id shouldBe operatorId
        }
    }

    @Test
    fun `상담원 수정 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val operatorId = createOperator(authResult.accessToken, channelId, authResult.userId, "원래 닉네임")

        val updateRequest = ChannelOperatorRequest.Update(nickname = "수정 시도")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `상담원 수정 실패 - 존재하지 않는 상담원`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val updateRequest = ChannelOperatorRequest.Update(nickname = "수정 시도")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/non-existent-operator-id",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_OPERATOR_001")
    }

    // ===========================================
    // 상담원 삭제 테스트
    // ===========================================

    @Test
    fun `상담원 삭제 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "삭제 테스트 채널")
        val operatorId = createOperator(authResult.accessToken, channelId, authResult.userId, "삭제될 상담원")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelOperatorResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe operatorId
        }

        // 삭제 후 조회 시 404 확인
        val getResponse =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `상담원 삭제 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val operatorId = createOperator(authResult.accessToken, channelId, authResult.userId, "삭제될 상담원")
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED

        // 삭제되지 않았는지 확인
        val getEntity = HttpEntity<Nothing?>(null, authResult.headers)
        val getResponse =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/$operatorId",
                HttpMethod.GET,
                getEntity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `상담원 삭제 실패 - 존재하지 않는 상담원`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${operatorsUrl(channelId)}/non-existent-operator-id",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_OPERATOR_001")
    }
}
