package site.rahoon.message.__monolitic.message.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.message.domain.component.MessageCreateValidator
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class MessageDomainService(
    private val messageRepository: MessageRepository,
    private val messageCreateValidator: MessageCreateValidator
) {
    @Transactional
    fun create(command: MessageCommand.Create): Message {
        // 입력값 검증
        messageCreateValidator.validate(command)

        // Message 생성 로직은 도메인 객체에서 처리
        val message = Message.create(
            chatRoomId = command.chatRoomId,
            userId = command.userId,
            content = command.content
        )

        return messageRepository.save(message)
    }

    /**
     * ID로 메시지 정보를 조회합니다.
     */
    fun getById(messageId: String): Message {
        return messageRepository.findById(messageId)
            ?: throw DomainException(
                error = MessageError.MESSAGE_NOT_FOUND,
                details = mapOf("messageId" to messageId)
            )
    }

    /**
     * 채팅방별 메시지 목록을 조회합니다.
     */
    fun getByChatRoomId(
        chatRoomId: String,
        afterCreatedAt: LocalDateTime?,
        afterId: String?,
        limit: Int
    ): List<Message> {
        return messageRepository.findPageByChatRoomId(
            chatRoomId = chatRoomId,
            afterCreatedAt = afterCreatedAt,
            afterId = afterId,
            limit = limit
        )
    }
}
