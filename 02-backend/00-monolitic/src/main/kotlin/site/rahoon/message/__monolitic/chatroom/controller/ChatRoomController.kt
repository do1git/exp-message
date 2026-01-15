package site.rahoon.message.__monolitic.chatroom.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.chatroom.application.ChatRoomApplicationService
import site.rahoon.message.__monolitic.chatroom.application.ChatRoomCriteria
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect

/**
 * 채팅방 관련 Controller
 * 채팅방 생성, 조회, 수정, 삭제 API 제공
 */
@RestController
@RequestMapping("/chat-rooms")
class ChatRoomController(
    private val chatRoomApplicationService: ChatRoomApplicationService
) {

    /**
     * 채팅방 생성
     * POST /chat-rooms
     */
    @PostMapping
    @AuthInfoAffect(required = true)
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: ChatRoomRequest.Create,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomResponse.Create> {
        val criteria = request.toCriteria(authInfo.userId)
        val chatRoomInfo = chatRoomApplicationService.create(criteria)
        val response = ChatRoomResponse.Create.from(chatRoomInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방 조회
     * GET /chat-rooms/{id}
     */
    @GetMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun getById(
        @PathVariable id: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomResponse.Detail> {
        val chatRoomInfo = chatRoomApplicationService.getById(id)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 내가 참여한 채팅방 목록 조회
     * GET /chat-rooms
     */
    @GetMapping
    @AuthInfoAffect(required = true)
    fun getMyChatRooms(
        authInfo: CommonAuthInfo
    ): CommonApiResponse<List<ChatRoomResponse.ListItem>> {
        val chatRoomInfoList = chatRoomApplicationService.getByMemberUserId(authInfo.userId)
        val response = chatRoomInfoList.map { ChatRoomResponse.ListItem.from(it) }
        
        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방 수정
     * PUT /chat-rooms/{id}
     */
    @PutMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: ChatRoomRequest.Update,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomResponse.Detail> {
        val criteria = request.toCriteria(id, authInfo.userId)
        val chatRoomInfo = chatRoomApplicationService.update(criteria)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방 삭제
     * DELETE /chat-rooms/{id}
     */
    @DeleteMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun delete(
        @PathVariable id: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomResponse.Detail> {
        val criteria = ChatRoomCriteria.Delete(
            chatRoomId = id,
            userId = authInfo.userId
        )
        val chatRoomInfo = chatRoomApplicationService.delete(criteria)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return CommonApiResponse.success(response)
    }
}
