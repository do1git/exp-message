package site.rahoon.message.__monolitic.message.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
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
import io.github.oshai.kotlinlogging.KotlinLogging
import site.rahoon.message.__monolitic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.common.test.assertError
import site.rahoon.message.__monolitic.common.test.assertSuccess

/**
 * Message Controller 통합 테스트
 * 메시지 전송, 조회 API에 대한 전체 스택 테스트
 */
class MessageControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0
) : IntegrationTestBase() {

    private val logger = KotlinLogging.logger {}

    private fun baseUrl(): String = "http://localhost:$port/messages"
    private fun chatRoomBaseUrl(): String = "http://localhost:$port/chat-rooms"

    /**
     * 채팅방을 생성하고 ID를 반환합니다.
     */
    private fun createChatRoom(accessToken: String, name: String = "테스트 채팅방"): String {
        val request = ChatRoomRequest.Create(name = name)
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response = restTemplate.exchange(
            chatRoomBaseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        return response.assertSuccess<ChatRoomResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.name shouldBe name
        }.id
    }

    /**
     * 메시지를 전송하고 ID를 반환합니다.
     */
    private fun sendMessage(accessToken: String, chatRoomId: String, content: String): String {
        val request = MessageRequest.Create(chatRoomId = chatRoomId, content = content)
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        return response.assertSuccess<MessageResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.content shouldBe content
            data.chatRoomId shouldBe chatRoomId
        }.id
    }

    // ===========================================
    // 메시지 전송 테스트
    // ===========================================

    @Test
    fun `메시지 전송 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val request = MessageRequest.Create(chatRoomId = chatRoomId, content = "안녕하세요!")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<MessageResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.chatRoomId shouldBe chatRoomId
            data.content shouldBe "안녕하세요!"
            data.id shouldNotBe null
            data.userId shouldNotBe null
            data.createdAt shouldNotBe null
        }
    }

    @Test
    fun `메시지 전송 실패 - 인증 없음`() {
        // given
        val request = MessageRequest.Create(chatRoomId = "test-chatroom-id", content = "안녕하세요!")
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `메시지 전송 실패 - 채팅방이 존재하지 않음`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val request = MessageRequest.Create(chatRoomId = "non-existent-chatroom-id", content = "안녕하세요!")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `메시지 전송 실패 - 채팅방 멤버가 아님`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val otherUserAuth = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(creatorAuth.accessToken)

        val request = MessageRequest.Create(chatRoomId = chatRoomId, content = "안녕하세요!")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), otherUserAuth.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.FORBIDDEN)
    }

    @Test
    fun `메시지 전송 실패 - 내용 누락`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val request = mapOf<String, Any>("chatRoomId" to chatRoomId)
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    // ===========================================
    // 메시지 조회 테스트
    // ===========================================

    @Test
    fun `메시지 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val messageId = sendMessage(authResult.accessToken, chatRoomId, "조회 테스트 메시지")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$messageId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe messageId
            data.content shouldBe "조회 테스트 메시지"
            data.chatRoomId shouldBe chatRoomId
        }
    }

    @Test
    fun `메시지 조회 실패 - 메시지가 존재하지 않음`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/non-existent-message-id",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `메시지 조회 실패 - 인증 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/test-message-id",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    // ===========================================
    // 채팅방별 메시지 목록 조회 테스트
    // ===========================================

    @Test
    fun `채팅방별 메시지 목록 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)

        sendMessage(authResult.accessToken, chatRoomId, "첫 번째 메시지")
        sendMessage(authResult.accessToken, chatRoomId, "두 번째 메시지")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<MessageResponse.Detail>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldBeGreaterThanOrEqual 2
            // 최신순 정렬 확인
            dataList[0].content shouldBe "두 번째 메시지"
            dataList[1].content shouldBe "첫 번째 메시지"
        }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 채팅방이 존재하지 않음`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=non-existent-chatroom-id",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.NOT_FOUND)
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 인증 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=test-chatroom-id",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.statusCode shouldBe HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `채팅방별 메시지 목록 조회 - 빈 목록`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<MessageResponse.Detail>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.shouldBeEmpty()
        }
    }
}
