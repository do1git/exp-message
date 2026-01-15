package site.rahoon.message.__monolitic.message.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

enum class MessageError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    MESSAGE_NOT_FOUND("MESSAGE_001", "Message not found", ErrorType.NOT_FOUND),
    INVALID_CONTENT("MESSAGE_002", "Invalid message content", ErrorType.CLIENT_ERROR),
    UNAUTHORIZED_ACCESS("MESSAGE_003", "Unauthorized access to send message", ErrorType.FORBIDDEN),
    CHAT_ROOM_NOT_FOUND("MESSAGE_004", "Chat room not found", ErrorType.NOT_FOUND)
}
