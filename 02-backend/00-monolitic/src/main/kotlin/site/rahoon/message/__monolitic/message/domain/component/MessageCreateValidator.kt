package site.rahoon.message.__monolitic.message.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.message.domain.MessageCommand
import site.rahoon.message.__monolitic.message.domain.MessageError

/**
 * Message 생성 시 입력값 검증을 위한 인터페이스
 */
interface MessageCreateValidator {
    /**
     * MessageCommand.Create를 검증합니다.
     * 검증 실패 시 DomainException을 발생시킵니다.
     *
     * @param command 검증할 명령 객체
     * @throws DomainException 검증 실패 시
     */
    fun validate(command: MessageCommand.Create)
}

/**
 * Message 생성 검증 구현체
 */
@Component
class MessageCreateValidatorImpl : MessageCreateValidator {

    override fun validate(command: MessageCommand.Create) {
        validateContent(command.content)
        validateChatRoomId(command.chatRoomId)
        validateUserId(command.userId)
    }

    private fun validateContent(content: String) {
        if (content.isBlank()) {
            throw DomainException(
                error = MessageError.INVALID_CONTENT,
                details = mapOf("reason" to "메시지 내용은 필수입니다")
            )
        }

        if (content.length > 10000) {
            throw DomainException(
                error = MessageError.INVALID_CONTENT,
                details = mapOf(
                    "contentLength" to content.length,
                    "reason" to "메시지 내용은 10000자 이하여야 합니다"
                )
            )
        }
    }

    private fun validateChatRoomId(chatRoomId: String) {
        if (chatRoomId.isBlank()) {
            throw DomainException(
                error = MessageError.INVALID_CONTENT,
                details = mapOf("reason" to "채팅방 ID는 필수입니다")
            )
        }
    }

    private fun validateUserId(userId: String) {
        if (userId.isBlank()) {
            throw DomainException(
                error = MessageError.INVALID_CONTENT,
                details = mapOf("reason" to "사용자 ID는 필수입니다")
            )
        }
    }
}
