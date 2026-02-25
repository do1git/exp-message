package site.rahoon.message.monolithic.channelconversation.controller

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
import site.rahoon.message.monolithic.channelconversation.application.ChannelConversationApplicationService
import site.rahoon.message.monolithic.channelconversation.application.ChannelConversationCriteria
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.controller.CommonApiResponse

/**
 * ChannelConversation Controller
 * 채널별 상담 세션 생성, 조회, 수정, 삭제 API
 */
@RestController
@RequestMapping("/channels/{channelId}/conversations")
class ChannelConversationController(
    private val channelConversationApplicationService: ChannelConversationApplicationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable channelId: String,
        @Valid @RequestBody request: ChannelConversationRequest.Create,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelConversationResponse.Create> {
        val criteria = request.toCriteria(channelId, authInfo.userId)
        val info = channelConversationApplicationService.create(criteria)
        val response = ChannelConversationResponse.Create.from(info)
        return CommonApiResponse.success(response)
    }

    @GetMapping("/{id}")
    @Suppress("UnusedParameter")
    fun getById(
        @PathVariable channelId: String,
        @PathVariable id: String,
    ): CommonApiResponse<ChannelConversationResponse.Detail> {
        val info = channelConversationApplicationService.getById(id)
        val response = ChannelConversationResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }

    @GetMapping
    fun getByChannelId(
        @PathVariable channelId: String,
    ): CommonApiResponse<List<ChannelConversationResponse.ListItem>> {
        val list = channelConversationApplicationService.getByChannelId(channelId)
        val response = list.map { ChannelConversationResponse.ListItem.from(it) }
        return CommonApiResponse.success(response)
    }

    @PutMapping("/{id}")
    @Suppress("UnusedParameter")
    fun update(
        @PathVariable channelId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: ChannelConversationRequest.Update,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelConversationResponse.Detail> {
        val criteria = request.toCriteria(id)
        val info = channelConversationApplicationService.update(criteria)
        val response = ChannelConversationResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }

    @DeleteMapping("/{id}")
    @Suppress("UnusedParameter")
    fun delete(
        @PathVariable channelId: String,
        @PathVariable id: String,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelConversationResponse.Detail> {
        val criteria = ChannelConversationCriteria.Delete(channelConversationId = id)
        val info = channelConversationApplicationService.delete(criteria)
        val response = ChannelConversationResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }
}
