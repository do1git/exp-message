package site.rahoon.message.__monolitic.authtoken.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.authtoken.application.AuthTokenApplicationService
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect
import site.rahoon.message.__monolitic.common.controller.component.IpAddressUtils

/**
 * 인증 관련 Controller
 * 로그인, 토큰 갱신, 로그아웃 API 제공
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authTokenApplicationService: AuthTokenApplicationService
) {

    /**
     * 로그인
     * POST /auth/login
     */
    @PostMapping("/login")
    fun loginWithLock(
        @Valid @RequestBody request: AuthRequest.Login,
        httpRequest: HttpServletRequest
    ): CommonApiResponse<AuthResponse.Login> {
        val ipAddress = IpAddressUtils.getClientIpAddress(httpRequest)
        val authToken = authTokenApplicationService.loginWithLock(
            email = request.email,
            password = request.password,
            ipAddress = ipAddress
        )
        val response = AuthResponse.Login.from(authToken)

        return CommonApiResponse.success(response)
    }

    /**
     * 토큰 갱신
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: AuthRequest.Refresh
    ): CommonApiResponse<AuthResponse.Login> {
        val authToken = authTokenApplicationService.refresh(
            refreshToken = request.refreshToken
        )
        val response = AuthResponse.Login.from(authToken)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 로그아웃
     * POST /auth/logout
     */
    @PostMapping("/logout")
    @AuthInfoAffect(required = true)
    fun logout(
        authInfo: CommonAuthInfo
    ): CommonApiResponse<AuthResponse.Logout> {
        val sessionId = authInfo.sessionId
            ?: throw IllegalStateException("세션 ID가 없습니다")
        
        authTokenApplicationService.logout(sessionId)
        
        val response = AuthResponse.Logout()
        
        return CommonApiResponse.success(response)
    }
}

