package site.rahoon.message.monolithic.common.websocket.config.doc

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * WebSocket 문서 API 컨트롤러
 * AsyncAPI 문서를 서빙하는 컨트롤러
 * WebSocket STOMP API 스펙을 AsyncAPI 3.0 형식으로 제공
 */
@RestController
@RequestMapping("/websocket-docs")
class WebSocketDocApiController(
    private val webSocketDocGenerator: WebSocketDocGenerator,
    private val objectMapper: ObjectMapper,
    @Value("\${websocket.base-package:site.rahoon.message}") private val basePackage: String,
) {
    companion object {
        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
    }

    /**
     * 프론트엔드 메타데이터 조회
     * GET /websocket-docs/metadata.json
     * 요청을 분석하여 동적으로 API 엔드포인트와 WebSocket URL을 생성
     */
    @GetMapping("/metadata.json", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getMetadata(request: HttpServletRequest): String {
        // 현재 요청을 기반으로 기본 URL 생성 (X-Forwarded-* 헤더 자동 처리)
        val baseUri = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .replacePath("")
            .build()
            .toUri()

        // Path prefix 추출 (X-Forwarded-Prefix 우선, 없으면 contextPath)
        val forwardedPrefix = request.getHeader("X-Forwarded-Prefix")
        val contextPath = request.contextPath
        val pathPrefix = forwardedPrefix?.takeIf { it.isNotBlank() } ?: contextPath

        // API 엔드포인트 경로 구성 (prefix + /websocket-docs/api)
        val apiEndpoint = if (pathPrefix.isNotBlank()) {
            if (pathPrefix.endsWith("/")) {
                "${pathPrefix}websocket-docs/api"
            } else {
                "$pathPrefix/websocket-docs/api"
            }
        } else {
            "/websocket-docs/api"
        }

        // WebSocket 경로 구성 (prefix + /ws)
        val wsPath = if (pathPrefix.isNotBlank() && !pathPrefix.endsWith("/")) {
            "$pathPrefix/ws"
        } else {
            "${pathPrefix}ws"
        }

        // SockJS는 http/https URL을 사용해야 함 (ws/wss가 아님)
        val httpScheme = baseUri.scheme // "http" 또는 "https"
        val httpPort = if (baseUri.port != -1 && baseUri.port != HTTP_PORT && baseUri.port != HTTPS_PORT) {
            ":${baseUri.port}"
        } else {
            ""
        }
        val websocketUrl = "$httpScheme://${baseUri.host}$httpPort$wsPath"

        val metadata = mapOf(
            "apiEndpoint" to apiEndpoint, // path prefix 포함 절대 경로
            "websocketUrl" to websocketUrl,
        )
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata)
    }

    /**
     * AsyncAPI 문서 조회
     * GET /websocket-docs/api
     */
    @GetMapping("/api", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getAsyncApiDoc(): String {
        // application.properties의 값에서 따옴표 제거
        val cleanBasePackage = basePackage.trim().removeSurrounding("\"")
        return webSocketDocGenerator.generate(cleanBasePackage)
    }
}
