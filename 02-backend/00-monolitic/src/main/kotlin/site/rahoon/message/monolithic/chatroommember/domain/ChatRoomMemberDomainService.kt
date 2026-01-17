package site.rahoon.message.monolithic.chatroommember.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomError
import site.rahoon.message.monolithic.chatroom.domain.ChatRoomRepository
import site.rahoon.message.monolithic.common.domain.DomainException

@Service
@Transactional(readOnly = true)
class ChatRoomMemberDomainService(
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val chatRoomRepository: ChatRoomRepository,
) {
    @Transactional
    fun join(command: ChatRoomMemberCommand.Join): ChatRoomMemberInfo.Detail {
        // 채팅방 존재 확인
        chatRoomRepository.findById(command.chatRoomId)
            ?: throw DomainException(
                error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                details = mapOf("chatRoomId" to command.chatRoomId),
            )

        // 이미 참가한 멤버인지 확인
        chatRoomMemberRepository
            .findByChatRoomIdAndUserId(command.chatRoomId, command.userId)
            ?.let {
                throw DomainException(
                    error = ChatRoomMemberError.ALREADY_JOINED,
                    details =
                        mapOf(
                            "chatRoomId" to command.chatRoomId,
                            "userId" to command.userId,
                        ),
                )
            }

        // ChatRoomMember 생성
        val chatRoomMember =
            ChatRoomMember.create(
                chatRoomId = command.chatRoomId,
                userId = command.userId,
            )

        val savedMember = chatRoomMemberRepository.save(chatRoomMember)
        return ChatRoomMemberInfo.Detail.from(savedMember)
    }

    @Transactional
    fun leave(command: ChatRoomMemberCommand.Leave): ChatRoomMemberInfo.Detail {
        // 채팅방 존재 확인
        chatRoomRepository.findById(command.chatRoomId)
            ?: throw DomainException(
                error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                details = mapOf("chatRoomId" to command.chatRoomId),
            )

        // 멤버인지 확인
        val chatRoomMember =
            chatRoomMemberRepository.findByChatRoomIdAndUserId(
                command.chatRoomId,
                command.userId,
            ) ?: throw DomainException(
                error = ChatRoomMemberError.NOT_A_MEMBER,
                details =
                    mapOf(
                        "chatRoomId" to command.chatRoomId,
                        "userId" to command.userId,
                    ),
            )

        // 멤버 제거
        chatRoomMemberRepository.delete(command.chatRoomId, command.userId)
        return ChatRoomMemberInfo.Detail.from(chatRoomMember)
    }

    /**
     * 특정 채팅방의 멤버 목록을 조회합니다.
     */
    fun getByChatRoomId(chatRoomId: String): List<ChatRoomMemberInfo.Detail> {
        // 채팅방 존재 확인
        chatRoomRepository.findById(chatRoomId)
            ?: throw DomainException(
                error = ChatRoomError.CHAT_ROOM_NOT_FOUND,
                details = mapOf("chatRoomId" to chatRoomId),
            )

        val members = chatRoomMemberRepository.findByChatRoomId(chatRoomId)
        return members.map { ChatRoomMemberInfo.Detail.from(it) }
    }

    /**
     * 특정 사용자가 참가한 채팅방 목록을 조회합니다.
     */
    fun getByUserId(userId: String): List<ChatRoomMemberInfo.Detail> {
        val members = chatRoomMemberRepository.findByUserId(userId)
        return members.map { ChatRoomMemberInfo.Detail.from(it) }
    }

    /**
     * 특정 사용자가 특정 채팅방의 멤버인지 확인합니다.
     */
    fun isMember(
        chatRoomId: String,
        userId: String,
    ): Boolean = chatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId) != null
}
