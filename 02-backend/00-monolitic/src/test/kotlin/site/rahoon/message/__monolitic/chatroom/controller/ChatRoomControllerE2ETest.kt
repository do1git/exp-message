package site.rahoon.message.__monolitic.chatroom.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.slf4j.LoggerFactory
import site.rahoon.message.__monolitic.authtoken.controller.AuthRequest
import site.rahoon.message.__monolitic.user.controller.UserRequest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ChatRoom Controller E2E 테스트
 * 채팅방 생성, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatRoomControllerE2ETest {

    companion object {
        @Container
        @JvmStatic
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val logger = LoggerFactory.getLogger(ChatRoomControllerE2ETest::class.java)

    private fun baseUrl(): String = "http://localhost:$port/chat-rooms"
    private fun authBaseUrl(): String = "http://localhost:$port/auth"
    private fun userBaseUrl(): String = "http://localhost:$port/users"

    /**
     * 로그인하여 액세스 토큰을 받아옵니다.
     */
    private fun loginAndGetToken(email: String = "test@example.com", password: String = "password123"): String {
        // 먼저 회원가입 시도
        val signUpRequest = UserRequest.SignUp(
            email = email,
            password = password,
            nickname = "testuser"
        )

        val signUpHeaders = HttpHeaders()
        signUpHeaders.contentType = MediaType.APPLICATION_JSON
        val signUpRequestBody = objectMapper.writeValueAsString(signUpRequest)
        val signUpEntity = HttpEntity(signUpRequestBody, signUpHeaders)

        try {
            restTemplate.exchange(
                userBaseUrl(),
                HttpMethod.POST,
                signUpEntity,
                String::class.java
            )
        } catch (e: Exception) {
            // 이미 존재하는 경우 무시
        }

        // 로그인
        val request = AuthRequest.Login(
            email = email,
            password = password
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode, "로그인 실패: ${response.body}")
        assertNotNull(response.body, "로그인 응답이 null입니다")

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(responseMap["success"] as Boolean, "로그인 응답이 실패했습니다: ${response.body}")
        
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap, "로그인 응답의 data가 null입니다")
        
        val accessToken = dataMap.get("accessToken") as? String
        assertNotNull(accessToken, "액세스 토큰이 null입니다")
        return accessToken
    }

    @Test
    fun `채팅방 생성 성공`() {
        // given
        val accessToken = loginAndGetToken()
        val request = ChatRoomRequest.Create(
            name = "테스트 채팅방"
        )

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.CREATED, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals("테스트 채팅방", dataMap["name"])
        assertNotNull(dataMap["id"])
        assertNotNull(dataMap["createdByUserId"])
        assertNotNull(dataMap["createdAt"])
        assertNotNull(dataMap["updatedAt"])
    }

    @Test
    fun `채팅방 생성 실패 - 인증 없음`() {
        // given
        val request = ChatRoomRequest.Create(
            name = "테스트 채팅방"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `채팅방 생성 실패 - 이름 누락`() {
        // given
        val accessToken = loginAndGetToken()
        val request = mapOf<String, Any>() // name 필드 누락

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 먼저 채팅방 생성
        val createRequest = ChatRoomRequest.Create(
            name = "조회 테스트 채팅방"
        )

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createRequestBody = objectMapper.writeValueAsString(createRequest)
        val createEntity = HttpEntity(createRequestBody, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(chatRoomId, dataMap["id"])
        assertEquals("조회 테스트 채팅방", dataMap["name"])
    }

    @Test
    fun `내가 생성한 채팅방 목록 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 2개 생성
        val createRequest1 = ChatRoomRequest.Create(name = "채팅방 1")
        val createRequest2 = ChatRoomRequest.Create(name = "채팅방 2")

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON

        val entity1 = HttpEntity(objectMapper.writeValueAsString(createRequest1), headers)
        val entity2 = HttpEntity(objectMapper.writeValueAsString(createRequest2), headers)

        restTemplate.exchange(baseUrl(), HttpMethod.POST, entity1, String::class.java)
        restTemplate.exchange(baseUrl(), HttpMethod.POST, entity2, String::class.java)

        // when
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataList = responseMap["data"] as? List<*>
        assertNotNull(dataList)
        assertTrue(dataList.size >= 2, "채팅방 목록이 2개 이상이어야 합니다")
    }

    @Test
    fun `채팅방 수정 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "원래 이름")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 수정
        val updateRequest = ChatRoomRequest.Update(name = "수정된 이름")

        val updateHeaders = HttpHeaders()
        updateHeaders.set("Authorization", "Bearer $accessToken")
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(objectMapper.writeValueAsString(updateRequest), updateHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.PUT,
            updateEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals("수정된 이름", dataMap["name"])
    }

    @Test
    fun `채팅방 삭제 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "삭제될 채팅방")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 삭제
        val deleteHeaders = HttpHeaders()
        deleteHeaders.set("Authorization", "Bearer $accessToken")
        val deleteEntity = HttpEntity<Nothing?>(null, deleteHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.DELETE,
            deleteEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(chatRoomId, dataMap["id"])

        // 삭제 후 조회 시 404 확인
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val getResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
    }
}
