package site.rahoon.message.monolithic.channel.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.channel.domain.component.ChannelApiKeyGenerator
import site.rahoon.message.monolithic.channel.domain.component.ChannelCreateValidator
import site.rahoon.message.monolithic.channel.domain.component.ChannelUpdateValidator
import site.rahoon.message.monolithic.common.domain.DomainException

@Service
@Transactional(readOnly = true)
class ChannelDomainService(
    private val channelRepository: ChannelRepository,
    private val channelCreateValidator: ChannelCreateValidator,
    private val channelUpdateValidator: ChannelUpdateValidator,
    private val channelApiKeyGenerator: ChannelApiKeyGenerator,
) {
    @Suppress("MagicNumber")
    private fun generateUniqueApiKey(): String {
        var attempts = 0
        val maxAttempts = 10
        do {
            val apiKey = channelApiKeyGenerator.generate()
            if (channelRepository.findByApiKey(apiKey) == null) return apiKey
            attempts++
        } while (attempts < maxAttempts)
        throw DomainException(
            error = ChannelError.API_KEY_ALREADY_EXISTS,
            details = mapOf("reason" to "API 키 생성 재시도 횟수 초과"),
        )
    }

    @Transactional
    fun create(command: ChannelCommand.Create): ChannelInfo.Detail {
        channelCreateValidator.validate(command)

        val apiKey = generateUniqueApiKey()
        val channel = Channel.create(name = command.name, apiKey = apiKey)
        val savedChannel = channelRepository.save(channel)
        return ChannelInfo.Detail.from(savedChannel)
    }

    @Transactional
    fun update(command: ChannelCommand.Update): ChannelInfo.Detail {
        channelUpdateValidator.validate(command)

        val channel =
            channelRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to command.id),
                )

        var updatedChannel = channel
        command.name?.let { updatedChannel = updatedChannel.updateName(it) }

        val savedChannel = channelRepository.save(updatedChannel)
        return ChannelInfo.Detail.from(savedChannel)
    }

    @Transactional
    fun delete(command: ChannelCommand.Delete): ChannelInfo.Detail {
        val channel =
            channelRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChannelError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to command.id),
                )
        channelRepository.delete(command.id)
        return ChannelInfo.Detail.from(channel)
    }

    fun getById(channelId: String): ChannelInfo.Detail {
        val channel =
            channelRepository.findById(channelId)
                ?: throw DomainException(
                    error = ChannelError.CHANNEL_NOT_FOUND,
                    details = mapOf("channelId" to channelId),
                )
        return ChannelInfo.Detail.from(channel)
    }

    fun getByApiKey(apiKey: String): ChannelInfo.Detail {
        val channel =
            channelRepository.findByApiKey(apiKey)
                ?: throw DomainException(
                    error = ChannelError.CHANNEL_NOT_FOUND,
                    details = mapOf("apiKey" to apiKey),
                )
        return ChannelInfo.Detail.from(channel)
    }
}
