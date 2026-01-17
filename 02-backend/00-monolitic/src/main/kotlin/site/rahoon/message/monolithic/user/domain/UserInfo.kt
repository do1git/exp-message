package site.rahoon.message.monolithic.user.domain

import java.time.LocalDateTime

/**
 * User 정보를 외부에 노출할 때 사용하는 객체
 * passwordHash를 제외한 사용자 정보만 포함
 */
object UserInfo {
    data class Detail(
        val id: String,
        val email: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * User를 UserInfo.Detail로 변환합니다.
             * passwordHash를 제외한 정보만 반환합니다.
             */
            fun from(user: User): Detail =
                Detail(
                    id = user.id,
                    email = user.email,
                    nickname = user.nickname,
                    createdAt = user.createdAt,
                    updatedAt = user.updatedAt,
                )
        }
    }
}
