package site.rahoon.message.monolithic.chatroom.domain

interface ChatRoomRepository {
    fun save(chatRoom: ChatRoom): ChatRoom

    fun findById(id: String): ChatRoom?

    fun findByIds(ids: List<String>): List<ChatRoom>

    fun findByCreatedByUserId(userId: String): List<ChatRoom>

    fun delete(id: String)
}
