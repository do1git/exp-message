package site.rahoon.message.monolithic.authtoken.application

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.user.application.UserApplicationITUtils
import site.rahoon.message.monolithic.user.application.UserApplicationService
import site.rahoon.message.monolithic.user.domain.UserRepository
import site.rahoon.message.monolithic.user.domain.UserRole

/**
 * Auth 관련 테스트 유틸리티
 *
 * 사용법:
 * ```kotlin
 * class MyControllerIT : IntegrationTestBase() {
 *     @Autowired lateinit var authTokenApplicationService: AuthTokenApplicationService
 *     @Autowired lateinit var userApplicationService: UserApplicationService
 *
 *     private val authApplicationITUtils by lazy {
 *         AuthApplicationITUtils(authTokenApplicationService, userApplicationService)
 *     }
 *
 *     @Test
 *     fun myTest() {
 *         val auth = authApplicationITUtils.signUpAndLogin()
 *         val headers = authApplicationITUtils.authHeaders(auth.accessToken)
 *         // ...
 *     }
 * }
 * ```
 */
@Component
class AuthApplicationITUtils(
    private val authTokenApplicationService: AuthTokenApplicationService,
    private val userApplicationService: UserApplicationService,
    private val userRepository: UserRepository,
) {
    private val userApplicationITUtils = UserApplicationITUtils(userApplicationService)

    /**
     * 로그인 결과
     */
    data class AuthResult(
        val email: String,
        val password: String,
        val userId: String,
        val accessToken: String,
        val refreshToken: String,
        val sessionId: String,
        val headers: HttpHeaders,
    )

    /**
     * 로그인을 수행합니다.
     *
     * @param email 이메일
     * @param password 비밀번호
     * @param ipAddress IP 주소 (기본값: "127.0.0.1")
     * @return AuthResult
     */
    fun login(
        email: String,
        password: String = "password123",
        ipAddress: String = "127.0.0.1",
    ): AuthResult {
        val authToken = authTokenApplicationService.login(email, password, ipAddress)
        val token = authToken.accessToken.token

        return AuthResult(
            email = email,
            password = password,
            userId = authToken.accessToken.userId,
            accessToken = token,
            refreshToken = authToken.refreshToken?.token ?: "",
            sessionId = authToken.accessToken.sessionId,
            headers = authHeaders(token),
        )
    }

    /**
     * 회원가입 후 로그인을 수행합니다.
     *
     * @param email 이메일 (기본값: 고유 이메일 생성)
     * @param password 비밀번호 (기본값: "password123")
     * @param nickname 닉네임 (기본값: "testuser")
     * @param ipAddress IP 주소 (기본값: "127.0.0.1")
     * @return AuthResult
     */
    fun signUpAndLogin(
        email: String = userApplicationITUtils.uniqueEmail(),
        password: String = "password123",
        nickname: String = "testuser",
        ipAddress: String = "127.0.0.1",
    ): AuthResult {
        userApplicationITUtils.signUp(email, password, nickname)
        return login(email, password, ipAddress)
    }

    /**
     * 회원가입 후 ADMIN 역할로 변경하고 로그인을 수행합니다.
     *
     * @param email 이메일 (기본값: 고유 이메일 생성)
     * @param password 비밀번호 (기본값: "password123")
     * @param nickname 닉네임 (기본값: "adminuser")
     * @param ipAddress IP 주소 (기본값: "127.0.0.1")
     * @return AuthResult (ADMIN 역할 토큰)
     */
    fun signUpAdminAndLogin(
        email: String = userApplicationITUtils.uniqueEmail("admin"),
        password: String = "password123",
        nickname: String = "adminuser",
        ipAddress: String = "127.0.0.1",
    ): AuthResult {
        userApplicationITUtils.signUp(email, password, nickname)
        val user = userRepository.findByEmail(email) ?: error("User not found after signup")
        userRepository.save(user.copy(role = UserRole.ADMIN))
        return login(email, password, ipAddress)
    }

    /**
     * 인증 헤더를 생성합니다.
     */
    fun authHeaders(accessToken: String): HttpHeaders =
        HttpHeaders().apply {
            set("Authorization", "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
        }
}
