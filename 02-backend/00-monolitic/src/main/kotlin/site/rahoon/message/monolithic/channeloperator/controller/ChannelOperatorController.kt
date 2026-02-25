package site.rahoon.message.monolithic.channeloperator.controller

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
import site.rahoon.message.monolithic.channeloperator.application.ChannelOperatorApplicationService
import site.rahoon.message.monolithic.channeloperator.application.ChannelOperatorCriteria
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.controller.CommonApiResponse

/**
 * ChannelOperator Controller
 * 채널 상담원 등록, 조회, 수정, 삭제 API
 */
@RestController
@RequestMapping("/channels/{channelId}/operators")
class ChannelOperatorController(
    private val channelOperatorApplicationService: ChannelOperatorApplicationService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Suppress("UnusedParameter")
    fun create(
        @PathVariable channelId: String,
        @Valid @RequestBody request: ChannelOperatorRequest.Create,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelOperatorResponse.Create> {
        val criteria = request.toCriteria(channelId)
        val info = channelOperatorApplicationService.create(criteria)
        val response = ChannelOperatorResponse.Create.from(info)
        return CommonApiResponse.success(response)
    }

    @GetMapping("/{id}")
    @Suppress("UnusedParameter")
    fun getById(
        @PathVariable channelId: String,
        @PathVariable id: String,
    ): CommonApiResponse<ChannelOperatorResponse.Detail> {
        val info = channelOperatorApplicationService.getById(id)
        val response = ChannelOperatorResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }

    @GetMapping
    fun getByChannelId(
        @PathVariable channelId: String,
    ): CommonApiResponse<List<ChannelOperatorResponse.ListItem>> {
        val list = channelOperatorApplicationService.getByChannelId(channelId)
        val response = list.map { ChannelOperatorResponse.ListItem.from(it) }
        return CommonApiResponse.success(response)
    }

    @PutMapping("/{id}")
    @Suppress("UnusedParameter")
    fun update(
        @PathVariable channelId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: ChannelOperatorRequest.Update,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelOperatorResponse.Detail> {
        val criteria = request.toCriteria(id)
        val info = channelOperatorApplicationService.update(criteria)
        val response = ChannelOperatorResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }

    @DeleteMapping("/{id}")
    @Suppress("UnusedParameter")
    fun delete(
        @PathVariable channelId: String,
        @PathVariable id: String,
        authInfo: CommonAuthInfo,
    ): CommonApiResponse<ChannelOperatorResponse.Detail> {
        val criteria = ChannelOperatorCriteria.Delete(channelOperatorId = id)
        val info = channelOperatorApplicationService.delete(criteria)
        val response = ChannelOperatorResponse.Detail.from(info)
        return CommonApiResponse.success(response)
    }
}
