package site.rahoon.message.monolithic.channel.domain

import site.rahoon.message.monolithic.common.domain.DomainError
import site.rahoon.message.monolithic.common.global.ErrorType

enum class ChannelError(
    override val code: String,
    override val message: String,
    override val type: ErrorType,
) : DomainError {
    CHANNEL_NOT_FOUND("CHANNEL_001", "Channel not found", ErrorType.NOT_FOUND),
    INVALID_NAME("CHANNEL_002", "Invalid channel name", ErrorType.CLIENT_ERROR),
    INVALID_API_KEY("CHANNEL_003", "Invalid API key", ErrorType.CLIENT_ERROR),
    API_KEY_ALREADY_EXISTS("CHANNEL_004", "API key already exists", ErrorType.CONFLICT),
}
