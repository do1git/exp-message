package site.rahoon.message.monolithic.chatroommember.application

import site.rahoon.message.monolithic.chatroommember.domain.ChatRoomMemberCommand

/**
 * ChatRoomMember Application Layer 입력 DTO
 */
object ChatRoomMemberCriteria {
    data class Join(
        val chatRoomId: String,
        val userId: String,
    ) {
        /**
         * ChatRoomMemberCommand.Join으로 변환합니다.
         */
        fun toCommand(): ChatRoomMemberCommand.Join =
            ChatRoomMemberCommand.Join(
                chatRoomId = this.chatRoomId,
                userId = this.userId,
            )
    }

    data class Leave(
        val chatRoomId: String,
        val userId: String,
    ) {
        /**
         * ChatRoomMemberCommand.Leave로 변환합니다.
         */
        fun toCommand(): ChatRoomMemberCommand.Leave =
            ChatRoomMemberCommand.Leave(
                chatRoomId = this.chatRoomId,
                userId = this.userId,
            )
    }
}
