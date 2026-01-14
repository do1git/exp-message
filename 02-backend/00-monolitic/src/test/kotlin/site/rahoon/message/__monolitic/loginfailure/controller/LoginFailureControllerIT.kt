package site.rahoon.message.__monolitic.loginfailure.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import io.github.oshai.kotlinlogging.KotlinLogging
import site.rahoon.message.__monolitic.authtoken.controller.AuthRequest
import site.rahoon.message.__monolitic.common.test.ConcurrentTestUtils
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureRepository
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureTracker
import site.rahoon.message.__monolitic.user.application.UserApplicationITUtils
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * 로그인 실패 추적 Race Condition 테스트
 * 
 * 문제 상황:
 * - 여러 요청이 동시에 들어올 때, incrementFailureCount() 메서드가
 *   read-modify-write 패턴을 사용하므로 race condition이 발생할 수 있음
 * - 예: Thread 1과 Thread 2가 동시에 count=4를 읽고, 각각 5로 증가시켜 저장하면
 *   실제로는 6번째 실패가 발생해도 잠금이 되지 않을 수 있음
 */
class LoginFailureControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val loginFailureTracker: LoginFailureTracker,
    private val loginFailureRepository: LoginFailureRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val userApplicationITUtils: UserApplicationITUtils,
    @LocalServerPort private var port: Int = 0
) : IntegrationTestBase() {

    private val logger = KotlinLogging.logger {}

    private fun authBaseUrl(): String = "http://localhost:$port/auth"

    private lateinit var testEmail: String
    private lateinit var testIpAddress: String

    @BeforeEach
    fun setUp() {
        // Given: 테스트용 사용자 생성
        val signUpResult = userApplicationITUtils.signUp(
            email = uniqueEmail("race-test"),
            nickname = "racetest"
        )
        testEmail = signUpResult.email

        // 테스트용 고유 IP 생성 (이 테스트 내에서 일관되게 사용)
        testIpAddress = uniqueIp()

        // 기존 실패 기록 삭제
        loginFailureRepository.deleteByKey(testEmail)
        loginFailureRepository.deleteByKey(testIpAddress)
    }

    /**
     * Race Condition 통합 테스트
     * 
     * 시나리오:
     * 1. 4번 실패한 상태에서 시작
     * 2. 20개 스레드가 동시에 로그인 실패를 시도
     * 3. 검증:
     *    - HTTP 응답: USER_001(로그인 실패) 1개, LOGIN_FAILURE_001(잠금) 19개
     *    - Redis: 실패 카운트 5 이상, TTL은 "테스트 시작 전 시간 + 15분"과 "테스트 마지막 시간 +15분" 사이
     */
    @Test
    fun `race condition 테스트 - 동시 요청 시 실패 횟수와 잠금 동작 검증`() {
        // Given: 4번 실패한 상태로 설정
        val email = testEmail
        val ipAddress = testIpAddress
        val initialFailureCount = 4
        val threadCount = 20
        val lockoutDurationMinutes = 15L

        repeat(initialFailureCount) { loginFailureTracker.incrementFailureCount(email, ipAddress) }
        val beforeCount = loginFailureRepository.findByKey(email).failureCount
        beforeCount shouldBe initialFailureCount

        // HttpEntity 미리 생성 (X-Forwarded-For 헤더로 IP 전달)
        val loginRequestEntity = HttpEntity(
            objectMapper.writeValueAsString(AuthRequest.Login(email = email, password = "wrongpassword")),
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", ipAddress)
            }
        )

        // When: 여러 스레드가 동시에 로그인 실패를 시도
        val testStartTime = Instant.now()
        val responseBodies = ConcurrentTestUtils.executeConcurrent(threadCount) {
            try {
                restTemplate.exchange(
                    "${authBaseUrl()}/login",
                    HttpMethod.POST,
                    loginRequestEntity,
                    String::class.java
                ).body
            } catch (e: Exception) {
                logger.warn { "로그인 요청 중 예외 발생: ${e.message}" }
                null
            }
        }
        val testEndTime = Instant.now()

        // 응답 코드 추출
        val errorCodes = responseBodies.map { responseBody ->
            extractErrorCode(responseBody)
        }

        // 응답 코드 통계 수집
        val user001Count = errorCodes.count { it == "USER_001" }
        val loginFailure001Count = errorCodes.count { it == "LOGIN_FAILURE_001" }
        val otherErrorCount = errorCodes.count { it != "USER_001" && it != "LOGIN_FAILURE_001" && it != null }

        // Then: 실제 값 출력
        val finalFailureCount = loginFailureRepository.findByKey(email).failureCount
        val redisKey = "login_failure:$email"
        val ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS)
        val checkTime = Instant.now()
        val actualExpireTime = if (ttlSeconds > 0) checkTime.plusSeconds(ttlSeconds) else null

        // TTL 검증: TTL이 설정된 시점(testStartTime ~ testEndTime)부터 15분 후가 되어야 함
        val expectedMinExpireTime = testStartTime.plus(Duration.ofMinutes(lockoutDurationMinutes)).minusSeconds(2)
        val expectedMaxExpireTime = testEndTime.plus(Duration.ofMinutes(lockoutDurationMinutes)).plusSeconds(2)

        logTestResults(
            user001Count = user001Count,
            loginFailure001Count = loginFailure001Count,
            otherErrorCount = otherErrorCount,
            finalFailureCount = finalFailureCount,
            ttlSeconds = ttlSeconds,
            actualExpireTime = actualExpireTime,
            expectedMinExpireTime = expectedMinExpireTime,
            expectedMaxExpireTime = expectedMaxExpireTime
        )

        // Then: HTTP 응답 검증
        user001Count shouldBeLessThanOrEqual 1
        loginFailure001Count shouldBeGreaterThanOrEqual 19
        otherErrorCount shouldBe 0

        // Then: Redis 결과 검증
        finalFailureCount shouldBeGreaterThanOrEqual 5
        ttlSeconds shouldBeGreaterThan 0L

        if (actualExpireTime != null) {
            val isInExpectedRange = !actualExpireTime.isBefore(expectedMinExpireTime) &&
                !actualExpireTime.isAfter(expectedMaxExpireTime)
            isInExpectedRange shouldBe true
        }
    }

    /**
     * 응답 body에서 에러 코드를 추출합니다.
     */
    private fun extractErrorCode(responseBody: String?): String? {
        if (responseBody == null) return "UNKNOWN"
        return try {
            @Suppress("UNCHECKED_CAST")
            val responseMap = objectMapper.readValue(responseBody, Map::class.java) as Map<String, Any>
            if (responseMap["success"] == true) null
            else (responseMap["error"] as? Map<*, *>)?.get("code") as? String ?: "UNKNOWN"
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    /**
     * 테스트 결과를 로깅합니다.
     */
    private fun logTestResults(
        user001Count: Int,
        loginFailure001Count: Int,
        otherErrorCount: Int,
        finalFailureCount: Int,
        ttlSeconds: Long,
        actualExpireTime: Instant?,
        expectedMinExpireTime: Instant,
        expectedMaxExpireTime: Instant
    ) {
        logger.info {
            """
            |
            |${"=".repeat(60)}
            |Race Condition 테스트 결과
            |${"=".repeat(60)}
            |HTTP 응답 통계:
            |  USER_001(로그인 실패): $user001Count
            |  LOGIN_FAILURE_001(잠금): $loginFailure001Count
            |  기타 에러: $otherErrorCount
            |
            |Redis 결과:
            |  실패 카운트: $finalFailureCount
            |  TTL (초): $ttlSeconds
            |  ${if (actualExpireTime != null) "예상 만료 시간: $actualExpireTime" else ""}
            |  ${if (actualExpireTime != null) "예상 범위: $expectedMinExpireTime ~ $expectedMaxExpireTime" else ""}
            |${"=".repeat(60)}
            """.trimMargin()
        }
    }
}
