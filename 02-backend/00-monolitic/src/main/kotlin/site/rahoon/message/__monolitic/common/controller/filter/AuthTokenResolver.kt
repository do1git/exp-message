package site.rahoon.message.__monolitic.common.controller.filter

import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo

/**
 * 인증 토큰을 검증하고 AuthInfo 객체로 변환하는 인터페이스
 * 
 * 의존성 역전 원칙을 적용하여 AuthInfoArgumentResolver가 구체적인 구현에 의존하지 않도록 합니다.
 */
interface AuthTokenResolver {
    /**
     * 토큰 문자열을 검증하고 AuthInfo 객체로 변환합니다.
     *
     * @param token JWT 토큰 문자열 (Bearer 접두사가 있으면 자동으로 제거됨)
     * @return AuthInfo (검증된 사용자 정보)
     * @throws site.rahoon.message.__monolitic.common.domain.DomainException CommonError에 해당하는 DomainException을 던집니다. 토큰이 유효하지 않거나 만료된 경우
     */
    fun verify(token: String): CommonAuthInfo
}
