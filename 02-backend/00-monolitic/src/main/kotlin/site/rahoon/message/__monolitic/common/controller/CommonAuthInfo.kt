package site.rahoon.message.__monolitic.common.controller

/** 현재 인증된 사용자 정보를 담는 객체 */
data class CommonAuthInfo(
    val userId: String,
    val sessionId: String? = null
)

/**
 * 로그인 여부가 영향을 주는 메소드에 대해 표시하는 어노테이션
 *
 * * 해당 어노테이션 사용시 AuthInfo? 또는 AuthInfo 변수에 데이터 자동 주입
 * * required가 true인 경우 인증되지 않은 경우 예외를 발생시킵니다.
 * * required가 false인 경우 인증되지 않은 경우 null을 반환합니다.
 * 
 * @property required 인증되지 않은 경우 예외를 발생시킬지 여부 (기본값: true)
 * 
 * @example
 * ```kotlin
 * @AuthInfoAffect(required = true)
 * fun getProfile(): UserProfile {
 *     // 인증된 사용자만 접근 가능
 * }
 * ```
*/
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthInfoAffect(
    val required: Boolean = true
)

