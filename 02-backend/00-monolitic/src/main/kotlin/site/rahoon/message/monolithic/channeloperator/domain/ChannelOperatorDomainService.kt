package site.rahoon.message.monolithic.channeloperator.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channeloperator.domain.component.ChannelOperatorCreateValidator
import site.rahoon.message.monolithic.channeloperator.domain.component.ChannelOperatorUpdateValidator
import site.rahoon.message.monolithic.common.domain.DomainException

@Service
@Transactional(readOnly = true)
class ChannelOperatorDomainService(
    private val channelOperatorRepository: ChannelOperatorRepository,
    private val channelOperatorCreateValidator: ChannelOperatorCreateValidator,
    private val channelOperatorUpdateValidator: ChannelOperatorUpdateValidator,
) {
    @Transactional
    fun create(command: ChannelOperatorCommand.Create): ChannelOperatorInfo.Detail {
        channelOperatorCreateValidator.validate(command)

        channelOperatorRepository.findByChannelIdAndUserId(command.channelId, command.userId)?.let {
            throw DomainException(
                error = ChannelOperatorError.ALREADY_REGISTERED,
                details = mapOf(
                    "channelId" to command.channelId,
                    "userId" to command.userId,
                ),
            )
        }

        val channelOperator =
            ChannelOperator.create(
                channelId = command.channelId,
                userId = command.userId,
                nickname = command.nickname,
            )
        val saved = channelOperatorRepository.save(channelOperator)
        return ChannelOperatorInfo.Detail.from(saved)
    }

    @Transactional
    fun update(command: ChannelOperatorCommand.Update): ChannelOperatorInfo.Detail {
        channelOperatorUpdateValidator.validate(command)

        val channelOperator =
            channelOperatorRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelOperatorError.CHANNEL_OPERATOR_NOT_FOUND,
                    details = mapOf("channelOperatorId" to command.id),
                )

        val updated = channelOperator.updateNickname(command.nickname)
        val saved = channelOperatorRepository.save(updated)
        return ChannelOperatorInfo.Detail.from(saved)
    }

    @Transactional
    fun delete(command: ChannelOperatorCommand.Delete): ChannelOperatorInfo.Detail {
        val channelOperator =
            channelOperatorRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelOperatorError.CHANNEL_OPERATOR_NOT_FOUND,
                    details = mapOf("channelOperatorId" to command.id),
                )
        channelOperatorRepository.delete(command.id)
        return ChannelOperatorInfo.Detail.from(channelOperator)
    }

    fun getById(id: String): ChannelOperatorInfo.Detail {
        val channelOperator =
            channelOperatorRepository.findById(id)
                ?: throw DomainException(
                    error = ChannelOperatorError.CHANNEL_OPERATOR_NOT_FOUND,
                    details = mapOf("channelOperatorId" to id),
                )
        return ChannelOperatorInfo.Detail.from(channelOperator)
    }

    fun getByChannelId(channelId: String): List<ChannelOperatorInfo.Detail> {
        val operators = channelOperatorRepository.findByChannelId(channelId)
        return operators.map { ChannelOperatorInfo.Detail.from(it) }
    }
}
