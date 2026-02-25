package site.rahoon.message.monolithic.channeloperator.domain

import site.rahoon.message.monolithic.common.domain.DomainError
import site.rahoon.message.monolithic.common.global.ErrorType

enum class ChannelOperatorError(
    override val code: String,
    override val message: String,
    override val type: ErrorType,
) : DomainError {
    CHANNEL_OPERATOR_NOT_FOUND("CHANNEL_OPERATOR_001", "Channel operator not found", ErrorType.NOT_FOUND),
    INVALID_NICKNAME("CHANNEL_OPERATOR_002", "Invalid operator nickname", ErrorType.CLIENT_ERROR),
    INVALID_CHANNEL_ID("CHANNEL_OPERATOR_003", "Invalid channel ID", ErrorType.CLIENT_ERROR),
    INVALID_USER_ID("CHANNEL_OPERATOR_004", "Invalid user ID", ErrorType.CLIENT_ERROR),
    ALREADY_REGISTERED("CHANNEL_OPERATOR_005", "User already registered as operator for this channel", ErrorType.CONFLICT),
}
