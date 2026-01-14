package site.rahoon.message.__monolitic.chatroom.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldContain
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
import io.github.oshai.kotlinlogging.KotlinLogging
import site.rahoon.message.__monolitic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.common.test.assertError
import site.rahoon.message.__monolitic.common.test.assertSuccess

/**
 * ChatRoom Controller 통합 테스트
 * 채팅방 생성, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
class ChatRoomControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0
) : IntegrationTestBase() {

    private val logger = KotlinLogging.logger {}

    private fun baseUrl(): String = "http://localhost:$port/chat-rooms"

    /**
     * 채팅방을 생성하고 ID를 반환합니다.
     */
    private fun createChatRoom(accessToken: String, name: String): String {
        val request = ChatRoomRequest.Create(name = name)
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

        return response.assertSuccess<ChatRoomResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.name shouldBe name
        }.id
    }

    /**
     * 채팅방에 참여합니다.
     */
    private fun joinChatRoom(accessToken: String, chatRoomId: String) {
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
        }
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId/members",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        response.statusCode shouldBe HttpStatus.CREATED
    }

    // ===========================================
    // 채팅방 생성 테스트
    // ===========================================

    @Test
    fun `채팅방 생성 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val request = ChatRoomRequest.Create(name = "테스트 채팅방")
        val entity = HttpEntity(objectMapper.writeValueAsString(request), authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<ChatRoomResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
            data.name shouldBe "테스트 채팅방"
            data.id shouldNotBe null
            data.createdByUserId shouldNotBe null
            data.createdAt shouldNotBe null
            data.updatedAt shouldNotBe null
        }
    }

    @Test
    fun `채팅방 생성 실패 - 인증 없음`() {
        // given
        val request = ChatRoomRequest.Create(name = "테스트 채팅방")
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
    fun `채팅방 생성 실패 - 이름 누락`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val request = mapOf<String, Any>()
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
    // 채팅방 조회 테스트
    // ===========================================

    @Test
    fun `채팅방 조회 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken, "조회 테스트 채팅방")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<ChatRoomResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe chatRoomId
            data.name shouldBe "조회 테스트 채팅방"
        }
    }

    // ===========================================
    // 내가 참여한 채팅방 목록 조회 테스트
    // ===========================================

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 생성한 채팅방`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId1 = createChatRoom(authResult.accessToken, "채팅방 1")
        val chatRoomId2 = createChatRoom(authResult.accessToken, "채팅방 2")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<ChatRoomResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldNotBe 0
            val chatRoomIds = dataList.map { it.id }
            chatRoomIds shouldContain chatRoomId1
            chatRoomIds shouldContain chatRoomId2
        }
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 다른 사용자가 생성한 채팅방에 참여`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val memberAuth = authApplicationITUtils.signUpAndLogin()

        val chatRoomId = createChatRoom(creatorAuth.accessToken, "참여할 채팅방")
        joinChatRoom(memberAuth.accessToken, chatRoomId)
        val entity = HttpEntity<Nothing?>(null, memberAuth.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<ChatRoomResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldNotBe 0
            val chatRoomIds = dataList.map { it.id }
            chatRoomIds shouldContain chatRoomId

            val chatRoom = dataList.find { it.id == chatRoomId }
            chatRoom shouldNotBe null
            chatRoom!!.name shouldBe "참여할 채팅방"
        }
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 여러 채팅방에 참여`() {
        // given
        val userAuth = authApplicationITUtils.signUpAndLogin()
        val otherUserAuth = authApplicationITUtils.signUpAndLogin()

        val chatRoomId1 = createChatRoom(otherUserAuth.accessToken, "채팅방 A")
        val chatRoomId2 = createChatRoom(otherUserAuth.accessToken, "채팅방 B")

        joinChatRoom(userAuth.accessToken, chatRoomId1)
        joinChatRoom(userAuth.accessToken, chatRoomId2)
        val entity = HttpEntity<Nothing?>(null, userAuth.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<ChatRoomResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldNotBe 0
            val chatRoomIds = dataList.map { it.id }
            chatRoomIds shouldContain chatRoomId1
            chatRoomIds shouldContain chatRoomId2
        }
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 참여한 채팅방이 없을 때`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<List<ChatRoomResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList shouldHaveSize 0
        }
    }

    // ===========================================
    // 채팅방 수정 테스트
    // ===========================================

    @Test
    fun `채팅방 수정 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken, "원래 이름")

        val updateRequest = ChatRoomRequest.Update(name = "수정된 이름")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.PUT,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<ChatRoomResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.name shouldBe "수정된 이름"
        }
    }

    @Test
    fun `채팅방 수정 실패 - 권한 없음`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val otherUserAuth = authApplicationITUtils.signUpAndLogin()

        val chatRoomId = createChatRoom(creatorAuth.accessToken, "원래 이름")

        val updateRequest = ChatRoomRequest.Update(name = "수정 시도")
        val entity = HttpEntity(objectMapper.writeValueAsString(updateRequest), otherUserAuth.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.PUT,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.FORBIDDEN)
    }

    // ===========================================
    // 채팅방 삭제 테스트
    // ===========================================

    @Test
    fun `채팅방 삭제 성공`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken, "삭제될 채팅방")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.DELETE,
            entity,
            String::class.java
        )

        // then
        response.assertSuccess<ChatRoomResponse.Detail>(objectMapper, HttpStatus.OK) { data ->
            data.id shouldBe chatRoomId
        }

        // 삭제 후 조회 시 404 확인
        val getResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )
        getResponse.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    @Test
    fun `채팅방 삭제 실패 - 권한 없음`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val otherUserAuth = authApplicationITUtils.signUpAndLogin()

        val chatRoomId = createChatRoom(creatorAuth.accessToken, "삭제될 채팅방")
        val entity = HttpEntity<Nothing?>(null, otherUserAuth.headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.DELETE,
            entity,
            String::class.java
        )

        // then
        response.assertError(objectMapper, HttpStatus.FORBIDDEN)

        // 삭제되지 않았는지 확인
        val getEntity = HttpEntity<Nothing?>(null, creatorAuth.headers)
        val getResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )
        getResponse.statusCode shouldBe HttpStatus.OK
    }
}
