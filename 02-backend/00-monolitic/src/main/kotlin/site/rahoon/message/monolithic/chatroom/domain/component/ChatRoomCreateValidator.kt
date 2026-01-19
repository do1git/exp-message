package site.rahoon.message.monolithic.chatroom.domain.component

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomCommand
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomError
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * ChatRoom 생성 시 입력값 검증을 위한 인터페이스
 */
interface ChatRoomCreateValidator {
    /**
     * ChatRoomCommand.Create를 검증합니다.
     * 검증 실패 시 DomainException을 발생시킵니다.
     *
     * @param command 검증할 명령 객체
     * @throws DomainException 검증 실패 시
     */
    fun validate(command: ChatRoomCommand.Create)
}

/**
 * ChatRoom 생성 검증 구현체
 */
@Component
class ChatRoomCreateValidatorImpl : ChatRoomCreateValidator {
    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 100
    }

    override fun validate(command: ChatRoomCommand.Create) {
        validateName(command.name)
        validateCreatedByUserId(command.createdByUserId)
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw DomainException(
                error = ChatRoomError.INVALID_NAME,
                details = mapOf("reason" to "채팅방 이름은 필수입니다"),
            )
        }

        if (name.length < NAME_MIN_LENGTH || name.length > NAME_MAX_LENGTH) {
            throw DomainException(
                error = ChatRoomError.INVALID_NAME,
                details =
                    mapOf(
                        "name" to name,
                        "reason" to "채팅방 이름은 1자 이상 100자 이하여야 합니다",
                    ),
            )
        }
    }

    private fun validateCreatedByUserId(userId: String) {
        if (userId.isBlank()) {
            throw DomainException(
                error = ChatRoomError.INVALID_NAME,
                details = mapOf("reason" to "생성자 ID는 필수입니다"),
            )
        }
    }
}
