package site.rahoon.message.monolithic.chatroom.controller

import site.rahoon.message.monolithic.chatroom.domain.ChatRoomInfo
import java.time.LocalDateTime

/**
 * ChatRoom Controller 응답 DTO
 */
object ChatRoomResponse {
    /**
     * 채팅방 생성 응답
     */
    data class Create(
        val id: String,
        val name: String,
        val createdByUserId: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * ChatRoomInfo.Detail로부터 ChatRoomResponse.Create를 생성합니다.
             */
            fun from(chatRoomInfo: ChatRoomInfo.Detail): Create =
                Create(
                    id = chatRoomInfo.id,
                    name = chatRoomInfo.name,
                    createdByUserId = chatRoomInfo.createdByUserId,
                    createdAt = chatRoomInfo.createdAt,
                    updatedAt = chatRoomInfo.updatedAt,
                )
        }
    }

    /**
     * 채팅방 조회 응답
     */
    data class Detail(
        val id: String,
        val name: String,
        val createdByUserId: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * ChatRoomInfo.Detail로부터 ChatRoomResponse.Detail을 생성합니다.
             */
            fun from(chatRoomInfo: ChatRoomInfo.Detail): Detail =
                Detail(
                    id = chatRoomInfo.id,
                    name = chatRoomInfo.name,
                    createdByUserId = chatRoomInfo.createdByUserId,
                    createdAt = chatRoomInfo.createdAt,
                    updatedAt = chatRoomInfo.updatedAt,
                )
        }
    }

    /**
     * 채팅방 목록 응답
     */
    data class ListItem(
        val id: String,
        val name: String,
        val createdByUserId: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * ChatRoomInfo.Detail로부터 ChatRoomResponse.ListItem을 생성합니다.
             */
            fun from(chatRoomInfo: ChatRoomInfo.Detail): ListItem =
                ListItem(
                    id = chatRoomInfo.id,
                    name = chatRoomInfo.name,
                    createdByUserId = chatRoomInfo.createdByUserId,
                    createdAt = chatRoomInfo.createdAt,
                    updatedAt = chatRoomInfo.updatedAt,
                )
        }
    }
}
