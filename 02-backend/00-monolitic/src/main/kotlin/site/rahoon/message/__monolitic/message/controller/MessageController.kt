package site.rahoon.message.__monolitic.message.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException
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
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: MessageRequest.Create,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<MessageResponse.Create> {
        val criteria = request.toCriteria(authInfo.userId)
        val message = messageApplicationService.create(criteria)
        val response = MessageResponse.Create.from(message)

        return CommonApiResponse.success(response)
    }

    /**
     * 메시지 조회
     * GET /messages/{id}
     */
    @GetMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun getById(
        @PathVariable id: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<MessageResponse.Detail> {
        val message = messageApplicationService.getById(id)
        val response = MessageResponse.Detail.from(message)

        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방별 메시지 목록 조회
     * GET /messages?chatRoomId={chatRoomId}
     */
    @GetMapping(params = ["chatRoomId"])
    @AuthInfoAffect(required = true)
    fun getByChatRoomId(
        @RequestParam chatRoomId: String,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
        authInfo: CommonAuthInfo
    ): CommonApiResponse.Page<MessageResponse.Detail> {
        val appliedLimit = when {
            limit == null -> DEFAULT_LIMIT
            limit <= 0 -> throw DomainException(
                error = CommonError.INVALID_PAGE_LIMIT,
                details = mapOf("limit" to limit, "reason" to "limit must be positive")
            )
            limit > MAX_LIMIT -> MAX_LIMIT
            else -> limit
        }

        val criteria = MessageCriteria.GetByChatRoomId(
            chatRoomId = chatRoomId,
            cursor = cursor,
            limit = appliedLimit
        )
        val result = messageApplicationService.getByChatRoomId(criteria)
        val response = result.items.map { MessageResponse.Detail.from(it) }

        return CommonApiResponse.Page.success(
            data = response,
            nextCursor = result.nextCursor,
            limit = result.limit
        )
    }

    companion object {
        private const val DEFAULT_LIMIT = 20
        private const val MAX_LIMIT = 100
    }
}
