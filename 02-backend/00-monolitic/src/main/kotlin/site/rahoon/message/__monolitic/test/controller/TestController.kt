package site.rahoon.message.__monolitic.test.controller

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.domain.CommonError
import site.rahoon.message.__monolitic.common.domain.DomainException

/**
 * Health Check Controller
 * 서비스 상태 확인 및 E2E 테스트용 API
 */
@RestController
@RequestMapping("/test")
class TestController {

    private val logger = LoggerFactory.getLogger(TestController::class.java)

    @GetMapping("/health")
    fun health(): CommonApiResponse<Map<String, String>> {
        val data = mapOf(
            "status" to "ok",
            "message" to "Service is running"
        )
        return CommonApiResponse.success(data)
    }

    @GetMapping("/error")
    fun error(
        @RequestParam(required = false, defaultValue = "CLIENT_ERROR") errorType: String
    ): CommonApiResponse<Nothing> {
        val error = when (errorType.uppercase()) {
            "NOT_FOUND" -> CommonError.NOT_FOUND
            "CONFLICT" -> CommonError.CONFLICT
            "CLIENT_ERROR" -> CommonError.CLIENT_ERROR
            "SERVER_ERROR" -> CommonError.SERVER_ERROR
            else -> CommonError.CLIENT_ERROR
        }
        
        throw DomainException(
            error = error,
            details = mapOf("test" to "E2E test error")
        )
    }
}
