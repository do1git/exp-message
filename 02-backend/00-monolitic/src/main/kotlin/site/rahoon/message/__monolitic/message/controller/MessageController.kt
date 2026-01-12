package site.rahoon.message.__monolitic.message.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import site.rahoon.message.__monolitic.common.global.utils.AuthInfo
import site.rahoon.message.__monolitic.common.global.utils.AuthInfoAffect
import site.rahoon.message.__monolitic.message.application.MessageApplicationService
import site.rahoon.message.__monolitic.message.application.MessageCriteria

/**
 * 메시지 관련 Controller
 * 메시지 전송, 조회 API 제공
 */
@RestController
@RequestMapping("/messages")
class MessageController(
    private val messageApplicationService: MessageApplicationService
) {

    /**
     * 메시지 전송
     * POST /messages
     */
    @PostMapping
    @AuthInfoAffect(required = true)
    fun create(
        @Valid @RequestBody request: MessageRequest.Create,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<MessageResponse.Create>> {
        val criteria = request.toCriteria(authInfo.userId)
        val message = messageApplicationService.create(criteria)
        val response = MessageResponse.Create.from(message)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(response)
        )
    }

    /**
     * 메시지 조회
     * GET /messages/{id}
     */
    @GetMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun getById(
        @PathVariable id: String,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<MessageResponse.Detail>> {
        val message = messageApplicationService.getById(id)
        val response = MessageResponse.Detail.from(message)

        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }

    /**
     * 채팅방별 메시지 목록 조회
     * GET /messages/chat-rooms/{chatRoomId}
     */
    @GetMapping("/chat-rooms/{chatRoomId}")
    @AuthInfoAffect(required = true)
    fun getByChatRoomId(
        @PathVariable chatRoomId: String,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<List<MessageResponse.Detail>>> {
        val criteria = MessageCriteria.GetByChatRoomId(
            chatRoomId = chatRoomId
        )
        val messages = messageApplicationService.getByChatRoomId(criteria)
        val response = messages.map { MessageResponse.Detail.from(it) }

        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }
}
