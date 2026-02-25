package site.rahoon.message.monolithic.user.controller

import site.rahoon.message.monolithic.user.domain.UserInfo
import site.rahoon.message.monolithic.user.domain.UserRole
import java.time.LocalDateTime

/**
 * User Controller 응답 DTO
 */
object UserResponse {
    /**
     * 회원가입 응답
     */
    data class SignUp(
        val id: String,
        val email: String,
        val nickname: String,
        val createdAt: LocalDateTime,
    ) {
        companion object {
            /**
             * UserInfo.Detail로부터 UserResponse.SignUp을 생성합니다.
             */
            fun from(userInfo: UserInfo.Detail): SignUp =
                SignUp(
                    id = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                    createdAt = userInfo.createdAt,
                )
        }
    }

    /**
     * 현재 로그인한 사용자 정보 응답
     */
    data class Me(
        val id: String,
        val email: String,
        val nickname: String,
        val role: UserRole,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * UserInfo.Detail로부터 UserResponse.Me를 생성합니다.
             */
            fun from(userInfo: UserInfo.Detail): Me =
                Me(
                    id = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                    role = userInfo.role,
                    createdAt = userInfo.createdAt,
                    updatedAt = userInfo.updatedAt,
                )
        }
    }
}
