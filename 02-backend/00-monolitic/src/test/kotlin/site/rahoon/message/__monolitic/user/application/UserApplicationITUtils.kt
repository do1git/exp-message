package site.rahoon.message.__monolitic.user.application

import org.springframework.boot.test.context.TestComponent
import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.common.test.TestUtils

/**
 * User 관련 테스트 유틸리티 (회원가입)
 *
 * 사용법:
 * ```kotlin
 * class MyControllerIT : IntegrationTestBase() {
 *     @Autowired lateinit var userApplicationService: UserApplicationService
 *
 *     private val userApplicationITUtils by lazy { UserApplicationITUtils(userApplicationService) }
 *
 *     @Test
 *     fun myTest() {
 *         val signUpResult = userApplicationITUtils.signUp()
 *         // ...
 *     }
 * }
 * ```
 *
 * 로그인이 필요한 경우 AuthApplicationITUtils를 사용하세요.
 */
@Component
class UserApplicationITUtils(
    private val userApplicationService: UserApplicationService
) {
    /**
     * 고유한 이메일을 생성합니다.
     */
    fun uniqueEmail(prefix: String = "test"): String = TestUtils.uniqueEmail(prefix)

    /**
     * 회원가입 결과
     */
    data class SignUpResult(
        val email: String,
        val password: String,
        val nickname: String,
        val userId: String
    )

    /**
     * 회원가입을 수행합니다.
     *
     * @param email 이메일 (기본값: 고유 이메일 생성)
     * @param password 비밀번호 (기본값: "password123")
     * @param nickname 닉네임 (기본값: "testuser")
     * @return SignUpResult
     */
    fun signUp(
        email: String = uniqueEmail(),
        password: String = "password123",
        nickname: String = "testuser"
    ): SignUpResult {
        val criteria = UserCriteria.Register(
            email = email,
            password = password,
            nickname = nickname
        )

        val userInfo = userApplicationService.register(criteria)

        return SignUpResult(
            email = email,
            password = password,
            nickname = nickname,
            userId = userInfo.id
        )
    }
}
