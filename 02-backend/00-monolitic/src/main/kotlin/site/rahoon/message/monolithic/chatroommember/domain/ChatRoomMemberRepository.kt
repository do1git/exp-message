package site.rahoon.message.monolithic.chatroommember.domain

interface ChatRoomMemberRepository {
    fun save(chatRoomMember: ChatRoomMember): ChatRoomMember

    fun findByChatRoomIdAndUserId(
        chatRoomId: String,
        userId: String,
    ): ChatRoomMember?

    fun findByChatRoomId(chatRoomId: String): List<ChatRoomMember>

    fun findByUserId(userId: String): List<ChatRoomMember>

    fun delete(
        chatRoomId: String,
        userId: String,
    )
}
