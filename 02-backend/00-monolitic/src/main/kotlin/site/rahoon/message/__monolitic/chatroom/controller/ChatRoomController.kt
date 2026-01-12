package site.rahoon.message.__monolitic.chatroom.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.chatroom.application.ChatRoomApplicationService
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import site.rahoon.message.__monolitic.common.global.utils.AuthInfo
import site.rahoon.message.__monolitic.common.global.utils.AuthInfoAffect

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
    fun create(
        @Valid @RequestBody request: ChatRoomRequest.Create,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<ChatRoomResponse.Create>> {
        val criteria = request.toCriteria()
        val chatRoomInfo = chatRoomApplicationService.create(criteria, authInfo.userId)
        val response = ChatRoomResponse.Create.from(chatRoomInfo)
        
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(response)
        )
    }

    /**
     * 채팅방 조회
     * GET /chat-rooms/{id}
     */
    @GetMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun getById(
        @PathVariable id: String,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<ChatRoomResponse.Detail>> {
        val chatRoomInfo = chatRoomApplicationService.getById(id)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }

    /**
     * 내가 생성한 채팅방 목록 조회
     * GET /chat-rooms
     */
    @GetMapping
    @AuthInfoAffect(required = true)
    fun getMyChatRooms(
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<List<ChatRoomResponse.ListItem>>> {
        val chatRoomInfoList = chatRoomApplicationService.getByCreatedByUserId(authInfo.userId)
        val response = chatRoomInfoList.map { ChatRoomResponse.ListItem.from(it) }
        
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
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
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<ChatRoomResponse.Detail>> {

        // TODO ChatRoomUser에 본인이 있는 채팅방을 모두 반환해야함
        val criteria = request.toCriteria()
        val chatRoomInfo = chatRoomApplicationService.update(criteria, id)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }

    /**
     * 채팅방 삭제
     * DELETE /chat-rooms/{id}
     */
    @DeleteMapping("/{id}")
    @AuthInfoAffect(required = true)
    fun delete(
        @PathVariable id: String,
        authInfo: AuthInfo
    ): ResponseEntity<ApiResponse<ChatRoomResponse.Detail>> {
        val chatRoomInfo = chatRoomApplicationService.delete(id)
        val response = ChatRoomResponse.Detail.from(chatRoomInfo)
        
        return ResponseEntity.status(HttpStatus.OK).body(
            ApiResponse.success(response)
        )
    }
}
