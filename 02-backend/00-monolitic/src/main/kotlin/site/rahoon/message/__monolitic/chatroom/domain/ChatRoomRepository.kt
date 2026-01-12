package site.rahoon.message.__monolitic.chatroom.domain

interface ChatRoomRepository {
    fun save(chatRoom: ChatRoom): ChatRoom
    fun findById(id: String): ChatRoom?
    fun findByCreatedByUserId(userId: String): List<ChatRoom>
    fun delete(id: String)
}
