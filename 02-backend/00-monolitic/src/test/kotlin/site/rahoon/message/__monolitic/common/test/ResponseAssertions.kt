package site.rahoon.message.__monolitic.common.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import site.rahoon.message.__monolitic.common.controller.ApiResponse

/**
 * API 응답 검증을 위한 확장 함수 모음
 * Kotest assertions를 활용하여 간결한 테스트 코드 작성을 지원합니다.
 */

/**
 * 성공 응답을 검증하고 data를 반환합니다.
 *
 * @param objectMapper JSON 파싱용 ObjectMapper
 * @param expectedStatus 기대하는 HTTP 상태 코드 (기본값: OK)
 * @param dataAssertions data에 대한 추가 검증 (선택)
 * @return 파싱된 data 객체
 */
inline fun <reified T> ResponseEntity<String>.assertSuccess(
    objectMapper: ObjectMapper,
    expectedStatus: HttpStatus = HttpStatus.OK,
    dataAssertions: (T) -> Unit = {}
): T {
    statusCode shouldBe expectedStatus
    body shouldNotBe null

    val response = objectMapper.readValue<ApiResponse<T>>(body!!)
    response.success shouldBe true
    response.data shouldNotBe null

    dataAssertions(response.data!!)
    return response.data!!
}

/**
 * 에러 응답을 검증합니다.
 *
 * @param objectMapper JSON 파싱용 ObjectMapper
 * @param expectedStatus 기대하는 HTTP 상태 코드
 * @param expectedCode 기대하는 에러 코드 (선택)
 * @param errorAssertions error에 대한 추가 검증 (선택)
 * @return 파싱된 ErrorInfo 객체
 */
inline fun ResponseEntity<String>.assertError(
    objectMapper: ObjectMapper,
    expectedStatus: HttpStatus,
    expectedCode: String? = null,
    errorAssertions: (ApiResponse.ErrorInfo) -> Unit = {}
): ApiResponse.ErrorInfo {
    statusCode shouldBe expectedStatus
    body shouldNotBe null

    val response = objectMapper.readValue<ApiResponse<Any>>(body!!)
    response.success shouldBe false
    response.error shouldNotBe null

    expectedCode?.let { response.error!!.code shouldBe it }
    errorAssertions(response.error!!)
    return response.error!!
}

/**
 * 에러 응답의 details에 특정 필드가 있는지 검증합니다.
 */
fun ApiResponse.ErrorInfo.shouldHaveDetailField(field: String) {
    details shouldNotBe null
    details!!.containsKey(field) shouldBe true
}
