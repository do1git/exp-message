package site.rahoon.message.monolithic.channel.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.channel.domain.ChannelDomainService
import site.rahoon.message.monolithic.channel.domain.ChannelInfo

/**
 * Channel Application Service
 */
@Service
class ChannelApplicationService(
    private val channelDomainService: ChannelDomainService,
) {
    fun create(criteria: ChannelCriteria.Create): ChannelInfo.Detail {
        val command = criteria.toCommand()
        return channelDomainService.create(command)
    }

    fun update(criteria: ChannelCriteria.Update): ChannelInfo.Detail {
        val command = criteria.toCommand()
        return channelDomainService.update(command)
    }

    fun delete(criteria: ChannelCriteria.Delete): ChannelInfo.Detail {
        val command = criteria.toCommand()
        return channelDomainService.delete(command)
    }

    fun getById(channelId: String): ChannelInfo.Detail = channelDomainService.getById(channelId)

    fun getByApiKey(apiKey: String): ChannelInfo.Detail = channelDomainService.getByApiKey(apiKey)
}
