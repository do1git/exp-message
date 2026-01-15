package site.rahoon.message.__monolitic.common.controller.filter

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException

/**
 * AuthInfo 파라미터를 자동으로 주입하는 ArgumentResolver
 * 
 * @AuthInfoAffect 어노테이션이 있는 메소드나 클래스에서 AuthInfo? 파라미터를 자동으로 주입합니다.
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증하고, AuthInfo 객체를 생성합니다.
 */
@Component
class CommonAuthInfoArgumentResolver(
    private val authTokenResolver: AuthTokenResolver
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        // AuthInfo 타입의 파라미터인지 확인
        val isAuthInfoType = parameter.parameterType == CommonAuthInfo::class.java ||
                             parameter.parameterType == CommonAuthInfo::class.javaObjectType
        
        if (!isAuthInfoType) {
            return false
        }
        
        // @AuthInfoAffect 어노테이션이 있는 경우에만 처리
        // 메소드 레벨 또는 클래스 레벨 어노테이션 확인
        val methodAnnotation = parameter.getMethodAnnotation(AuthInfoAffect::class.java)
        val classAnnotation = parameter.containingClass.getAnnotation(AuthInfoAffect::class.java)
        
        return methodAnnotation != null || classAnnotation != null
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Any? {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw IllegalStateException("HttpServletRequest를 가져올 수 없습니다")

        // @AuthInfoAffect 어노테이션 확인 (메소드 또는 클래스 레벨)
        // supportsParameter에서 이미 어노테이션 존재 여부를 확인했으므로, 여기서는 반드시 존재함
        val methodAnnotation = parameter.getMethodAnnotation(AuthInfoAffect::class.java)
        val classAnnotation = parameter.containingClass.getAnnotation(AuthInfoAffect::class.java)
        
        // 메소드 레벨 어노테이션이 있으면 우선, 없으면 클래스 레벨 어노테이션 사용
        val annotation = methodAnnotation ?: classAnnotation
            ?: throw IllegalStateException("@AuthInfoAffect 어노테이션이 없습니다. supportsParameter에서 체크해야 합니다.")
        
        val required = annotation.required

        // Authorization 헤더에서 토큰 추출
        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank()) {
            if (required) {
                throw DomainException(
                    error = CommonError.UNAUTHORIZED,
                    details = mapOf("reason" to "Authorization header is missing")
                )
            }
            return null
        }

        try {
            // 토큰 검증 및 AuthInfo 반환
            return authTokenResolver.verify(authHeader)
        } catch (e: DomainException) {
            // 토큰 검증 실패
            if (required) {
                throw e
            }
            return null
        }
    }
}