package site.rahoon.message.monolithic.chatroommember.domain

import java.time.LocalDateTime

/**
 * ChatRoomMember 정보를 외부에 노출할 때 사용하는 객체
 */
object ChatRoomMemberInfo {
    data class Detail(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val joinedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * ChatRoomMember를 ChatRoomMemberInfo.Detail로 변환합니다.
             */
            fun from(chatRoomMember: ChatRoomMember): Detail =
                Detail(
                    id = chatRoomMember.id,
                    chatRoomId = chatRoomMember.chatRoomId,
                    userId = chatRoomMember.userId,
                    joinedAt = chatRoomMember.joinedAt,
                )
        }
    }
}
