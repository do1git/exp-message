package site.rahoon.message.__monolitic.message.domain

interface MessageRepository {
    fun save(message: Message): Message
    fun findById(id: String): Message?
    fun findByChatRoomIdOrderByCreatedAtDesc(chatRoomId: String): List<Message>
}
