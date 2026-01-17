package site.rahoon.message.monolithic.chatroom.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.chatroom.domain.component.ChatRoomCreateValidator
import site.rahoon.message.monolithic.chatroom.domain.component.ChatRoomUpdateValidator
import site.rahoon.message.monolithic.common.domain.DomainException

@Service
@Transactional(readOnly = true)
class ChatRoomDomainService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomCreateValidator: ChatRoomCreateValidator,
    private val chatRoomUpdateValidator: ChatRoomUpdateValidator,
) {
    @Transactional
    fun create(command: ChatRoomCommand.Create): ChatRoomInfo.Detail {
        // 입력값 검증
        chatRoomCreateValidator.validate(command)

        // ChatRoom 생성 로직은 도메인 객체에서 처리
        val chatRoom =
            ChatRoom.create(
                name = command.name,
                createdByUserId = command.createdByUserId,
            )

        val savedChatRoom = chatRoomRepository.save(chatRoom)
        return ChatRoomInfo.Detail.from(savedChatRoom)
    }

    @Transactional
    fun update(command: ChatRoomCommand.Update): ChatRoomInfo.Detail {
        // 입력값 검증
        chatRoomUpdateValidator.validate(command)

        val chatRoom =
            chatRoomRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                    details = mapOf("chatRoomId" to command.id),
                )

        // 업데이트 로직은 도메인 객체에서 처리
        val updatedChatRoom = chatRoom.updateName(command.name)

        val savedChatRoom = chatRoomRepository.save(updatedChatRoom)
        return ChatRoomInfo.Detail.from(savedChatRoom)
    }

    @Transactional
    fun delete(command: ChatRoomCommand.Delete): ChatRoomInfo.Detail {
        val chatRoom =
            chatRoomRepository.findById(command.id)
                ?: throw DomainException(
                    error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                    details = mapOf("chatRoomId" to command.id),
                )

        chatRoomRepository.delete(command.id)
        return ChatRoomInfo.Detail.from(chatRoom)
    }

    /**
     * ID로 채팅방 정보를 조회합니다.
     */
    fun getById(chatRoomId: String): ChatRoomInfo.Detail {
        val chatRoom =
            chatRoomRepository.findById(chatRoomId)
                ?: throw DomainException(
                    error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                    details = mapOf("chatRoomId" to chatRoomId),
                )
        return ChatRoomInfo.Detail.from(chatRoom)
    }

    /**
     * 특정 사용자가 생성한 채팅방 목록을 조회합니다.
     */
    fun getByCreatedByUserId(userId: String): List<ChatRoomInfo.Detail> {
        val chatRooms = chatRoomRepository.findByCreatedByUserId(userId)
        return chatRooms.map { ChatRoomInfo.Detail.from(it) }
    }

    /**
     * 여러 ID로 채팅방 목록을 조회합니다.
     */
    fun getByIds(chatRoomIds: List<String>): List<ChatRoomInfo.Detail> {
        if (chatRoomIds.isEmpty()) return emptyList()
        val chatRooms = chatRoomRepository.findByIds(chatRoomIds)
        return chatRooms.map { ChatRoomInfo.Detail.from(it) }
    }
}
