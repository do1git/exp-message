package site.rahoon.message.monolithic.channelconversation.domain

import site.rahoon.message.monolithic.common.domain.DomainError
import site.rahoon.message.monolithic.common.global.ErrorType

enum class ChannelConversationError(
    override val code: String,
    override val message: String,
    override val type: ErrorType,
) : DomainError {
    CHANNEL_CONVERSATION_NOT_FOUND("CHANNEL_CONVERSATION_001", "Channel conversation not found", ErrorType.NOT_FOUND),
    INVALID_NAME("CHANNEL_CONVERSATION_002", "Invalid conversation name", ErrorType.CLIENT_ERROR),
    INVALID_CHANNEL_ID("CHANNEL_CONVERSATION_003", "Invalid channel ID", ErrorType.CLIENT_ERROR),
    INVALID_CUSTOMER_ID("CHANNEL_CONVERSATION_004", "Invalid customer ID", ErrorType.CLIENT_ERROR),
    INVALID_CHAT_ROOM_ID("CHANNEL_CONVERSATION_005", "Invalid chat room ID", ErrorType.CLIENT_ERROR),
}
