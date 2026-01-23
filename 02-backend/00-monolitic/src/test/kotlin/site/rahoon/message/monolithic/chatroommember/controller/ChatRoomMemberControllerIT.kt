package site.rahoon.message.monolithic.chatroommember.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
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
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess

/**
 * ChatRoomMember Controller 통합 테스트
 * 채팅방 참가, 나가기, 멤버 목록 조회 API에 대한 전체 스택 테스트
 */
class ChatRoomMemberControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger {}

    private fun baseUrl(chatRoomId: String): String = "http://localhost:$port/chat-rooms/$chatRoomId/members"

    private fun chatRoomBaseUrl(): String = "http://localhost:$port/chat-rooms"

    /**
     * 채팅방을 생성하고 ID를 반환합니다.
     */
    private fun createChatRoom(
        accessToken: String,
        name: String = "테스트 채팅방",
    ): String {
        val request = ChatRoomRequest.Create(name = name)
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)

        val response =
            restTemplate.exchange(
                chatRoomBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        return response
            .assertSuccess<ChatRoomResponse.Create>(objectMapper, HttpStatus.CREATED) { data ->
                data.name shouldBe name
            }.id
    }

    /**
     * 채팅방에 참여합니다.
     */
    private fun joinChatRoom(
        accessToken: String,
        chatRoomId: String,
    ) {
        val headers =
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
            }
        val entity = HttpEntity<Nothing?>(null, headers)

        val response =
            restTemplate.exchange(
                "${baseUrl(chatRoomId)}/me",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        response.statusCode shouldBe HttpStatus.CREATED
    }

    // ===========================================
    // 채팅방 참가 테스트
    // ===========================================

    @Test
    fun `채팅방 참가 성공`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val memberAuth = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(creatorAuth.accessToken, "참가 테스트 채팅방")
        val entity = HttpEntity<Nothing?>(null, memberAuth.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl(chatRoomId)}/me",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChatRoomMemberResponse.Member>(objectMapper, HttpStatus.CREATED) { data ->
            data.chatRoomId shouldBe chatRoomId
            data.userId shouldNotBe null
            data.joinedAt shouldNotBe null
        }
    }

    @Test
    fun `채팅방 참가 실패 - 이미 참가한 멤버`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken, "중복 참가 테스트 채팅방")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when - 이미 생성자로 자동 참가되어 있으므로 다시 참가 시도
        val response =
            restTemplate.exchange(
                "${baseUrl(chatRoomId)}/me",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `채팅방 참가 실패 - 존재하지 않는 채팅방`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val nonExistentChatRoomId = "non-existent-chat-room-id"
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl(nonExistentChatRoomId)}/me",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        // then
        response.statusCode shouldBe HttpStatus.NOT_FOUND
    }

    // ===========================================
    // 채팅방 나가기 테스트
    // ===========================================

    @Test
    fun `채팅방 나가기 성공`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val memberAuth = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(creatorAuth.accessToken, "나가기 테스트 채팅방")

        joinChatRoom(memberAuth.accessToken, chatRoomId)
        val entity = HttpEntity<Nothing?>(null, memberAuth.headers)

        // when
        val response =
            restTemplate.exchange(
                "${baseUrl(chatRoomId)}/me",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<ChatRoomMemberResponse.Member>(objectMapper, HttpStatus.OK) { data ->
            data.chatRoomId shouldBe chatRoomId
        }
    }

    @Test
    fun `채팅방 나가기 실패 - 멤버가 아님`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val otherUserAuth = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(creatorAuth.accessToken, "나가기 실패 테스트 채팅방")
        val entity = HttpEntity<Nothing?>(null, otherUserAuth.headers)

        // when - 참가하지 않은 사용자가 나가기 시도
        val response =
            restTemplate.exchange(
                "${baseUrl(chatRoomId)}/me",
                HttpMethod.DELETE,
                entity,
                String::class.java,
            )

        // then
        response.assertError(objectMapper, HttpStatus.BAD_REQUEST)
    }

    // ===========================================
    // 채팅방 멤버 목록 조회 테스트
    // ===========================================

    @Test
    fun `채팅방 멤버 목록 조회 성공`() {
        // given
        val creatorAuth = authApplicationITUtils.signUpAndLogin()
        val member2Auth = authApplicationITUtils.signUpAndLogin()
        val member3Auth = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(creatorAuth.accessToken, "멤버 목록 테스트 채팅방")

        joinChatRoom(member2Auth.accessToken, chatRoomId)
        joinChatRoom(member3Auth.accessToken, chatRoomId)
        val entity = HttpEntity<Nothing?>(null, creatorAuth.headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(chatRoomId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChatRoomMemberResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldBeGreaterThanOrEqual 3
        }
    }

    @Test
    fun `채팅방 생성 시 생성자가 자동으로 멤버로 추가됨`() {
        // given
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken, "자동 멤버 추가 테스트 채팅방")
        val entity = HttpEntity<Nothing?>(null, authResult.headers)

        // when
        val response =
            restTemplate.exchange(
                baseUrl(chatRoomId),
                HttpMethod.GET,
                entity,
                String::class.java,
            )

        // then
        response.assertSuccess<List<ChatRoomMemberResponse.ListItem>>(objectMapper, HttpStatus.OK) { dataList ->
            dataList.size shouldBe 1
        }
    }
}
