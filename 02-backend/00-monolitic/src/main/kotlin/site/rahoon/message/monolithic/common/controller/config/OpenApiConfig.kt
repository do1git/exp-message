package site.rahoon.message.monolithic.common.controller.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.tags.Tag
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.customizers.GlobalOperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import site.rahoon.message.monolithic.common.auth.CommonAdminAuthInfo
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
class OpenApiConfig {
    @Value("\${SWAGGER_UI_HOST:}")
    private var swaggerHost: String = ""

    // 전역 무시 설정 제거 - @AuthInfoAffect가 있는 경우에만 OperationCustomizer에서 처리
    companion object {
        private const val SWAGGER_ORDER_DEFAULT = 1000
    }

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI().apply {
            info(
                Info()
                    .title("Message API")
                    .description(
                        "Message Service API Documentation\n\n" +
                            "[WebSocket 문서](../websocket-docs/)에서 비동기 처리 API(AsyncAPI)를 확인할 수 있습니다.",
                    ).version("1.0.0"),
            )

            // Security Scheme 추가 (Bearer Token)
            components =
                (components ?: Components()).apply {
                    addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("일반 사용자 JWT (USER 역할). 토큰만 입력하면 됩니다. (Bearer는 자동 추가)"),
                    )
                    addSecuritySchemes(
                        "bearerAuthAdmin",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("관리자 JWT (ADMIN 역할). 토큰만 입력하면 됩니다. (Bearer는 자동 추가)"),
                    )
                }

            // Host가 지정된 경우 Server 정보 추가 (쉼표로 구분된 여러 호스트 지원)
            val hosts =
                swaggerHost
                    .split(",")
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toList()

