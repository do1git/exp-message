package site.rahoon.message.__monolitic.authtoken.infrastructure

import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.authtoken.domain.AccessTokenVerifier
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.filter.AuthTokenResolver

/**
 * AuthTokenResolver 인터페이스의 구현체
 * 
 * AccessTokenVerifier를 사용하여 토큰을 검증하고, 
 * AccessToken 도메인 객체를 AuthInfo로 변환합니다.
 * 
 * Infrastructure 레이어에 위치하여 도메인과 공통 인터페이스 간의 어댑터 역할을 수행합니다.
 * 도메인 예외(AuthTokenError)를 공통 예외(CommonError)로 변환합니다.
 */
@Component
class AuthTokenResolverImpl(
    private val accessTokenVerifier: AccessTokenVerifier
) : AuthTokenResolver {

    /**
     * 토큰 문자열을 검증하고 AuthInfo 객체로 변환합니다.
     *
     * @param token JWT 토큰 문자열 (Bearer 접두사가 있으면 AccessTokenVerifier에서 자동으로 제거됨)
     * @return AuthInfo (검증된 사용자 정보)
     * @throws DomainException CommonError에 해당하는 DomainException을 던집니다. 토큰이 유효하지 않거나 만료된 경우
     */
    override fun verify(token: String): CommonAuthInfo {
        return try {
            val accessToken = accessTokenVerifier.verify(token)
            CommonAuthInfo(
                userId = accessToken.userId,
                sessionId = accessToken.sessionId
            )
        } catch (e: Exception) {
            // AuthTokenError
            if (e is DomainException && e.error is AuthTokenError) {
                val errorType: CommonError = when (e.error) {
                    AuthTokenError.TOKEN_EXPIRED,
                    AuthTokenError.INVALID_TOKEN -> CommonError.UNAUTHORIZED
                    else -> CommonError.SERVER_ERROR
                }
                throw DomainException(
                    error = errorType,
                    details = e.details,
                    cause = if(errorType == CommonError.SERVER_ERROR) e else null
                )
            }
            // 그외 도메인 에러
            // 이미 CommonError이거나 다른 도메인 에러인 경우 SERVER_ERROR 처리
            if (e is DomainException) {
                throw DomainException(
                    error = CommonError.SERVER_ERROR,
                    details = e.details,
                    cause = e
                )
            }
            throw e
        }
    }
}
