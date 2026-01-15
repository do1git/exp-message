package site.rahoon.message.__monolitic.chatroom.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

enum class ChatRoomError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    CHAT_ROOM_NOT_FOUND("CHAT_ROOM_001", "Chat room not found", ErrorType.NOT_FOUND),
    INVALID_NAME("CHAT_ROOM_002", "Invalid chat room name", ErrorType.CLIENT_ERROR),
    UNAUTHORIZED_ACCESS("CHAT_ROOM_003", "Unauthorized access to chat room", ErrorType.FORBIDDEN)
}
