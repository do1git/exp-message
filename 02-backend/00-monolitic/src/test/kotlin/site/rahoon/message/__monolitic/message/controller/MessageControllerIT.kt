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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import site.rahoon.message.__monolitic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.common.test.assertError
import site.rahoon.message.__monolitic.common.test.assertSuccess
import site.rahoon.message.__monolitic.common.test.assertSuccessPage

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

    private fun assertSortedByCreatedAtDescThenIdDesc(items: List<MessageResponse.Detail>) {
        for (i in 0 until items.size - 1) {
            val a = items[i]
            val b = items[i + 1]

            when {
                a.createdAt.isAfter(b.createdAt) -> Unit
                a.createdAt.isEqual(b.createdAt) -> {
                    // id desc
                    (a.id >= b.id) shouldBe true
                }
                else -> {
                    // createdAt desc 위배
                    false shouldBe true
                }
            }
        }
    }

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
        response.assertSuccessPage<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { dataList, pageInfo ->
            dataList.size shouldBeGreaterThanOrEqual 2
            // 최신순 정렬 확인
            dataList[0].content shouldBe "두 번째 메시지"
            dataList[1].content shouldBe "첫 번째 메시지"
            pageInfo.limit shouldBe 20
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
        response.assertSuccessPage<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { dataList, pageInfo ->
            dataList.shouldBeEmpty()
            pageInfo.nextCursor shouldBe null
            pageInfo.limit shouldBe 20
        }
    }

    @Test
    fun `채팅방별 메시지 목록 조회 - nextCursor로 페이지가 끊김 없이 이어짐`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)

        // 메시지 5개 생성(여러 페이지로 나뉘도록)
        repeat(5) { idx ->
            sendMessage(authResult.accessToken, chatRoomId, "페이지네이션 메시지 ${idx + 1}")
            // 동일 createdAt(저장 정밀도)로 인한 플래키를 줄이기 위해 아주 짧게 간격을 둠
            Thread.sleep(5)
        }

        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when - 1페이지(limit=2)
        val page1Response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId&limit=2",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        val page1 = page1Response.assertSuccessPage<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { dataList, pageInfo ->
            dataList.size shouldBe 2
            pageInfo.limit shouldBe 2
            pageInfo.nextCursor shouldNotBe null
            assertSortedByCreatedAtDescThenIdDesc(dataList)
        }

        // when - 2페이지(nextCursor)
        val encodedCursor1 = URLEncoder.encode(page1.pageInfo.nextCursor!!, StandardCharsets.UTF_8)
        val page2Response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId&limit=2&cursor=$encodedCursor1",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        val page2 = page2Response.assertSuccessPage<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { dataList, pageInfo ->
            dataList.size shouldBe 2
            pageInfo.limit shouldBe 2
            pageInfo.nextCursor shouldNotBe null
            assertSortedByCreatedAtDescThenIdDesc(dataList)
        }

        // when - 3페이지(마지막 페이지, 남은 1개)
        val encodedCursor2 = URLEncoder.encode(page2.pageInfo.nextCursor!!, StandardCharsets.UTF_8)
        val page3Response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId&limit=2&cursor=$encodedCursor2",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        val page3 = page3Response.assertSuccessPage<MessageResponse.Detail>(objectMapper, HttpStatus.OK) { dataList, pageInfo ->
            dataList.size shouldBe 1
            pageInfo.limit shouldBe 2
            // 마지막 페이지(비어있지 않아도) nextCursor는 null이어야 함
            pageInfo.nextCursor shouldBe null
            assertSortedByCreatedAtDescThenIdDesc(dataList)
        }

        // 페이지 연속성 검증(중복/누락 없이 이어짐)
        val all = page1.data!! + page2.data!! + page3.data!!
        all.map { it.id }.distinct().size shouldBe 5
        assertSortedByCreatedAtDescThenIdDesc(all)

        val page1Ids = page1.data!!.map { it.id }.toSet()
        val page2Ids = page2.data!!.map { it.id }.toSet()
        val page3Ids = page3.data!!.map { it.id }.toSet()
        (page1Ids.intersect(page2Ids).isEmpty()) shouldBe true
        (page1Ids.intersect(page3Ids).isEmpty()) shouldBe true
        (page2Ids.intersect(page3Ids).isEmpty()) shouldBe true
    }
}
