package site.rahoon.message.__monolitic.common.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import site.rahoon.message.__monolitic.common.application.ApplicationException
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.domain.ErrorType
import java.time.ZonedDateTime

/**
 * 전역 예외 핸들러
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * DomainException 처리
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        e: DomainException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val status = when (e.error.type) {
            ErrorType.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorType.CONFLICT -> HttpStatus.CONFLICT
            ErrorType.SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
            ErrorType.CLIENT_ERROR -> HttpStatus.BAD_REQUEST
        }

        val response = ApiResponse.error<Nothing>(
            code = e.error.code,
            message = e.error.message,
            details = e.details,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(status).body(response)
    }

    /**
     * ApplicationException 처리
     */
    @ExceptionHandler(ApplicationException::class)
    fun handleApplicationException(
        e: ApplicationException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val details = e.details?.let { mapOf("detail" to it) }

        val response = ApiResponse.error<Nothing>(
            code = e.errorCode,
            message = e.errorMessage,
            details = details,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        e: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        val errors = e.bindingResult.fieldErrors.associate { 
            it.field to (it.defaultMessage ?: "")
        } as Map<String, Any>

        val response = ApiResponse.error<Nothing>(
            code = "VALIDATION_ERROR",
            message = "입력값 검증에 실패했습니다",
            details = errors,
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
    ): ResponseEntity<ApiResponse<Nothing>> {
        val response = ApiResponse.error<Nothing>(
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 오류가 발생했습니다",
            details = null,
            occurredAt = ZonedDateTime.now(),
            path = request.requestURI
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}

