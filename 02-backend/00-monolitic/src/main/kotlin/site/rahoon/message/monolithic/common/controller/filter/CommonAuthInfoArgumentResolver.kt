package site.rahoon.message.monolithic.common.controller.filter

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import site.rahoon.message.monolithic.common.auth.AuthTokenResolver
import site.rahoon.message.monolithic.common.auth.CommonAdminAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthRole
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.observation.MdcKeys

/**
 * AuthInfo 파라미터를 자동으로 주입하는 ArgumentResolver
 *
 * @AuthInfoAffect 어노테이션이 있는 메소드나 클래스에서 AuthInfo? 파라미터를 자동으로 주입합니다.
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증하고, AuthInfo 객체를 생성합니다.
 */
@Component
class CommonAuthInfoArgumentResolver(
    private val authTokenResolver: AuthTokenResolver,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val type = parameter.parameterType
        return type == CommonAuthInfo::class.java ||
            type == CommonAuthInfo::class.javaObjectType ||
            type == CommonAdminAuthInfo::class.java ||
            type == CommonAdminAuthInfo::class.javaObjectType
    }

    @Suppress("ThrowsCount", "ReturnCount")
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw IllegalStateException("HttpServletRequest를 가져올 수 없습니다")

        // @AuthInfoAffect 어노테이션 확인 (메소드 또는 클래스 레벨)
        // supportsParameter에서 이미 어노테이션 존재 여부를 확인했으므로, 여기서는 반드시 존재함
//        val methodAnnotation = parameter.getMethodAnnotation(AuthInfoAffect::class.java)
//        val classAnnotation = parameter.containingClass.getAnnotation(AuthInfoAffect::class.java)
        // 메소드 레벨 어노테이션이 있으면 우선, 없으면 클래스 레벨 어노테이션 사용
//        val annotation =
//            methodAnnotation ?: classAnnotation
//                ?: throw IllegalStateException("@AuthInfoAffect 어노테이션이 없습니다. supportsParameter에서 체크해야 합니다.")
//        val required = annotation.required

        val required = !(parameter.isOptional)

        // Authorization 헤더에서 토큰 추출
        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank()) {
            if (required) {
                throw DomainException(
                    error = CommonError.UNAUTHORIZED,
                    details = mapOf("reason" to "Authorization header is missing"),
                )
            }
            return null
        }

        try {
            val authInfo = authTokenResolver.verify(authHeader)
            authInfo?.let {
                MDC.put(MdcKeys.USER_ID, it.userId)
                MDC.put(MdcKeys.AUTH_SESSION_ID, it.sessionId)
            }

            val isAdminAuthInfo =
                parameter.parameterType == CommonAdminAuthInfo::class.java ||
                    parameter.parameterType == CommonAdminAuthInfo::class.javaObjectType

            return if (isAdminAuthInfo) {
                if (authInfo == null) {
                    throw DomainException(
                        error = CommonError.UNAUTHORIZED,
                        details = mapOf("reason" to "Authorization required"),
                    )
                }
                if (authInfo.role != CommonAuthRole.ADMIN) {
                    throw DomainException(
                        error = CommonError.FORBIDDEN,
                        details = mapOf("reason" to "Admin role required"),
                    )
                }
                CommonAdminAuthInfo(authInfo)
            } else {
                authInfo
            }
        } catch (e: DomainException) {
            // 토큰 검증 실패
            if (required) {
                throw e
            }
            return null
        }
    }
}