            if (hosts.isNotEmpty()) {
                hosts.forEachIndexed { index, host ->
                    addServersItem(
                        Server()
                            .url(host)
                            .description(if (hosts.size > 1) "API Server ${index + 1}" else "API Server"),
                    )
                }
            }
        }

    /**
     * API 문서 그룹: 일반 API (Admin 제외)
     */
    @Bean
    fun apiGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("api")
            .displayName("00. USER API")
            .pathsToExclude("/admin/**")
            .build()

    /**
     * API 문서 그룹: Admin 전용 API
     */
    @Bean
    fun adminApiGroup(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("admin")
            .displayName("01. Admin API")
            .pathsToMatch("/admin/**")
            .build()

    /**
     * 메서드 이름 기반으로 Swagger Operation의 summary를 자동 생성하는 Customizer
     *
     * 현재는 메서드 이름을 기반으로 summary를 생성하지만,
     * 추후 KDoc 주석의 내용을 읽어서 반영하는 것을 목표로 한다.
     */
    @Bean
    fun methodNameBasedSummaryCustomizer(): GlobalOperationCustomizer {
        return GlobalOperationCustomizer customizer@{ operation, handlerMethod ->
            val method = (handlerMethod as? HandlerMethod)?.method ?: return@customizer operation

            // summary가 비어있으면 메서드 이름 기반으로 생성
            if (operation.summary.isNullOrBlank()) {
                val summary =
                    method.name
                        .replace(Regex("([A-Z])"), " $1")
                        .trim()
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }

                operation.summary = summary
            }
            operation
        }
    }

    /**
     * 컨트롤러 소스 수정 없이 Swagger 문서만 후처리해서,
     *
     * 1) 기본 규칙으로 태그를 정규화한다.
     *  - controller 접미사 제거 (예: auth-controller, AuthController -> Auth)
     *  - kebab/snake/camel 케이스를 단어로 분리
     *  - 단어별 첫 글자 대문자(Title Case)로 변환
     *
     * 2) 정규화된 태그명을 기준으로 override(태그명/description)를 적용한다.
     */
    @Bean
    @Suppress("MagicNumber", "LongMethod")
    fun swaggerTagAliasCustomizer(): GlobalOpenApiCustomizer {
        data class TagOverride(
            val name: String,
            val description: String? = null,
            val order: Int = SWAGGER_ORDER_DEFAULT,
        )

        /**
         * key는 "정규화된 태그명" 기준.
         * 예) 원본이 auth-controller / AuthController / authController 여도 normalize 후 "Auth"가 되므로,
         * 여기서는 "Auth"만 잡으면 된다.
         */
        val tagOverrides =
            mapOf(
                "Auth" to TagOverride("Auth", "로그인, 토큰 갱신, 로그아웃", 10),
                "User" to TagOverride("User", "회원가입, 정보 조회", 20),
                "Chat Room" to TagOverride("Chat Room", "채팅방 관리", 30),
                "Chat Room Member" to TagOverride("Chat Room Member", "채팅방 멤버 관리", 40),
                "Message" to TagOverride("Message", "메시징", 50),
                "Channel" to TagOverride("Channel", "채널 정보 조회", 100),
                "Channel Operator" to TagOverride("Channel Operator", "채널 관리자 관리", 110),
                "Channel Conversation" to TagOverride("Channel Conversation", "채널 대화 관리", 120),
                "Test" to TagOverride("Test", "테스트용", 900),
                "Web Socket Doc Web" to TagOverride("WebSocket Doc Web", "WebSocket 문서 Web", 910),
                "Web Socket Doc API" to TagOverride("WebSocket Doc API", "WebSocket 문서 API", 911),
            )

        fun normalizeTag(raw: String): String {
            val noControllerSuffix =
                raw
                    .trim()
                    // auth-controller / auth_controller / auth controller / AuthController 대응
                    .replace(Regex("(?i)([-_\\s]*controller)$"), "")

            val withSpaces =
                noControllerSuffix
                    // kebab/snake -> space
                    .replace(Regex("[-_]+"), " ")
                    // camelCase / PascalCase -> space
                    .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")

            val words =
                withSpaces
                    .trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotBlank() }

            return words.joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        return GlobalOpenApiCustomizer customizer@{ openApi ->
            val paths = openApi.paths ?: return@customizer

            // UI에 노출할 태그 정의(이름/설명/순서)도 같이 만들어준다.
            data class ResolvedTagMeta(
                val tag: Tag,
                val order: Int,
            )

            val resolvedTags = linkedMapOf<String, ResolvedTagMeta>()

            paths.values
                .asSequence()
                .flatMap { pathItem ->
                    sequenceOf(
                        pathItem.get,
                        pathItem.post,
                        pathItem.put,
                        pathItem.delete,
                        pathItem.patch,
                        pathItem.options,
                        pathItem.head,
                        pathItem.trace,
                    ).filterNotNull()
                }.forEach { op ->
                    val rawTags = op.tags ?: return@forEach

                    val normalizedTags =
                        rawTags
                            .asSequence()
                            .map { normalizeTag(it) }
                            .filter { it.isNotBlank() }
                            .toList()

                    val finalTags =
                        normalizedTags.map { normalized ->
                            val override = tagOverrides[normalized]
                            val finalName = override?.name ?: normalized

                            // tags 메타데이터도 생성/갱신 (description 포함)
                            val existingMeta = resolvedTags[finalName]
                            val existingTag = existingMeta?.tag ?: Tag().name(finalName)

                            val description = override?.description
                            val withDescription =
                                if (!description.isNullOrBlank()) {
                                    existingTag.description(description)
                                } else {
                                    existingTag
                                }

                            val order = override?.order ?: existingMeta?.order ?: SWAGGER_ORDER_DEFAULT
                            val mergedOrder = minOf(existingMeta?.order ?: order, order)

                            resolvedTags[finalName] = ResolvedTagMeta(tag = withDescription, order = mergedOrder)

                            finalName
                        }

                    op.tags = finalTags
                }

            // swagger-ui 태그 목록도 정규화/override 결과로 덮어쓴다.
            openApi.tags =
                resolvedTags.values
                    .asSequence()
                    .sortedWith(compareBy<ResolvedTagMeta> { it.order }.thenBy { it.tag.name })
                    .map { it.tag }
                    .toList()
        }
    }

    /**
     * @AuthInfoAffect 어노테이션이 있는 메소드에 Security를 자동으로 추가하고,
     * AuthInfo 파라미터를 문서에서 제거하는 Customizer
     */
    @Bean
    fun authInfoAffectOperationCustomizer(): GlobalOperationCustomizer {
        return GlobalOperationCustomizer customizer@{ operation, handlerMethod ->
            val handlerMethodObj = handlerMethod as? HandlerMethod ?: return@customizer operation

//            val method = handlerMethodObj.method
//            val hasAuthInfoAffect =
//                method.isAnnotationPresent(AuthInfoAffect::class.java) ||
//                    method.declaringClass.isAnnotationPresent(AuthInfoAffect::class.java)
//            if (!hasAuthInfoAffect) return@customizer operation

            val parameters = operation.parameters ?: return@customizer operation
            if (parameters.isEmpty()) return@customizer operation

            // AuthInfo 타입의 파라미터 제거
            // authInfo 파라미터 제거 (이름 기반 + 이름이 없는 경우 삭제)
            val methodParameters = handlerMethodObj.methodParameters
            val authInfoMethodParams = methodParameters
                .asSequence()
                .filter { methodParam ->
                    val type = methodParam.parameterType
                    type == CommonAuthInfo::class.java ||
                        type == CommonAuthInfo::class.javaObjectType ||
                        type == CommonAdminAuthInfo::class.java ||
                        type == CommonAdminAuthInfo::class.javaObjectType
                }.toList()
            val authInfoParamNames = authInfoMethodParams.asSequence().mapNotNull { it.parameterName }.toSet()
            val swaggerParamNamesToRemove = authInfoParamNames.ifEmpty { setOf("authInfo") }
            val filteredParameters = parameters.filterNot { it.name in swaggerParamNamesToRemove }
            operation.parameters = filteredParameters

            // 파라미터 개수가 변경되었으면 Security 추가 (파라미터 타입에 따라 scheme 선택)
            if (filteredParameters.size != parameters.size) {
                val hasCommonAdminAuthInfo =
                    authInfoMethodParams.any { param ->
                        val type = param.parameterType
                        type == CommonAdminAuthInfo::class.java ||
                            type == CommonAdminAuthInfo::class.javaObjectType
                    }
                val securityScheme = if (hasCommonAdminAuthInfo) "bearerAuthAdmin" else "bearerAuth"
                operation.addSecurityItem(SecurityRequirement().addList(securityScheme))
            }
            operation
        }
    }
}
