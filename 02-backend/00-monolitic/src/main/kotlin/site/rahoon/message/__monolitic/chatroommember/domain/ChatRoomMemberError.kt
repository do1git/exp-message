package site.rahoon.message.__monolitic.chatroommember.domain

import site.rahoon.message.__monolitic.common.domain.DomainError
import site.rahoon.message.__monolitic.common.global.ErrorType

enum class ChatRoomMemberError(
    override val code: String,
    override val message: String,
    override val type: ErrorType
) : DomainError {
    CHAT_ROOM_MEMBER_NOT_FOUND("CHAT_ROOM_MEMBER_001", "Chat room member not found", ErrorType.NOT_FOUND),
    ALREADY_JOINED("CHAT_ROOM_MEMBER_002", "User is already a member of this chat room", ErrorType.CLIENT_ERROR),
    NOT_A_MEMBER("CHAT_ROOM_MEMBER_003", "User is not a member of this chat room", ErrorType.CLIENT_ERROR)
}
