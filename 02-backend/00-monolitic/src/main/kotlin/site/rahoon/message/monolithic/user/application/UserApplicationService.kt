package site.rahoon.message.monolithic.user.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.user.domain.UserDomainService
import site.rahoon.message.monolithic.user.domain.UserInfo

/**
 * User Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class UserApplicationService(
    private val userDomainService: UserDomainService,
) {
    /**
     * 회원가입
     */
    fun register(criteria: UserCriteria.Register): UserInfo.Detail {
        val command = criteria.toCommand()
        return userDomainService.create(command)
    }

    /**
     * 현재 로그인한 사용자 정보를 조회합니다.
     */
    fun getCurrentUser(userId: String): UserInfo.Detail = userDomainService.getById(userId)
}
