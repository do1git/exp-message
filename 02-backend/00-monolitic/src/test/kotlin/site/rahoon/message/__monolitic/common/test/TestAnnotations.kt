package site.rahoon.message.__monolitic.common.test

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor

/**
 * 통합 테스트 마커 어노테이션
 *
 * - Testcontainers 사용 (MySQL, Redis 등)
 * - 전체 애플리케이션 컨텍스트 로드 (RANDOM_PORT)
 * - HTTP 요청/응답 + DB 연동 테스트
 *
 * 실행: `./gradlew integrationTest`
 *
 * 사용법: IntegrationTestBase를 상속하면 자동으로 적용됨
 * ```kotlin
 * class MyControllerIT : IntegrationTestBase() {
 *     @Test
 *     fun myTest() { ... }
 * }
 * ```
 *
 * 네이밍 컨벤션:
 * - 단위 테스트: *Test (예: MyServiceTest)
 * - 통합 테스트: *IT (예: MyControllerIT)
 *
 * 참고: IntegrationTestBase를 상속하지 않은 테스트는 자동으로 단위 테스트로 간주됨
 *       `./gradlew unitTest` 실행 시 @IntegrationTest 제외한 모든 테스트 실행
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
annotation class IntegrationTest
