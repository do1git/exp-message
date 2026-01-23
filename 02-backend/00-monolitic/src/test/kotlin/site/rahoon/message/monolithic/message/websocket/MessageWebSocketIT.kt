package site.rahoon.message.monolithic.message.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertSuccess
import site.rahoon.message.monolithic.message.application.MessageEvent
import site.rahoon.message.monolithic.message.controller.MessageRequest
import site.rahoon.message.monolithic.message.controller.MessageResponse
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * WebSocket(STOMP) 구독 후 POST /messages 시 /topic/chat-rooms/{id}/messages 로
 * 브로드캐스트되는지 통합 테스트.
 */
class MessageWebSocketIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    @Test
    fun `POST messages 후 구독자에게 MessageResponse Detail 브로드캐스트`() {
        // given: 로그인, 채팅방 생성
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"

        // STOMP 클라이언트
        val receives = ArrayBlockingQueue<MessageEvent.Created>(1)
        val stompClient = createStompClient()
        val session: StompSession =
            stompClient
                .connect(wsUrl, object : StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS)

        // 구독
        session.subscribe(
            "/topic/chat-rooms/$chatRoomId/messages",
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): java.lang.reflect.Type = MessageEvent.Created::class.java

                override fun handleFrame(
                    headers: StompHeaders,
                    payload: Any?,
                ) {
                    if (payload is MessageEvent.Created) {
                        receives.offer(payload)
                    }
                }
            },
        )

        // 구독 등록 대기
        Thread.sleep(200)

        // when: 메시지 전송
        val content = "웹소켓 IT 메시지"
        val createRequest = MessageRequest.Create(chatRoomId = chatRoomId, content = content)
        val entity =
            HttpEntity(
                objectMapper.writeValueAsString(createRequest),
                HttpHeaders().apply {
                    set("Authorization", "Bearer ${authResult.accessToken}")
                    contentType = MediaType.APPLICATION_JSON
                },
            )
        restTemplate
            .exchange(
                "http://localhost:$port/messages",
                HttpMethod.POST,
                entity,
                String::class.java,
            ).assertSuccess<MessageResponse.Create>(objectMapper, org.springframework.http.HttpStatus.CREATED) { data ->
                data.content shouldBe content
            }

        // then: 구독자에게 수신
        val received = receives.poll(5, TimeUnit.SECONDS).shouldNotBeNull()
        received.content shouldBe content
        received.chatRoomId shouldBe chatRoomId
    }

    private fun createStompClient(): WebSocketStompClient {
        val transports = listOf(WebSocketTransport(StandardWebSocketClient()))
        val sockJsClient = SockJsClient(transports)
        val stompClient = WebSocketStompClient(sockJsClient)
        val converter = MappingJackson2MessageConverter()
        converter.objectMapper = objectMapper
        stompClient.setMessageConverter(converter)
        return stompClient
    }

    private fun createChatRoom(accessToken: String): String {
        val request = ChatRoomRequest.Create(name = "WS테스트방")
        val entity =
            HttpEntity(
                objectMapper.writeValueAsString(request),
                HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                    contentType = MediaType.APPLICATION_JSON
                },
            )
        val res =
            restTemplate.exchange(
                "http://localhost:$port/chat-rooms",
                HttpMethod.POST,
                entity,
                String::class.java,
            )
        return res
            .assertSuccess<ChatRoomResponse.Create>(objectMapper, org.springframework.http.HttpStatus.CREATED) { }
            .id
    }
}
