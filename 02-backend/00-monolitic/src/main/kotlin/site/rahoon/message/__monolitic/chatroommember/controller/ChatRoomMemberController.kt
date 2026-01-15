package site.rahoon.message.__monolitic.chatroommember.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberApplicationService
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect

/**
 * 채팅방 멤버 관련 Controller
 * 채팅방 참가, 나가기, 멤버 목록 조회 API 제공
 */
@RestController
@RequestMapping("/chat-rooms/{chatRoomId}/members")
class ChatRoomMemberController(
    private val chatRoomMemberApplicationService: ChatRoomMemberApplicationService
) {

    /**
     * 채팅방 참가
     * POST /chat-rooms/{chatRoomId}/members
     */
    @PostMapping
    @AuthInfoAffect(required = true)
    @ResponseStatus(HttpStatus.CREATED)
    fun join(
        @PathVariable chatRoomId: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomMemberResponse.Member> {
        val criteria = ChatRoomMemberRequest.toJoinCriteria(chatRoomId, authInfo.userId)
        val memberInfo = chatRoomMemberApplicationService.join(criteria)
        val response = ChatRoomMemberResponse.Member.from(memberInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방 나가기
     * DELETE /chat-rooms/{chatRoomId}/members/me
     */
    @DeleteMapping("/me")
    @AuthInfoAffect(required = true)
    fun leave(
        @PathVariable chatRoomId: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<ChatRoomMemberResponse.Member> {
        val criteria = ChatRoomMemberRequest.toLeaveCriteria(chatRoomId, authInfo.userId)
        val memberInfo = chatRoomMemberApplicationService.leave(criteria)
        val response = ChatRoomMemberResponse.Member.from(memberInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 채팅방 멤버 목록 조회
     * GET /chat-rooms/{chatRoomId}/members
     */
    @GetMapping
    @AuthInfoAffect(required = true)
    fun getMembers(
        @PathVariable chatRoomId: String,
        authInfo: CommonAuthInfo
    ): CommonApiResponse<List<ChatRoomMemberResponse.ListItem>> {
        val memberInfoList = chatRoomMemberApplicationService.getByChatRoomId(chatRoomId)
        val response = memberInfoList.map { ChatRoomMemberResponse.ListItem.from(it) }
        
        return CommonApiResponse.success(response)
    }
}
