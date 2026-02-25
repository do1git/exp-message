package site.rahoon.message.monolithic.channelconversation.controller

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
 * ChannelConversation Controller 통합 테스트
 * 채널별 상담 세션 생성, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
class ChannelConversationControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun adminChannelsUrl(): String = "http://localhost:$port/admin/channels"

    private fun conversationsUrl(channelId: String): String = "http://localhost:$port/channels/$channelId/conversations"

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
     * 상담 세션을 생성하고 ID를 반환합니다.
     * customerId는 authInfo.userId로 자동 설정됩니다.
     */
    private fun createConversation(
        accessToken: String,
        channelId: String,
        name: String,
    ): String {
        val request = ChannelConversationRequest.Create(name = name)
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<ChannelConversationResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.name shouldBe name
                data.channelId shouldBe channelId
            }.id
    }

    // ===========================================
    // 상담 세션 생성 테스트
    // ===========================================

    @Test
    fun `상담 세션 생성 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "상담 세션 테스트 채널")
        val request = ChannelConversationRequest.Create(name = "상담 세션1")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelConversationResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.channelId shouldBe channelId
            data.customerId shouldBe authResult.userId
            data.name shouldBe "상담 세션1"
            data.id shouldNotBe null
            data.createdAt shouldNotBe null
            data.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `상담 세션 생성 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val request = ChannelConversationRequest.Create(name = "상담 세션1")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `상담 세션 생성 실패 - 이름 누락`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val request = mapOf<String, Any>()
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    // ===========================================
    // 상담 세션 조회 테스트 (ID)
    // ===========================================

    @Test
    fun `상담 세션 ID로 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "조회 테스트 채널")
        val conversationId = createConversation(authResult.accessToken, channelId, "조회 테스트 상담 세션")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelConversationResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe conversationId
            data.channelId shouldBe channelId
            data.name shouldBe "조회 테스트 상담 세션"
        }
    }

    @Test
    fun `상담 세션 ID로 조회 실패 - 존재하지 않는 상담 세션`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/non-existent-conversation-id",
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_CONVERSATION_001")
    }

    // ===========================================
    // 채널별 상담 세션 목록 조회 테스트
    // ===========================================

    @Test
    fun `채널별 상담 세션 목록 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "목록 테스트 채널")
        createConversation(authResult.accessToken, channelId, "상담 세션1")
        createConversation(authResult.accessToken, channelId, "상담 세션2")

        val otherUser = authApplicationITUtils.signUpAndLogin()
        createConversation(otherUser.accessToken, channelId, "상담 세션3")

        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelConversationResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(3)
            val names = dataList.map { it.name }.toSet()
            names shouldBe setOf("상담 세션1", "상담 세션2", "상담 세션3")
        }
    }

    @Test
    fun `채널별 상담 세션 목록 조회 - 상담 세션이 없을 때`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "빈 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                conversationsUrl(channelId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChannelConversationResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldHaveSize(0)
        }
    }

    // ===========================================
    // 상담 세션 수정 테스트
    // ===========================================

    @Test
    fun `상담 세션 수정 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "수정 테스트 채널")
        val conversationId = createConversation(authResult.accessToken, channelId, "원래 이름")

        val updateRequest = ChannelConversationRequest.Update(name = "수정된 이름")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelConversationResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.name shouldBe "수정된 이름"
            data.id shouldBe conversationId
        }
    }

    @Test
    fun `상담 세션 수정 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val conversationId = createConversation(authResult.accessToken, channelId, "원래 이름")

        val updateRequest = ChannelConversationRequest.Update(name = "수정 시도")
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `상담 세션 수정 실패 - 존재하지 않는 상담 세션`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val updateRequest = ChannelConversationRequest.Update(name = "수정 시도")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/non-existent-conversation-id",
                HttpMethod.PUT,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_CONVERSATION_001")
    }

    // ===========================================
    // 상담 세션 삭제 테스트
    // ===========================================

    @Test
    fun `상담 세션 삭제 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "삭제 테스트 채널")
        val conversationId = createConversation(authResult.accessToken, channelId, "삭제될 상담 세션")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChannelConversationResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe conversationId
        }

        // 삭제 후 조회 시 404 확인
        val getResponse =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.GET,
                entity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `상담 세션 삭제 실패 - 인증 없음`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val conversationId = createConversation(authResult.accessToken, channelId, "삭제될 상담 세션")
        val entity = HttpEntity<Nothing?>(null, HttpHeaders())

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/$conversationId",
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
                "${conversationsUrl(channelId)}/$conversationId",
                HttpMethod.GET,
                getEntity,
                String::class.java,
            )
        getResponse.statusCode shouldBe HttpStatus.OK
    }

    @Test
    fun `상담 세션 삭제 실패 - 존재하지 않는 상담 세션`() {
        // given
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val channelId = createChannel(authResult.accessToken, "테스트 채널")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${conversationsUrl(channelId)}/non-existent-conversation-id",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND, "CHANNEL_CONVERSATION_001")
    }
}
