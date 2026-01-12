package site.rahoon.message.__monolitic.common.controller

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import site.rahoon.message.__monolitic.common.global.utils.AuthInfo
import site.rahoon.message.__monolitic.common.global.utils.AuthInfoAffect

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
class OpenApiConfig {

    @Value("\${SWAGGER_UI_HOST:}")
    private var swaggerHost: String = ""

    // 전역 무시 설정 제거 - @AuthInfoAffect가 있는 경우에만 OperationCustomizer에서 처리

    @Bean
    fun openAPI(): OpenAPI {
        val openAPI = OpenAPI()
            .info(
                Info()
                    .title("Message API")
                    .description("Message Service API Documentation")
                    .version("1.0.0")
            )

        // Security Scheme 추가 (Bearer Token)
        if (openAPI.components == null) {
            openAPI.components = Components()
        }
        openAPI.components
            .addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 사용한 인증. 'Bearer ' 접두사를 포함하여 토큰을 입력하세요.")
            )

        // Host가 지정된 경우 Server 정보 추가 (쉼표로 구분된 여러 호스트 지원)
        if (swaggerHost.isNotBlank()) {
            val hosts = swaggerHost.split(",").map { it.trim() }.filter { it.isNotBlank() }
            hosts.forEachIndexed { index, host ->
                openAPI.addServersItem(
                    Server()
                        .url(host)
                        .description(if (hosts.size > 1) "API Server ${index + 1}" else "API Server")
                )
            }
        }

        return openAPI
    }

    /**
     * 메서드 이름 기반으로 Swagger Operation의 summary를 자동 생성하는 Customizer
     * 
     * 현재는 메서드 이름을 기반으로 summary를 생성하지만,
     * 추후 KDoc 주석의 내용을 읽어서 반영하는 것을 목표로 한다.
     */
    @Bean
    fun methodNameBasedSummaryCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            val handlerMethodObj = handlerMethod as? HandlerMethod
            if (handlerMethodObj != null) {
                val method = handlerMethodObj.method
                
                // summary가 비어있으면 메서드 이름 기반으로 생성
                if (operation.summary.isNullOrBlank()) {
                    val methodName = method.name
                    val summary = methodName
                        .replace(Regex("([A-Z])"), " $1")
                        .trim()
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                    operation.summary = summary
                }
            }
            operation
        }
    }

    /**
     * @AuthInfoAffect 어노테이션이 있는 메소드에 Security를 자동으로 추가하고,
     * AuthInfo 파라미터를 문서에서 제거하는 Customizer
     */
    @Bean
    fun authInfoAffectOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            val handlerMethodObj = handlerMethod as? HandlerMethod
            if (handlerMethodObj != null) {
                val method = handlerMethodObj.method
                
                // 메소드 레벨 어노테이션 확인
                val methodAnnotation = method.getAnnotation(AuthInfoAffect::class.java)
                // 클래스 레벨 어노테이션 확인
                val classAnnotation = method.declaringClass.getAnnotation(AuthInfoAffect::class.java)
                
                // 메소드 또는 클래스에 @AuthInfoAffect 어노테이션이 있는 경우
                if (methodAnnotation != null || classAnnotation != null) {
                    // Security 추가
                    operation.addSecurityItem(
                        SecurityRequirement().addList("bearerAuth")
                    )
                    
                    // AuthInfo 타입의 파라미터 제거
                    val parameters = operation.parameters
                    if (parameters != null && parameters.isNotEmpty()) {
                        // HandlerMethod의 파라미터 정보를 사용하여 AuthInfo 타입 파라미터 이름 찾기
                        val methodParameters = handlerMethodObj.methodParameters
                        val authInfoParameterNames = methodParameters
                            .filter { param ->
                                param.parameterType == AuthInfo::class.java || 
                                param.parameterType == AuthInfo::class.javaObjectType
                            }
                            .mapNotNull { param ->
                                // 파라미터 이름 가져오기
                                param.parameterName
                            }
                            .toSet()
                        
                        // 파라미터 이름으로 매칭하여 제거
                        if (authInfoParameterNames.isNotEmpty()) {
                            val filteredParameters = parameters
                                .filterNot { param ->
                                    // 파라미터 이름으로 매칭
                                    authInfoParameterNames.contains(param.name)
                                }
                            operation.parameters = filteredParameters
                        } else {
                            // 파라미터 이름을 가져올 수 없는 경우, 인덱스 기반으로 fallback
                            val authInfoParameterIndices = methodParameters
                                .mapIndexedNotNull { index, param ->
                                    if (param.parameterType == AuthInfo::class.java || 
                                        param.parameterType == AuthInfo::class.javaObjectType) {
                                        index
                                    } else {
                                        null
                                    }
                                }
                                .toSet()
                            
                            if (authInfoParameterIndices.isNotEmpty()) {
                                val filteredParameters = parameters
                                    .mapIndexedNotNull { index, param ->
                                        if (authInfoParameterIndices.contains(index)) {
                                            null // AuthInfo 파라미터는 제거
                                        } else {
                                            param
                                        }
                                    }
                                operation.parameters = filteredParameters
                            }
                        }
                    }
                }
            }
            operation
        }
    }
}

