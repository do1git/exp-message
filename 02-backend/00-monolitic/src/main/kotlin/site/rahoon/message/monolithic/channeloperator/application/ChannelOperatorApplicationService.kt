package site.rahoon.message.monolithic.channeloperator.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorDomainService
import site.rahoon.message.monolithic.channeloperator.domain.ChannelOperatorInfo

/**
 * ChannelOperator Application Service
 */
@Service
class ChannelOperatorApplicationService(
    private val channelOperatorDomainService: ChannelOperatorDomainService,
) {
    fun create(criteria: ChannelOperatorCriteria.Create): ChannelOperatorInfo.Detail {
        val command = criteria.toCommand()
        return channelOperatorDomainService.create(command)
    }

    fun update(criteria: ChannelOperatorCriteria.Update): ChannelOperatorInfo.Detail {
        val command = criteria.toCommand()
        return channelOperatorDomainService.update(command)
    }

    fun delete(criteria: ChannelOperatorCriteria.Delete): ChannelOperatorInfo.Detail {
        val command = criteria.toCommand()
        return channelOperatorDomainService.delete(command)
    }

    fun getById(id: String): ChannelOperatorInfo.Detail = channelOperatorDomainService.getById(id)

    fun getByChannelId(channelId: String): List<ChannelOperatorInfo.Detail> = channelOperatorDomainService.getByChannelId(channelId)
}
