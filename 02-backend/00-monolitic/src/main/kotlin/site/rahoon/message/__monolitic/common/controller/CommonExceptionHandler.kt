package site.rahoon.message.__monolitic.common.controller

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.ErrorType
import java.time.ZonedDateTime

/**
 * 전역 예외 핸들러
 */
@RestControllerAdvice
class CommonExceptionHandler {

    private val logger = LoggerFactory.getLogger(CommonExceptionHandler::class.java)

    /**
     * DomainException 처리
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException,
        request: HttpServletRequest
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val status = when (e.error.type) {
            ErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorType.CONFLICT -> HttpStatus.CONFLICT
            ErrorType.SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
            ErrorType.CLIENT_ERROR -> HttpStatus.BAD_REQUEST
            ErrorType.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            ErrorType.FORBIDDEN -> HttpStatus.FORBIDDEN
        }

        val response = CommonApiResponse.error<Nothing>(
            code = e.error.code,
            message = e.error.message,
            details = e.details,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(response)
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "")
        } as Map<String, Any>

        val response = CommonApiResponse.error<Nothing>(
            code = "VALIDATION_ERROR",
            message = "입력값 검증에 실패했습니다",
            details = errors,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * 리소스를 찾을 수 없을 때 처리 (404)
     */
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        e: NoResourceFoundException,
        request: HttpServletRequest
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val response = CommonApiResponse.error<Nothing>(
            code = "NOT_FOUND",
            message = "요청한 리소스를 찾을 수 없습니다",
            details = mapOf("resourcePath" to e.resourcePath),
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    /**
     * JSON 파싱 오류 처리 (400)
     * 필수 필드 누락이나 잘못된 JSON 형식일 때 발생
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        e: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val errorMessage = e.message?.let {
            when {
                it.contains("missing") || it.contains("NULL") -> "필수 필드가 누락되었습니다"
                it.contains("JSON parse error") -> "잘못된 JSON 형식입니다"
                else -> "요청 본문을 읽을 수 없습니다"
            }
        } ?: "요청 본문을 읽을 수 없습니다"

        logger.debug(
            """
            |Bad Request - JSON Parse Error
            |├─ Request: ${request.method} ${request.requestURI}
            |├─ Exception: ${e.javaClass.simpleName}
            |└─ Message: ${e.message ?: "No message"}
            """.trimMargin()
        )

        val response = CommonApiResponse.error<Nothing>(
            code = "BAD_REQUEST",
            message = errorMessage,
            details = mapOf("originalMessage" to (e.message ?: "")),
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest
    ): ResponseEntity<CommonApiResponse<Nothing>> {
        val queryString = request.queryString?.let { "?$it" } ?: ""
        logger.error(
            """
            |Internal Server Error
            |├─ Request: ${request.method} ${request.requestURI}$queryString
            |├─ Exception: ${e.javaClass.simpleName}
            |└─ Message: ${e.message ?: "No message"}
            """.trimMargin(),
            e
        )

        val response = CommonApiResponse.error<Nothing>(
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 오류가 발생했습니다",
            details = null,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}

