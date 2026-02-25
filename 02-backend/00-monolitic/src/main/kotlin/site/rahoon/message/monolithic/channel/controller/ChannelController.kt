package site.rahoon.message.monolithic.channel.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.channel.application.ChannelApplicationService
import site.rahoon.message.monolithic.common.controller.CommonApiResponse

/**
 * Channel Controller
 * 채널(서비스 단위) 조회 API
 */
@RestController
@RequestMapping("/channels")
class ChannelController(
    private val channelApplicationService: ChannelApplicationService,
) {
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: String,
    ): CommonApiResponse<ChannelResponse.Detail> {
        val channelInfo = channelApplicationService.getById(id)
        val response = ChannelResponse.Detail.from(channelInfo)
        return CommonApiResponse.success(response)
    }

    @GetMapping(params = ["apiKey"])
    fun getByApiKey(
        @RequestParam apiKey: String,
    ): CommonApiResponse<List<ChannelResponse.Detail>> {
        val channelInfo = channelApplicationService.getByApiKey(apiKey)
        val response = listOf(ChannelResponse.Detail.from(channelInfo))
        return CommonApiResponse.success(response)
    }
}
