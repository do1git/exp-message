package site.rahoon.message.monolithic.common.websocket.config.doc

import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

/**
 * WebSocket 문서 웹 UI 컨트롤러
 * WebSocket 테스트 페이지를 서빙하는 컨트롤러
 */
@RestController
@RequestMapping("/websocket-docs")
class WebSocketDocWebController {
    @GetMapping("")
    fun redirectToIndex(): ResponseEntity<Void> {
        // 상대 경로로 리다이렉트 (브라우저가 자동으로 현재 경로 기준으로 해석)
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .header(HttpHeaders.LOCATION, "./")
            .build()
    }

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun index(): ResponseEntity<String> {
        val resource = ClassPathResource("static/websocket-docs/index.html")
        val content = StreamUtils.copyToString(resource.inputStream, StandardCharsets.UTF_8)
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
            .body(content)
    }

    @GetMapping("/styles.css", produces = ["text/css"])
    fun styles(): ResponseEntity<String> {
        val resource = ClassPathResource("static/websocket-docs/styles.css")
        val content = StreamUtils.copyToString(resource.inputStream, StandardCharsets.UTF_8)
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, "text/css")
            .body(content)
    }

    @GetMapping("/app.js", produces = ["application/javascript"])
    fun appJs(): ResponseEntity<String> {
        val resource = ClassPathResource("static/websocket-docs/app.js")
        val content = StreamUtils.copyToString(resource.inputStream, StandardCharsets.UTF_8)
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/javascript")
            .body(content)
    }
}
