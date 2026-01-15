package site.rahoon.message.__monolitic.user.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.__monolitic.common.controller.CommonApiResponse
import site.rahoon.message.__monolitic.common.controller.CommonAuthInfo
import site.rahoon.message.__monolitic.common.controller.AuthInfoAffect
import site.rahoon.message.__monolitic.user.application.UserApplicationService

/**
 * 사용자 관련 Controller
 * 회원가입 및 사용자 관리 API 제공
 */
@RestController
@RequestMapping("/users")
class UserController(
    private val userApplicationService: UserApplicationService
) {

    /**
     * 회원가입
     * POST /users
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun signUp(
        @Valid @RequestBody request: UserRequest.SignUp
    ): CommonApiResponse<UserResponse.SignUp> {
        val criteria = request.toCriteria()
        val userInfo = userApplicationService.register(criteria)
        val response = UserResponse.SignUp.from(userInfo)
        
        return CommonApiResponse.success(response)
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     * GET /users/me
     */
    @GetMapping("/me")
    @AuthInfoAffect(required = true)
    fun getCurrentUser(
        authInfo: CommonAuthInfo
    ): CommonApiResponse<UserResponse.Me> {
        val userInfo = userApplicationService.getCurrentUser(authInfo.userId)
        val response = UserResponse.Me.from(userInfo)
        
        return CommonApiResponse.success(response)
    }
}

